package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

// Decision D1a: a late bill and its fine travel in one 'monthly' payment row.
// getType() is inherited, so the row still says "monthly".
public class LateMonthlyPayment extends MonthlyPayment {

    private final BigDecimal fineAmount;

    public LateMonthlyPayment(int institutionId, LocalDate paidOn, YearMonth billingMonth,
                              BigDecimal planCost, BigDecimal discountRate, BigDecimal fineAmount) {
        super(institutionId, paidOn, billingMonth, planCost, discountRate);
        this.fineAmount = fineAmount;
    }

    public BigDecimal billAmount()    { return super.amountDue(); }   // the bill alone
    public BigDecimal getFineAmount() { return fineAmount; }

    // Overriding: PaymentDAO and BillingDAO call amountDue() and get bill plus fine,
    // without ever asking which subclass they hold.
    @Override
    public BigDecimal amountDue() {
        return money(super.amountDue().add(fineAmount));
    }
}