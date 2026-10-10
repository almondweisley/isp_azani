package model;

import java.math.BigDecimal;
import java.time.YearMonth;

public class OverdueFine {

    private int fineId;
    private final int institutionId;
    private final YearMonth billingMonth;
    private final BigDecimal fineAmount;
    private boolean settled;

    //When reading a saved row
    public OverdueFine(int fineId, int institutionId, YearMonth billingMonth, BigDecimal fineAmount, boolean settled) {
        this.fineId = fineId;
        this.institutionId = institutionId;
        this.billingMonth = billingMonth;
        this.fineAmount = fineAmount;
        this.settled = settled;
    }

    //Static factory method: The only place a new fine gets its amount
    // The rate comes from fees

    public static OverdueFine forUnpaidBill(int institutionId, YearMonth billingMonth, BigDecimal amountDue) {
        BigDecimal fine = amountDue.multiply(Fees.FINE_RATE).setScale(Fees.MONEY_SCALE, Fees.ROUNDING);
        return new OverdueFine(0, institutionId, billingMonth, fine, false);
    }

    public int getFineId() {
        return fineId;
    }
    public int getInstitutionId() {
        return institutionId;
    }
    public YearMonth getBillingMonth() {
        return billingMonth;
    }
    public BigDecimal getFineAmount() {
        return fineAmount;
    }
    public boolean isSettled() {
        return settled;
    }
    public void setFineId(int fineId) {
        this.fineId = fineId;
    }
    public void markSettled() {
        this.settled = true;
    }
}