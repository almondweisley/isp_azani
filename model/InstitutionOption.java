package model;

/** A light id-and-name pair for the PaymentForm combo box. */
public class InstitutionOption {

    private final int id;
    private final String name;

    public InstitutionOption(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }   // JComboBox calls toString() to draw each entry
}