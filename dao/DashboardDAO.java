package dao;

import db.DBConnection;
import model.DashboardStats;
import model.DashboardStats.RecentPayment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only queries for the dashboard. No business rule lives here:
 * it counts and sums rows exactly as they sit in the tables.
 */
public class DashboardDAO {

    // Four scalar subqueries in ONE statement: one network round trip to Aiven instead of four.
    private static final String STATS_SQL =
            "SELECT "
          + " (SELECT COUNT(*) FROM institutions) AS institutions, "
          + " (SELECT COUNT(*) FROM subscriptions WHERE status = 'active') AS active_subs, "
          + " (SELECT COUNT(*) FROM disconnections WHERE reconnected = FALSE) AS open_disc, "
          + " (SELECT COALESCE(SUM(amount), 0) FROM payments "
          + "   WHERE paid_on >= ? AND paid_on < ?) AS collected";   // ? = first day, first day of next month

    private static final String RECENT_SQL =
            "SELECT p.paid_on, i.name, p.payment_type, p.amount "
          + "FROM payments p JOIN institutions i ON i.institution_id = p.institution_id "
          + "ORDER BY p.paid_on DESC, p.payment_id DESC "
          + "LIMIT ?";

    /**
     * @param today        the date the dashboard treats as "now" (passed in so a test can fix it)
     * @param recentLimit  how many recent payments to return
     */
    public DashboardStats load(LocalDate today, int recentLimit) throws SQLException {
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate firstOfNext  = firstOfMonth.plusMonths(1);

        // One connection serves both statements; try-with-resources closes all of them in reverse order.
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stats  = con.prepareStatement(STATS_SQL);
             PreparedStatement recent = con.prepareStatement(RECENT_SQL)) {

            stats.setDate(1, Date.valueOf(firstOfMonth));
            stats.setDate(2, Date.valueOf(firstOfNext));

            int institutions, activeSubs, openDisc;
            BigDecimal collected;
            try (ResultSet rs = stats.executeQuery()) {
                rs.next();                                   // scalar subqueries always return exactly one row
                institutions = rs.getInt("institutions");
                activeSubs   = rs.getInt("active_subs");
                openDisc     = rs.getInt("open_disc");
                collected    = rs.getBigDecimal("collected"); // DECIMAL in MySQL -> BigDecimal in Java
            }

            recent.setInt(1, recentLimit);
            List<RecentPayment> rows = new ArrayList<>();
            try (ResultSet rs = recent.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RecentPayment(
                            rs.getDate("paid_on").toLocalDate(),
                            rs.getString("name"),
                            rs.getString("payment_type"),
                            rs.getBigDecimal("amount")));
                }
            }
            return new DashboardStats(institutions, activeSubs, openDisc, collected, rows);
        }
    }
}