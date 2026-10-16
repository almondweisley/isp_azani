package dao;

import model.ReportTable;

import java.sql.SQLException;

// Developer check for step 8. Task 3 passed on 2026-10-15, so its lines are
// commented out to keep the console short. Remove the // to print them again.
// The console is your screen, not the user's, so printing the message here is fine.
public class ReportDAOTest {

    public static void main(String[] args) {
        ReportDAO dao = new ReportDAO();
        try {
            // print(dao.registeredInstitutions());
            print(dao.defaulters());
            print(dao.disconnectionIssues());
            // print(dao.infrastructureRequirements());
            print(dao.installationCost());
            print(dao.equipmentCost());
            print(dao.upgradedCharges());
            print(dao.chargesByCategory());
            print(dao.amountPerService());
        } catch (SQLException e) {
            System.out.println("Report failed: " + e.getMessage());
        }
    }

    private static void print(ReportTable table) {
        System.out.println(table.toText());
    }
}