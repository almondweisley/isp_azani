package model;

import java.math.BigDecimal;

// One row of brief Table 1: a bandwidth and its monthly price.
public class BandwidthPlan {

    private final int planId;
    private final int mbps;
    private final BigDecimal monthlyCost;   // money stays in BigDecimal, never double

    public BandwidthPlan(int planId, int mbps, BigDecimal monthlyCost) {
        this.planId = planId;
        this.mbps = mbps;
        this.monthlyCost = monthlyCost;
    }

    public int getPlanId()             { return planId; }
    public int getMbps()               { return mbps; }
    public BigDecimal getMonthlyCost() { return monthlyCost; }

    // JComboBox calls toString() to draw each item, so the form in part 3 shows this text.
    @Override
    public String toString() {
        return String.format("%d Mbps - KSh %,.2f", mbps, monthlyCost);
    }
}