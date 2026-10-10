package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public abstract class Payment{
    
    private int paymentId;
    private final int institutionId;
    private final LocalDate paidOn;

    protected Payment(int institutionId, LocalDate paidOn) {
        this.institutionId = institutionId;
        this.paidOn = paidOn;
    }

    //The exact value stored in payment.payment_type
    public abstract String getType();

    //What the institution owes for this payment
    public abstract BigDecimal amountDue();

    public String getBillingMonth() {
        return null;
    }

    protected static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public int getPaymentId() {
        return paymentId;
    }
    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public LocalDate getPaidOn(){
        return paidOn;
    }
    

} 