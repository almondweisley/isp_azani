package dao;

import db.DBConnection;
import model.ReportTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Step 8: one method per list in task 3 and per computation in task 4.
// Every method returns a ReportTable (headings plus rows), so step 9 can show any
// report in one JTable without writing a separate model class per report.
// Headings come from the SQL aliases (AS `...`), read back through ResultSetMetaData.
// Dates pass through DATE_FORMAT, so they reach Java as text and cannot shift (bug 1).
public class ReportDAO {

    // Must match the CHECK constraint on payments.payment_type.
    private static final String REGISTRATION = "registration";
    private static final String INSTALLATION = "installation";
    private static final String MONTHLY      = "monthly";
    private static final String RECONNECTION = "reconnection";

    // Shared by 3(d), 4(a) and 4(b): all equipment orders totalled per institution.
    // Declared first: a static field may only use constants declared above it.
    // Totalling BEFORE the join means one row per institution, so nothing is counted twice.
    private static final String EQUIPMENT_TOTALS =
            "(SELECT o.institution_id, "
                    + "        SUM(o.computer_qty) AS pcs, "
                    + "        GROUP_CONCAT(CONCAT(t.min_nodes, '-', t.max_nodes) SEPARATOR ', ') AS tiers, "
                    + "        SUM(o.computer_cost) AS pc_cost, "
                    + "        SUM(o.lan_cost) AS lan_cost, "
                    + "        SUM(o.total_cost) AS total "
                    + " FROM equipment_orders o "
                    + " LEFT JOIN lan_node_tiers t ON t.tier_id = o.tier_id "
                    + " GROUP BY o.institution_id) ";

    // ---------------------------------------------------------------- task 3

    // Brief 3(a): every institution with its contact person.
    // LEFT JOIN keeps an institution even when its contact row is missing.
    // EXISTS marks whether the registration fee is on record.
    private static final String REGISTERED_INSTITUTIONS =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       i.address AS `Address`, "
                    + "       DATE_FORMAT(i.registered_on, '%Y-%m-%d') AS `Registered on`, "
                    + "       i.status AS `Status`, c.full_name AS `Contact person`, "
                    + "       c.phone AS `Phone`, c.email AS `Email`, "
                    + "       CASE WHEN EXISTS (SELECT 1 FROM payments p "
                    + "                         WHERE p.institution_id = i.institution_id "
                    + "                           AND p.payment_type = 'registration') "
                    + "            THEN 'Yes' ELSE 'No' END AS `Fee paid` "
                    + "FROM institutions i "
                    + "LEFT JOIN contact_persons c ON c.institution_id = i.institution_id "
                    + "ORDER BY i.name";

    // Brief 3(b): a defaulter holds at least one unsettled overdue fine.
    private static final String DEFAULTERS =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       COUNT(*) AS `Months fined`, "
                    + "       GROUP_CONCAT(f.billing_month ORDER BY f.billing_month SEPARATOR ', ') AS `Months`, "
                    + "       SUM(f.fine_amount) AS `Fines owed`, "
                    + "       CASE WHEN EXISTS (SELECT 1 FROM disconnections d "
                    + "                         WHERE d.institution_id = i.institution_id "
                    + "                           AND d.reconnected = FALSE) "
                    + "            THEN 'Disconnected' ELSE 'Connected' END AS `Service` "
                    + "FROM institutions i "
                    + "JOIN overdue_fines f ON f.institution_id = i.institution_id "
                    + "WHERE f.settled = FALSE "
                    + "GROUP BY i.institution_id, i.name, i.type "
                    + "ORDER BY i.name";

