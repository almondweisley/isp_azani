package model;

import java.time.LocalDate;

// One institution's internet service: which plan it holds and whether it is connected.
public class Subscription {

    // These two strings match the CHECK constraint on subscriptions.status.
    public static final String ACTIVE = "active";
    public static final String DISCONNECTED = "disconnected";

    private int subscriptionId;          // stays 0 until MySQL assigns one
    private final int institutionId;
    private int planId;                  // not final: an upgrade changes it
    private final LocalDate startDate;
    private String status;

    // Used when reading a saved row.
    public Subscription(int subscriptionId, int institutionId, int planId,
                        LocalDate startDate, String status) {
        this.subscriptionId = subscriptionId;
        this.institutionId = institutionId;
        this.planId = planId;
        this.startDate = startDate;
        this.status = status;
    }

    // Used for a new subscription; every new one starts active.
    public Subscription(int institutionId, int planId, LocalDate startDate) {
        this(0, institutionId, planId, startDate, ACTIVE);
    }

    public int getSubscriptionId()     { return subscriptionId; }
    public int getInstitutionId()      { return institutionId; }
    public int getPlanId()             { return planId; }
    public LocalDate getStartDate()    { return startDate; }
    public String getStatus()          { return status; }

    public void setSubscriptionId(int id) { this.subscriptionId = id; }
    public void setPlanId(int planId)     { this.planId = planId; }
}