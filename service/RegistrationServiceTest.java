package service;

public class RegistrationServiceTest {

    public static void main(String[] args) {
        RegistrationService svc = new RegistrationService();

        // 1. A valid registration
        attempt(svc, "Test Academy", "junior", "Nairobi", "Jane Doe", "0712 345 678", "jane@example.org");
        // 2. The same name again
        attempt(svc, "Test Academy", "junior", "Nairobi", "Jane Doe", "0712345678", "jane@example.org");
        // 3. A type outside the four allowed values
        attempt(svc, "Another School", "university", "Kisumu", "John Doe", "0700000000", "");
        // 4. A blank name
        attempt(svc, "   ", "primary", "Kisumu", "John Doe", "0700000000", "");
         // 5. A phone number with hyphens
        attempt(svc, "Hill View School", "primary", "Nakuru", "Ann Mwangi", "0712-345-678", "");
        // 6. An email address without @
        attempt(svc, "Hill View School", "primary", "Nakuru", "Ann Mwangi", "0712345678", "ann.example.org");
        // 7. A blank contact name
        attempt(svc, "Hill View School", "primary", "Nakuru", "  ", "0712345678", "");
    }

    private static void attempt(RegistrationService svc, String name, String type, String address,
                                String contact, String phone, String email) {
        try {
            int id = svc.register(name, type, address, contact, phone, email);
            System.out.println("Saved, institution id " + id);
        } catch (ServiceException e) {
            System.out.println("Refused: " + e.getMessage());
        }
    }
}
