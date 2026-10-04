package dao;

import java.time.LocalDate;
import model.ContactPerson;
import model.Institution;

public class DAOTest {
    public static void main(String[] args) {
        ContactPerson c = new ContactPerson("Mary Chebet", "+254711000111", "mary.chebet@example.org");
        Institution i = new Institution("Tumaini Primary School", "primary", "Eldoret, Uasin Gishu County", c);
        i.setRegisteredOn(LocalDate.now());   // the service will do this later
        i.setStatus("inactive");              // placeholder until we settle the status rule

        try {
            int id = new InstitutionDAO().saveWithContact(i);
            System.out.println("Saved. institution_id = " + id);
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());   // test code only; the form will use a status label
        }
    }
}