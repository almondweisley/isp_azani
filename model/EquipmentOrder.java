package model;

import java.math.BigDecimal;
import java.time.LocalDate;

// What an institution that failed the visit buys from Azani.
public class EquipmentOrder {

    private int orderId;
    private final int institutionId;
    private final int computerQty;
    private final LANNodeTier tier;      // null when the institution buys computers only
    private final LocalDate orderDate;

    public EquipmentOrder(int institutionId, int computerQty, LANNodeTier tier, LocalDate orderDate) {
        this.institutionId = institutionId;
        this.computerQty = computerQty;
        this.tier = tier;
        this.orderDate = orderDate;
    }

    // Task 4(b): KSh 40,000 per computer, read from Fees.
    public BigDecimal computerCost() {
        return Fees.PC_PRICE.multiply(BigDecimal.valueOf(computerQty))
                .setScale(Fees.MONEY_SCALE, Fees.ROUNDING);
    }

    // Task 4(b): the tier price from Table 2, or zero when no LAN was bought.
    public BigDecimal lanCost() {
        BigDecimal cost = (tier == null) ? BigDecimal.ZERO : tier.getCost();
        return cost.setScale(Fees.MONEY_SCALE, Fees.ROUNDING);
    }

    public BigDecimal totalCost() {
        return computerCost().add(lanCost());
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public int getInstitutionId() { return institutionId; }
    public int getComputerQty() { return computerQty; }
    public LANNodeTier getTier() { return tier; }
    public LocalDate getOrderDate() { return orderDate; }
}