    // Brief 3(c): every disconnection on record, open ones first.
    private static final String DISCONNECTION_ISSUES =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       c.phone AS `Contact phone`, "
                    + "       DATE_FORMAT(d.disconnected_on, '%Y-%m-%d') AS `Disconnected on`, "
                    + "       CASE WHEN d.reconnected THEN 'Reconnected' ELSE 'Still disconnected' END AS `State`, "
                    + "       DATE_FORMAT(d.reconnected_on, '%Y-%m-%d') AS `Reconnected on` "
                    + "FROM disconnections d "
                    + "JOIN institutions i ON i.institution_id = d.institution_id "
                    + "LEFT JOIN contact_persons c ON c.institution_id = i.institution_id "
                    + "ORDER BY d.reconnected, d.disconnected_on, i.name";

    // Brief 3(d): the latest site visit plus everything bought, one row per institution.
    private static final String INFRASTRUCTURE =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, "
                    + "       DATE_FORMAT(ra.visit_date, '%Y-%m-%d') AS `Visit date`, "
                    + "       ra.user_count AS `Users`, "
                    + "       CASE ra.has_computers WHEN 1 THEN 'Yes' WHEN 0 THEN 'No' END AS `Has PCs`, "
                    + "       CASE ra.has_lan WHEN 1 THEN 'Yes' WHEN 0 THEN 'No' END AS `Has LAN`, "
                    + "       CASE ra.is_ready WHEN 1 THEN 'Ready' WHEN 0 THEN 'Not ready' "
                    + "            ELSE 'Not visited' END AS `Readiness`, "
                    + "       COALESCE(e.pcs, 0) AS `PCs bought`, "
                    + "       COALESCE(e.tiers, 'None') AS `LAN tier`, "
                    + "       COALESCE(e.pc_cost, 0.00) AS `PC cost`, "
                    + "       COALESCE(e.lan_cost, 0.00) AS `LAN cost`, "
                    + "       COALESCE(e.total, 0.00) AS `Equipment total` "
                    + "FROM institutions i "
                    + "LEFT JOIN readiness_assessments ra "
                    + "       ON ra.assessment_id = (SELECT r2.assessment_id FROM readiness_assessments r2 "
                    + "                              WHERE r2.institution_id = i.institution_id "
                    + "                              ORDER BY r2.visit_date DESC, r2.assessment_id DESC "
                    + "                              LIMIT 1) "
                    + "LEFT JOIN " + EQUIPMENT_TOTALS + "e ON e.institution_id = i.institution_id "
                    + "ORDER BY i.name";

    // ---------------------------------------------------------------- task 4

    // Brief 4(a), reading A3: registration fee + installation fee + PCs + LAN.
    // Fees come from payments (money recorded), equipment from equipment_orders.
    // Each side is totalled in its own derived table first, so joining both
    // cannot multiply rows (two payments x three orders would give six rows).
    // The two ? marks take REGISTRATION and INSTALLATION, in that order.
    private static final String INSTALLATION_COST =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       COALESCE(p.reg, 0.00) AS `Registration`, "
                    + "       COALESCE(p.inst, 0.00) AS `Installation`, "
                    + "       COALESCE(e.pc_cost, 0.00) AS `PCs`, "
                    + "       COALESCE(e.lan_cost, 0.00) AS `LAN`, "
                    + "       COALESCE(p.reg, 0.00) + COALESCE(p.inst, 0.00) "
                    + "     + COALESCE(e.pc_cost, 0.00) + COALESCE(e.lan_cost, 0.00) AS `Total installation cost` "
                    + "FROM institutions i "
                    + "LEFT JOIN (SELECT institution_id, "
                    + "                  SUM(CASE WHEN payment_type = ? THEN amount ELSE 0.00 END) AS reg, "
                    + "                  SUM(CASE WHEN payment_type = ? THEN amount ELSE 0.00 END) AS inst "
                    + "           FROM payments GROUP BY institution_id) p "
                    + "       ON p.institution_id = i.institution_id "
                    + "LEFT JOIN " + EQUIPMENT_TOTALS + "e ON e.institution_id = i.institution_id "
                    + "ORDER BY i.name";

