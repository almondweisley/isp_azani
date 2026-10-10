package model;

import java.math.BigDecimal;
import java.time.LocalDate;

// A history row: the subscription moved from oldPlanId to newPlanId on upgradedOn.
public class Upgrade {

    private int upgradeId;
    private final int subscriptionId;
    private final int oldPlanId;
    private final int newPlanId;
    private final BigDecimal discountRate;   // 0.10 from Fees, stored so a later rate change leaves old rows intact
    private final LocalDate upgradedOn;

    public Upgrade(int subscriptionId, int oldPlanId, int newPlanId,
                   BigDecimal discountRate, LocalDate upgradedOn) {
        this.subscriptionId = subscriptionId;
        this.oldPlanId = oldPlanId;
        this.newPlanId = newPlanId;
        this.discountRate = discountRate;
        this.upgradedOn = upgradedOn;
    }

    public int getUpgradeId()            { return upgradeId; }
    public int getSubscriptionId()       { return subscriptionId; }
    public int getOldPlanId()            { return oldPlanId; }
    public int getNewPlanId()            { return newPlanId; }
    public BigDecimal getDiscountRate()  { return discountRate; }
    public LocalDate getUpgradedOn()     { return upgradedOn; }

    public void setUpgradeId(int id)     { this.upgradeId = id; }
}