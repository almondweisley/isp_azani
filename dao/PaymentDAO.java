package dao;

import db.DBConnection;
import model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;

public class PaymentDAO {

    /** The facts a monthly bill needs: subscription start, plan price and any discount. */
    public static class BillBasis {
        public final LocalDate subscriptionStart;
        public final BigDecimal planCost;
        public final BigDecimal discountRate;

        BillBasis(LocalDate subscriptionStart, BigDecimal planCost, BigDecimal discountRate) {
            this.subscriptionStart = subscriptionStart;
            this.planCost = planCost;
            this.discountRate = discountRate;
        }
    }

    /** Saves any Payment subclass. The DAO never asks which subclass it received. */
    public int insert(Payment p) throws SQLException {
        String sql = "INSERT INTO payments (institution_id, payment_type, amount, paid_on, billing_month) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getInstitutionId());
            ps.setString(2, p.getType());                  // polymorphic call
            ps.setBigDecimal(3, p.amountDue());            // polymorphic call
            ps.setDate(4, Date.valueOf(p.getPaidOn()));
            if (p.getBillingMonth() == null) {
                ps.setNull(5, Types.VARCHAR);              // registration and installation rows
            } else {
                ps.setString(5, p.getBillingMonth());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                p.setPaymentId(keys.getInt(1));            // MySQL assigned the AUTO_INCREMENT id
                return p.getPaymentId();
            }
        }
    }

    /** How many payments of one type an institution already has. */
    public int countByType(int institutionId, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payments WHERE institution_id = ? AND payment_type = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** True when the institution already paid the bill for that month. */
    public boolean monthlyPaid(int institutionId, YearMonth month) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payments "
                + "WHERE institution_id = ? AND payment_type = 'monthly' AND billing_month = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Finds the plan in force for one month, together with any upgrade discount.
     * Plan in force: the newest upgrade on or before month end; otherwise, when the
     * month predates every upgrade, the plan the first upgrade replaced; otherwise
     * the subscription's own plan. Returns null when the institution has no subscription.
     */
    public BillBasis findBillBasis(int institutionId, YearMonth month) throws SQLException {
        String sql =
                "SELECT x.start_date, bp.monthly_cost, x.discount_rate FROM ( "
                        + "  SELECT s.start_date, "
                        + "    COALESCE( "
                        + "      (SELECT u.new_plan_id FROM upgrades u "
                        + "        WHERE u.subscription_id = s.subscription_id AND u.upgraded_on <= ? "
                        + "        ORDER BY u.upgraded_on DESC, u.upgrade_id DESC LIMIT 1), "
                        + "      (SELECT u.old_plan_id FROM upgrades u "
                        + "        WHERE u.subscription_id = s.subscription_id "
                        + "        ORDER BY u.upgraded_on, u.upgrade_id LIMIT 1), "
                        + "      s.plan_id) AS plan_in_force, "
                        // Decision B, reading (i): the discount counts only when the upgrade falls inside this month.
                        + "    (SELECT u.discount_rate FROM upgrades u "
                        + "      WHERE u.subscription_id = s.subscription_id AND u.upgraded_on BETWEEN ? AND ? "
                        + "      ORDER BY u.upgraded_on DESC, u.upgrade_id DESC LIMIT 1) AS discount_rate "
                        + "  FROM subscriptions s WHERE s.institution_id = ? "
                        + "  ORDER BY s.start_date DESC, s.subscription_id DESC LIMIT 1 "
                        + ") x JOIN bandwidth_plans bp ON bp.plan_id = x.plan_in_force";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(month.atEndOfMonth()));   // 2025-04-30
            ps.setDate(2, Date.valueOf(month.atDay(1)));         // 2025-04-01
            ps.setDate(3, Date.valueOf(month.atEndOfMonth()));
            ps.setInt(4, institutionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;                                 // no subscription row at all
                }
                BigDecimal discount = rs.getBigDecimal("discount_rate");
                return new BillBasis(
                        rs.getDate("start_date").toLocalDate(),
                        rs.getBigDecimal("monthly_cost"),
                        discount == null ? BigDecimal.ZERO : discount);
            }
        }
    }
}