    // Brief 4(b), reading B1: every institution that bought any equipment.
    // A plain JOIN (not LEFT) drops institutions with no order, which is the point here.
    private static final String EQUIPMENT_COST =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       CASE WHEN e.pcs > 0 AND e.lan_cost > 0 THEN 'PCs and LAN' "
                    + "            WHEN e.pcs > 0 THEN 'PCs only' "
                    + "            ELSE 'LAN only' END AS `Bought`, "
                    + "       e.pcs AS `PCs`, e.pc_cost AS `PC cost`, "
                    + "       COALESCE(e.tiers, 'None') AS `LAN tier`, e.lan_cost AS `LAN cost`, "
                    + "       e.total AS `Total` "
                    + "FROM " + EQUIPMENT_TOTALS + "e "
                    + "JOIN institutions i ON i.institution_id = e.institution_id "
                    + "ORDER BY i.name";

    // Brief 4(c): one row per upgrade, using the rate stored on the upgrade row.
    // Decision B1: the discount applies in the upgrade month only; later months pay full price.
    // bandwidth_plans is joined twice under two aliases: bo (old plan) and bn (new plan).
    private static final String UPGRADED_CHARGES =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       DATE_FORMAT(u.upgraded_on, '%Y-%m-%d') AS `Upgraded on`, "
                    + "       CONCAT(bo.mbps, ' to ', bn.mbps, ' Mbps') AS `Upgrade`, "
                    + "       CONCAT(ROUND(u.discount_rate * 100), '%') AS `Discount`, "
                    + "       ROUND(bn.monthly_cost * u.discount_rate, 2) AS `Discount amount`, "
                    + "       ROUND(bn.monthly_cost * (1 - u.discount_rate), 2) AS `Upgrade-month charge`, "
                    + "       bn.monthly_cost AS `Monthly charge after` "
                    + "FROM upgrades u "
                    + "JOIN subscriptions s ON s.subscription_id = u.subscription_id "
                    + "JOIN institutions i ON i.institution_id = s.institution_id "
                    + "JOIN bandwidth_plans bo ON bo.plan_id = u.old_plan_id "
                    + "JOIN bandwidth_plans bn ON bn.plan_id = u.new_plan_id "
                    + "ORDER BY i.name, u.upgraded_on";

    // Brief 4(d), reading D1: money collected per institution category.
    // A late month is one monthly payment holding bill + fine (8050 = 7000 + 1050),
    // so the bill part is monthly payments minus the fines settled with them.
    // Unsettled fines are not collected money; they belong to 3(b) instead.
    // Payments and fines are totalled per institution first (no fan-out), then grouped by type.
    // FIELD() returns the position of type in its list, giving school order, not A to Z.
    // The two ? marks take MONTHLY and RECONNECTION, in that order.
    private static final String CHARGES_BY_CATEGORY =
            "SELECT i.type AS `Category`, "
                    + "       COUNT(*) AS `Institutions`, "
                    + "       SUM(COALESCE(p.monthly, 0.00)) - SUM(COALESCE(f.fines, 0.00)) AS `Monthly charges`, "
                    + "       SUM(COALESCE(f.fines, 0.00)) AS `Overdue fines`, "
                    + "       SUM(COALESCE(p.reconnection, 0.00)) AS `Reconnection fees`, "
                    + "       SUM(COALESCE(p.monthly, 0.00)) + SUM(COALESCE(p.reconnection, 0.00)) AS `Total collected` "
                    + "FROM institutions i "
                    + "LEFT JOIN (SELECT institution_id, "
                    + "                  SUM(CASE WHEN payment_type = ? THEN amount ELSE 0.00 END) AS monthly, "
                    + "                  SUM(CASE WHEN payment_type = ? THEN amount ELSE 0.00 END) AS reconnection "
                    + "           FROM payments GROUP BY institution_id) p "
                    + "       ON p.institution_id = i.institution_id "
                    + "LEFT JOIN (SELECT institution_id, SUM(fine_amount) AS fines "
                    + "           FROM overdue_fines WHERE settled = TRUE "
                    + "           GROUP BY institution_id) f "
                    + "       ON f.institution_id = i.institution_id "
                    + "GROUP BY i.type "
                    + "ORDER BY FIELD(i.type, 'primary', 'junior', 'senior', 'college')";

