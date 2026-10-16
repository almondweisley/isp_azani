package model;

// The nine reports the brief asks for, as a fixed set of named constants.
// An enum suits this: the list never changes while the program runs, and the
// compiler rejects any report name that is not on it.
public enum ReportType {

    REGISTERED_INSTITUTIONS("3(a) Registered institutions", "3a_registered_institutions"),
    DEFAULTERS("3(b) Defaulters", "3b_defaulters"),
    DISCONNECTION_ISSUES("3(c) Disconnection issues", "3c_disconnection_issues"),
    INFRASTRUCTURE("3(d) Infrastructure requirements", "3d_infrastructure"),
    INSTALLATION_COST("4(a) Total installation cost", "4a_installation_cost"),
    EQUIPMENT_COST("4(b) PC and LAN cost", "4b_pc_and_lan_cost"),
    UPGRADED_CHARGES("4(c) Upgraded service charges", "4c_upgraded_charges"),
    CHARGES_BY_CATEGORY("4(d) Charges, fines, reconnections by category", "4d_charges_by_category"),
    AMOUNT_PER_SERVICE("4(e) Amount per service by institution", "4e_amount_per_service");

    // Each constant carries its own data, set by the constructor above.
    private final String label;
    private final String fileName;

    // An enum constructor is always private: only the constants above can call it.
    ReportType(String label, String fileName) {
        this.label = label;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    // JComboBox shows toString(), so the drop-down lists the brief's wording.
    @Override
    public String toString() {
        return label;
    }
}