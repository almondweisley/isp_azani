package model;

public class ModelTest {
    public static void main(String[] args) {
        ContactPerson c = new ContactPerson("Mary Chebet", "+254711000111", "mary.chebet@example.org");
        Institution i = new Institution("Tumaini Primary School", "primary", "Eldoret, Uasin Gishu County", c);

        System.out.println(i.getInstitutionId());
        System.out.println(i.getContact().getFullName());
        System.out.println(i.getRegisteredOn());
    }
}