    // Brief 4(e), reading E2: one row per institution, one column per service (a pivot).
    // Each SUM(CASE ...) keeps only one payment type; the ? marks take the four types in order.
    // Monthly includes any fine paid with a late bill.
    private static final String AMOUNT_PER_SERVICE =
            "SELECT i.institution_id AS `ID`, i.name AS `Institution`, i.type AS `Category`, "
                    + "       SUM(CASE WHEN p.payment_type = ? THEN p.amount ELSE 0.00 END) AS `Registration`, "
                    + "       SUM(CASE WHEN p.payment_type = ? THEN p.amount ELSE 0.00 END) AS `Installation`, "
                    + "       SUM(CASE WHEN p.payment_type = ? THEN p.amount ELSE 0.00 END) AS `Monthly`, "
                    + "       SUM(CASE WHEN p.payment_type = ? THEN p.amount ELSE 0.00 END) AS `Reconnection`, "
                    + "       COALESCE(SUM(p.amount), 0.00) AS `Total` "
                    + "FROM institutions i "
                    + "LEFT JOIN payments p ON p.institution_id = i.institution_id "
                    + "GROUP BY i.institution_id, i.name, i.type "
                    + "ORDER BY i.name";

    // ---------------------------------------------------------------- public methods

    public ReportTable registeredInstitutions() throws SQLException {
        return run("3(a) Registered institutions", REGISTERED_INSTITUTIONS);
    }

    public ReportTable defaulters() throws SQLException {
        return run("3(b) Defaulters", DEFAULTERS);
    }

    public ReportTable disconnectionIssues() throws SQLException {
        return run("3(c) Institutions with disconnection issues", DISCONNECTION_ISSUES);
    }

    public ReportTable infrastructureRequirements() throws SQLException {
        return run("3(d) Infrastructure requirements per institution", INFRASTRUCTURE);
    }

    public ReportTable installationCost() throws SQLException {
        return run("4(a) Total installation cost per institution", INSTALLATION_COST,
                REGISTRATION, INSTALLATION)
                .withTotalRow("TOTAL", 1);                 // label goes in the Institution column
    }

    public ReportTable equipmentCost() throws SQLException {
        return run("4(b) PC and LAN cost per institution", EQUIPMENT_COST)
                .withTotalRow("TOTAL", 1);
    }

    public ReportTable upgradedCharges() throws SQLException {
        return run("4(c) Monthly charges for upgraded services", UPGRADED_CHARGES)
                .withTotalRow("TOTAL", 1);
    }

    public ReportTable chargesByCategory() throws SQLException {
        return run("4(d) Monthly charges, fines and reconnection fees by category",
                CHARGES_BY_CATEGORY, MONTHLY, RECONNECTION)
                .withTotalRow("TOTAL", 0);                 // label goes in the Category column
    }

    public ReportTable amountPerService() throws SQLException {
        return run("4(e) Amount per service by institution", AMOUNT_PER_SERVICE,
                REGISTRATION, INSTALLATION, MONTHLY, RECONNECTION)
                .withTotalRow("TOTAL", 1);
    }

    // One runner for every report. Object... params is varargs: zero or more values,
    // bound to the ? marks in the order they appear in the SQL text.
    private ReportTable run(String title, String sql, Object... params) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int k = 0; k < params.length; k++) {
                ps.setObject(k + 1, params[k]);              // JDBC counts parameters from 1
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();     // describes the columns, not the rows
                int n = md.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int c = 1; c <= n; c++) {
                    columns.add(md.getColumnLabel(c));       // the alias; getColumnName gives the raw name
                }
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int c = 1; c <= n; c++) {
                        row[c - 1] = rs.getObject(c);        // DECIMAL arrives as BigDecimal, NULL as null
                    }
                    rows.add(row);
                }
                return new ReportTable(title, columns, rows);
            }
        }
    }
}