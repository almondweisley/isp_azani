package dao;

import db.DBConnection;
import model.Disconnection;
import model.OverdueFine;
import model.Payment;
import model.Subscription;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// SQL for overdue_fines and disconnections, plus the payment rows they need. No business rule lives here.
public class BillingDAO {

    // Every institution that holds a subscription, for a full billing run.
    public List<Integer> listSubscribedInstitutions() throws SQLException {
        String sql = "SELECT DISTINCT institution_id FROM subscriptions ORDER BY institution_id";
        List<Integer> ids = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    // True when the month's bill was paid on or before the given date.
    public boolean monthlyPaidBy(int institutionId, YearMonth month, LocalDate by) throws SQLException {
        String sql = "SELECT 1 FROM payments WHERE institution_id = ? AND payment_type = 'monthly' "
                + "AND billing_month = ? AND paid_on <= ? LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setString(2, month.toString());
            ps.setDate(3, Date.valueOf(by));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // The fine on one bill, or null when none exists yet.
    public OverdueFine findFine(int institutionId, YearMonth month) throws SQLException {
        String sql = "SELECT fine_id, institution_id, billing_month, fine_amount, settled "
                + "FROM overdue_fines WHERE institution_id = ? AND billing_month = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new OverdueFine(
                        rs.getInt("fine_id"),
                        rs.getInt("institution_id"),
                        YearMonth.parse(rs.getString("billing_month")),   // "2026-08" -> YearMonth
                        rs.getBigDecimal("fine_amount"),
                        rs.getBoolean("settled"));
            }
        }
    }

    public int insertFine(OverdueFine f) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return insertFine(con, f);
        }
    }

    public int countUnsettledFines(int institutionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM overdue_fines WHERE institution_id = ? AND settled = FALSE";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // Decision D5a: true when the service stood cut off on this day.
    public boolean cutOffOn(int institutionId, LocalDate day) throws SQLException {
        String sql = "SELECT 1 FROM disconnections WHERE institution_id = ? AND disconnected_on <= ? "
                + "AND (reconnected = FALSE OR reconnected_on > ?) LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setDate(2, Date.valueOf(day));
            ps.setDate(3, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Stops a repeat run from recording the same disconnection twice.
    public boolean hasDisconnectionOn(int institutionId, LocalDate day) throws SQLException {
        String sql = "SELECT 1 FROM disconnections WHERE institution_id = ? AND disconnected_on = ? LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setDate(2, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // The disconnection still waiting for reconnection, or null.
    public Disconnection findOpenDisconnection(int institutionId) throws SQLException {
        String sql = "SELECT disconnection_id, institution_id, disconnected_on, reconnected, reconnected_on "
                + "FROM disconnections WHERE institution_id = ? AND reconnected = FALSE "
                + "ORDER BY disconnected_on DESC, disconnection_id DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Date back = rs.getDate("reconnected_on");          // NULL in MySQL arrives as null
                return new Disconnection(
                        rs.getInt("disconnection_id"),
                        rs.getInt("institution_id"),
                        rs.getDate("disconnected_on").toLocalDate(),
                        rs.getBoolean("reconnected"),
                        back == null ? null : back.toLocalDate());
            }
        }
    }

    // One transaction: the disconnection row and the subscription status (decision D4a).
    public void disconnect(Disconnection d, int subscriptionId) throws SQLException {
        String insertSql = "INSERT INTO disconnections (institution_id, disconnected_on, reconnected, reconnected_on) "
                + "VALUES (?, ?, FALSE, NULL)";
        String updateSql = "UPDATE subscriptions SET status = ? WHERE subscription_id = ?";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ins = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement upd = con.prepareStatement(updateSql)) {
                ins.setInt(1, d.getInstitutionId());
                ins.setDate(2, Date.valueOf(d.getDisconnectedOn()));
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    keys.next();
                    d.setDisconnectionId(keys.getInt(1));
                }
                upd.setString(1, Subscription.DISCONNECTED);
                upd.setInt(2, subscriptionId);
                upd.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // Decision D1a in one transaction: the bill-plus-fine payment and the settled fine.
    public void recordLatePayment(Payment p, OverdueFine fine) throws SQLException {
        String settleSql = "UPDATE overdue_fines SET settled = TRUE WHERE fine_id = ? AND settled = FALSE";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                insertPayment(con, p);
                if (fine.getFineId() == 0) {
                    insertFine(con, fine);                 // billing had not run yet: the fine is born settled
                } else {
                    try (PreparedStatement ps = con.prepareStatement(settleSql)) {
                        ps.setInt(1, fine.getFineId());
                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("The fine was already settled.");
                        }
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // One transaction: the reconnection fee, the closed disconnection and the active subscription.
    public void reconnect(Disconnection d, Payment p, int subscriptionId) throws SQLException {
        String closeSql = "UPDATE disconnections SET reconnected = TRUE, reconnected_on = ? "
                + "WHERE disconnection_id = ? AND reconnected = FALSE";
        String activeSql = "UPDATE subscriptions SET status = ? WHERE subscription_id = ?";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement close = con.prepareStatement(closeSql);
                 PreparedStatement active = con.prepareStatement(activeSql)) {
                insertPayment(con, p);
                close.setDate(1, Date.valueOf(d.getReconnectedOn()));
                close.setInt(2, d.getDisconnectionId());
                if (close.executeUpdate() != 1) {
                    throw new SQLException("The disconnection was already closed.");
                }
                active.setString(1, Subscription.ACTIVE);
                active.setInt(2, subscriptionId);
                active.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // Takes the caller's connection, so the insert joins the caller's transaction.
    private int insertFine(Connection con, OverdueFine f) throws SQLException {
        String sql = "INSERT INTO overdue_fines (institution_id, billing_month, fine_amount, settled) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getInstitutionId());
            ps.setString(2, f.getBillingMonth().toString());
            ps.setBigDecimal(3, f.getFineAmount());
            ps.setBoolean(4, f.isSettled());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                f.setFineId(keys.getInt(1));
                return f.getFineId();
            }
        }
    }

    // Same SQL as PaymentDAO.insert, but on the caller's connection.
    private int insertPayment(Connection con, Payment p) throws SQLException {
        String sql = "INSERT INTO payments (institution_id, payment_type, amount, paid_on, billing_month) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getInstitutionId());
            ps.setString(2, p.getType());
            ps.setBigDecimal(3, p.amountDue());
            ps.setDate(4, Date.valueOf(p.getPaidOn()));
            if (p.getBillingMonth() == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, p.getBillingMonth());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                p.setPaymentId(keys.getInt(1));
                return p.getPaymentId();
            }
        }
    }
}