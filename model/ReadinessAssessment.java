package model;

import java.time.LocalDate;

// The findings of one site visit, plus the order it led to, if any.
public class ReadinessAssessment {

    private int assessmentId;
    private final int institutionId;
    private final LocalDate visitDate;
    private final int userCount;
    private final boolean hasComputers;
    private final boolean hasLan;
    private EquipmentOrder order;        // association: a failed visit may lead to one order

    public ReadinessAssessment(int institutionId, LocalDate visitDate, int userCount,
                               boolean hasComputers, boolean hasLan) {
        this.institutionId = institutionId;
        this.visitDate = visitDate;
        this.userCount = userCount;
        this.hasComputers = hasComputers;
        this.hasLan = hasLan;
    }

    // The brief's readiness rule lives here, in one place: computers AND a LAN.
    public boolean isReady() {
        return hasComputers && hasLan;
    }

    public String summary() {
        if (isReady()) {
            return "ready for installation; record the installation fee on the payment screen";
        }
        if (order == null) {
            return "not ready; nothing ordered yet";
        }
        return "not ready; order: computers KSh " + order.computerCost()
                + " + LAN KSh " + order.lanCost() + " = KSh " + order.totalCost();
    }

    public int getAssessmentId() { return assessmentId; }
    public void setAssessmentId(int assessmentId) { this.assessmentId = assessmentId; }
    public int getInstitutionId() { return institutionId; }
    public LocalDate getVisitDate() { return visitDate; }
    public int getUserCount() { return userCount; }
    public boolean hasComputers() { return hasComputers; }
    public boolean hasLan() { return hasLan; }
    public EquipmentOrder getOrder() { return order; }
    public void setOrder(EquipmentOrder order) { this.order = order; }
}