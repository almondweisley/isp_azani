package dao;

import db.DBConnection;
import model.ContactPerson;
import model.Institution;
import model.InstitutionOption;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InstitutionDAO {

    private static final String INSERT_INSTITUTION =
            "INSERT INTO institutions (name, type, address, registered_on, status) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private static final String INSERT_CONTACT =
            "INSERT INTO contact_persons (institution_id, full_name, phone, email) "
                    + "VALUES (?, ?, ?, ?)";

    private static final String SELECT_OPTIONS =
            "SELECT institution_id, name FROM institutions ORDER BY name";

    // Saves the institution and its contact in one transaction.
    // Returns the new institution id.
    public int saveWithContact(Institution inst) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement psInt =
                         conn.prepareStatement(INSERT_INSTITUTION, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psContact =
                         conn.prepareStatement(INSERT_CONTACT)) {

                // Insert the institution
                psInt.setString(1, inst.getName());
                psInt.setString(2, inst.getType());
                psInt.setString(3, inst.getAddress());
                psInt.setDate(4, Date.valueOf(inst.getRegisteredOn()));
                psInt.setString(5, inst.getStatus());
                psInt.executeUpdate();

                // Read back the id MySQL generated with AUTO_INCREMENT
                int newId;
                try (ResultSet keys = psInt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("MySQL returned no institution_id.");
                    }
                    newId = keys.getInt(1);
                }

                ContactPerson contact = inst.getContact();
                contact.setInstitutionId(newId);
                psContact.setInt(1, newId);
                psContact.setString(2, contact.getFullName());
                psContact.setString(3, contact.getPhone());
                psContact.setString(4, contact.getEmail());
                psContact.executeUpdate();

                conn.commit();
                inst.setInstitutionId(newId);
                return newId;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();   // undo the institution row if the contact insert failed
                throw e;
            }
        }
    }

    // Every institution, sorted by name, for drop-down lists.
    // A single SELECT needs no transaction, so autocommit stays on.
    public List<InstitutionOption> listOptions() throws SQLException {
        List<InstitutionOption> options = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_OPTIONS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                options.add(new InstitutionOption(
                        rs.getInt("institution_id"),
                        rs.getString("name")));
            }
        }
        return options;
    }
}