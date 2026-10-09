package dao;

import db.DBConnection;
import model.BandwidthPlan;
import model.Subscription;
import model.Upgrade;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// SQL for bandwidth_plans, subscriptions and upgrades. No business rule lives here.
public class ServiceDAO {

    // Every plan, cheapest first, for the form's combo box.
    public List<BandwidthPlan> listPlans() throws SQLException {
        String sql = "SELECT plan_id, mbps, monthly_cost FROM bandwidth_plans ORDER BY mbps";
        List<BandwidthPlan> plans = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                plans.add(mapPlan(rs));
            }
        }
        return plans;
    }

    // One plan by its key, or null when no such plan exists.
    public BandwidthPlan findPlan(int planId) throws SQLException {
        String sql = "SELECT plan_id, mbps, monthly_cost FROM bandwidth_plans WHERE plan_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPlan(rs) : null;
            }
        }
    }

    // The institution's latest subscription, or null when it has none.
    public Subscription findByInstitution(int institutionId) throws SQLException {
        String sql = "SELECT subscription_id, institution_id, plan_id, start_date, status "
                + "FROM subscriptions WHERE institution_id = ? "
                + "ORDER BY subscription_id DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Subscription(
                        rs.getInt("subscription_id"),
                        rs.getInt("institution_id"),
                        rs.getInt("plan_id"),
                        rs.getDate("start_date").toLocalDate(),   // java.sql.Date -> LocalDate
                        rs.getString("status"));
            }
        }
    }

    // Saves a new subscription and writes MySQL's new key back into the object.
    public int insertSubscription(Subscription s) throws SQLException {
        String sql = "INSERT INTO subscriptions (institution_id, plan_id, start_date, status) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getInstitutionId());
            ps.setInt(2, s.getPlanId());
            ps.setDate(3, Date.valueOf(s.getStartDate()));
            ps.setString(4, s.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                s.setSubscriptionId(id);
                return id;
            }
        }
    }

    // Two tables change together: the upgrade history row and the subscription's current plan.
    public int recordUpgrade(Upgrade u) throws SQLException {
        String insertSql = "INSERT INTO upgrades "
                + "(subscription_id, old_plan_id, new_plan_id, discount_rate, upgraded_on) "
                + "VALUES (?, ?, ?, ?, ?)";
        // The extra "AND plan_id = ?" refuses the update if someone changed the plan meanwhile.
        String updateSql = "UPDATE subscriptions SET plan_id = ? "
                + "WHERE subscription_id = ? AND plan_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);                       // start the transaction
            try (PreparedStatement ins = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement upd = con.prepareStatement(updateSql)) {

                ins.setInt(1, u.getSubscriptionId());
                ins.setInt(2, u.getOldPlanId());
                ins.setInt(3, u.getNewPlanId());
                ins.setBigDecimal(4, u.getDiscountRate());
                ins.setDate(5, Date.valueOf(u.getUpgradedOn()));
                ins.executeUpdate();

                int upgradeId;
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    keys.next();
                    upgradeId = keys.getInt(1);
                }

                upd.setInt(1, u.getNewPlanId());
                upd.setInt(2, u.getSubscriptionId());
                upd.setInt(3, u.getOldPlanId());
                if (upd.executeUpdate() != 1) {
                    // Jumps to the catch below, which undoes the INSERT as well.
                    throw new SQLException("The subscription no longer holds the old plan.");
                }

                con.commit();                               // both changes become permanent together
                u.setUpgradeId(upgradeId);
                return upgradeId;
            } catch (SQLException e) {
                con.rollback();                             // neither change survives
                throw e;                                    // the service wraps it, cause intact
            }
        }
    }


    // True when the institution has at least one payment of this type.
    // paymentType must match the CHECK constraint: 'registration', 'installation', ...
    public boolean hasPayment(int institutionId, String paymentType) throws SQLException {
        String sql = "SELECT 1 FROM payments WHERE institution_id = ? AND payment_type = ? LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            ps.setString(2, paymentType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();                 // any row at all means "yes"
            }
        }
    }

    // is_ready from the latest visit: TRUE, FALSE, or null when no visit exists.
    public Boolean latestVisitReady(int institutionId) throws SQLException {
        String sql = "SELECT is_ready FROM readiness_assessments WHERE institution_id = ? "
                + "ORDER BY visit_date DESC, assessment_id DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean("is_ready") : null;
            }
        }
    }

    // Every upgrade of one subscription, oldest first, so the service can replay the plan history.
    public List<Upgrade> listUpgrades(int subscriptionId) throws SQLException {
        String sql = "SELECT upgrade_id, subscription_id, old_plan_id, new_plan_id, discount_rate, upgraded_on "
                + "FROM upgrades WHERE subscription_id = ? ORDER BY upgraded_on, upgrade_id";
        List<Upgrade> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, subscriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Upgrade u = new Upgrade(
                            rs.getInt("subscription_id"),
                            rs.getInt("old_plan_id"),
                            rs.getInt("new_plan_id"),
                            rs.getBigDecimal("discount_rate"),
                            rs.getDate("upgraded_on").toLocalDate());
                    u.setUpgradeId(rs.getInt("upgrade_id"));
                    list.add(u);
                }
            }
        }
        return list;
    }

    // One place turns a result row into a BandwidthPlan, so both queries stay consistent.
    private BandwidthPlan mapPlan(ResultSet rs) throws SQLException {
        return new BandwidthPlan(
                rs.getInt("plan_id"),
                rs.getInt("mbps"),
                rs.getBigDecimal("monthly_cost"));
    }
}