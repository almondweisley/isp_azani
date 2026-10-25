package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * The numbers the dashboard shows, loaded in one trip to the database.
 * Immutable: every field is final and set once in the constructor.
 */
public final class DashboardStats {

    private final int institutions;
    private final int activeSubscriptions;
    private final int openDisconnections;
    private final BigDecimal collectedThisMonth;
    private final List<RecentPayment> recentPayments;

    public DashboardStats(int institutions, int activeSubscriptions, int openDisconnections,
                          BigDecimal collectedThisMonth, List<RecentPayment> recentPayments) {
        this.institutions = institutions;
        this.activeSubscriptions = activeSubscriptions;
        this.openDisconnections = openDisconnections;
        this.collectedThisMonth = collectedThisMonth;
        // unmodifiableList: the form can read the list but cannot change it
        this.recentPayments = Collections.unmodifiableList(recentPayments);
    }

    public int getInstitutions()              { return institutions; }
    public int getActiveSubscriptions()       { return activeSubscriptions; }
    public int getOpenDisconnections()        { return openDisconnections; }
    public BigDecimal getCollectedThisMonth() { return collectedThisMonth; }
    public List<RecentPayment> getRecentPayments() { return recentPayments; }

    /** One row of the "Recent payments" table. A static nested class needs no outer object. */
    public static final class RecentPayment {
        private final LocalDate paidOn;
        private final String institution;
        private final String type;
        private final BigDecimal amount;

        public RecentPayment(LocalDate paidOn, String institution, String type, BigDecimal amount) {
            this.paidOn = paidOn;
            this.institution = institution;
            this.type = type;
            this.amount = amount;
        }

        public LocalDate getPaidOn()   { return paidOn; }
        public String getInstitution() { return institution; }
        public String getType()        { return type; }
        public BigDecimal getAmount()  { return amount; }
    }
}