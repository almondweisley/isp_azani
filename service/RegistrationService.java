package service;

import dao.InstitutionDAO;
import model.ContactPerson;
import model.Institution;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.regex.Pattern;


public class RegistrationService{

    // Must match the CHECK constraint on institutions.type in the DDL
    private static final Set<String> TYPES = Set.of("primary", "junior", "senior", "college");

    // MySQL error code for a duplicate value in a UNIQUE column
    private static final int DUPLICATE_KEY = 1062;

    private static final Pattern PHONE = Pattern.compile("\\+?[0-9]{9,15}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final InstitutionDAO dao = new InstitutionDAO();

    public int register(String name, String type, String address, String contactName, String phone, String email)
    throws ServiceException {
        String n = clean(name);
        String t = clean(type).toLowerCase();
        String a = clean(address);
        String cn = clean(contactName);
        String ph = clean(phone).replaceAll("\\s+", "");
        String em = clean(email);

        if (n.isEmpty()) {
            throw new ServiceException("Enter the institution name.");
        }
        if (n.length() > 100) {
            throw new ServiceException("The institution name is longer than 100 characters");

        }
        if (!TYPES.contains(t)) {
            throw new ServiceException ("Choose primary, junior, senior or college as the type.");
        }
        if (a.length() > 150) {
            throw new ServiceException ("The address is longer than 150 characters");
        }
        if (cn.isEmpty()) {
            throw new ServiceException("Enter the contact person's name.");
        }
        if (cn.length() > 100) {
            throw new ServiceException("The contact name is longer than 100 characters.");
        }
        if (!ph.isEmpty() && !PHONE.matcher(ph).matches()) {
            throw new ServiceException("The phone number needs 9 to 15 digits, with an optional leading +.");
        }
        if (!em.isEmpty() && (em.length() > 100 || !EMAIL.matcher(em).matches())) {
            throw new ServiceException("The email address is not valid.");
        }

        ContactPerson contact = new ContactPerson(cn, blankToNull(ph), blankToNull(em));
        Institution inst = new Institution(n, t, blankToNull(a), contact);

        // Business rule (assumption for the report): a new registration is active and dated today
        inst.setRegisteredOn(LocalDate.now());
        inst.setStatus("active");

        try {
            return dao.saveWithContact(inst);
        } catch (SQLException e) {
            if (e.getErrorCode() == DUPLICATE_KEY) {
                throw new ServiceException("An institution named " + n + " is already registered.", e);
            }
            throw new ServiceException("The registration could not be saved. Check that MySQL is running.", e);
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
