package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import db.DBConnection;
import model.EquipmentOrder;
import model.LANNodeTier;
import model.ReadinessAssessment;

public class SiteDAO {

    // Returns the Table 2 row whose range holds the node count, or null if none does.
    public LANNodeTier findTierFor(int nodes) throws SQLException {
        String sql = "SELECT tier_id, min_nodes, max_nodes, cost FROM lan_node_tiers "
                + "WHERE ? BETWEEN min_nodes AND max_nodes";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nodes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LANNodeTier(rs.getInt("tier_id"), rs.getInt("min_nodes"),
                            rs.getInt("max_nodes"), rs.getBigDecimal("cost"));
                }
                return null;
            }
        }
    }

    // Returns the latest visit's verdict: TRUE, FALSE, or null when no visit exists.
    public Boolean latestReadiness(int institutionId) throws SQLException {
        String sql = "SELECT is_ready FROM readiness_assessments WHERE institution_id = ? "
                + "ORDER BY visit_date DESC, assessment_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, institutionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean("is_ready") : null;
            }
        }
    }

    // Saves the visit and its order together: both rows land, or neither does.
    public void saveVisit(ReadinessAssessment visit) throws SQLException {
        String visitSql = "INSERT INTO readiness_assessments "
                + "(institution_id, visit_date, user_count, has_computers, has_lan, is_ready) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String orderSql = "INSERT INTO equipment_orders "
                + "(institution_id, tier_id, computer_qty, computer_cost, lan_cost, total_cost, order_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);                     // start the transaction
            try {
                try (PreparedStatement ps = conn.prepareStatement(visitSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, visit.getInstitutionId());
                    ps.setDate(2, java.sql.Date.valueOf(visit.getVisitDate()));
                    ps.setInt(3, visit.getUserCount());
                    ps.setBoolean(4, visit.hasComputers());
                    ps.setBoolean(5, visit.hasLan());
                    ps.setBoolean(6, visit.isReady());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) visit.setAssessmentId(keys.getInt(1));
                    }
                }

                EquipmentOrder order = visit.getOrder();
                if (order != null) {
                    try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, order.getInstitutionId());
                        if (order.getTier() == null) {
                            ps.setNull(2, Types.INTEGER);  // computers only: tier_id stays NULL
                        } else {
                            ps.setInt(2, order.getTier().getTierId());
                        }
                        ps.setInt(3, order.getComputerQty());
                        ps.setBigDecimal(4, order.computerCost());
                        ps.setBigDecimal(5, order.lanCost());
                        ps.setBigDecimal(6, order.totalCost());
                        ps.setDate(7, java.sql.Date.valueOf(order.getOrderDate()));
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) order.setOrderId(keys.getInt(1));
                        }
                    }
                }
                conn.commit();                             // both rows become permanent
            } catch (SQLException e) {
                conn.rollback();                           // undo the half-finished work
                throw e;
            }
        }
    }
}