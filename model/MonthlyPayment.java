package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class MonthlyPayment extends Payment {

    private final YearMonth billingMonth;     // for example 2025-04
    private final BigDecimal planCost;        // price of the plan in force that month
    private final BigDecimal discountRate;    // 0.10 in the upgrade month, otherwise 0.00

    public MonthlyPayment(int institutionId, LocalDate paidOn, YearMonth billingMonth,
                          BigDecimal planCost, BigDecimal discountRate) {
        super(institutionId, paidOn);
        this.billingMonth = billingMonth;
        this.planCost = planCost;
        this.discountRate = discountRate;
    }

    @Override
    public String getType() { return "monthly"; }

    @Override
    public BigDecimal amountDue() {
        BigDecimal payableShare = BigDecimal.ONE.subtract(discountRate);   // 1 - 0.10 = 0.90
        return money(planCost.multiply(payableShare));
    }

    @Override
    public String getBillingMonth() { return billingMonth.toString(); }   // "2025-04", same format as the sample data

    public YearMonth getYearMonth() { return billingMonth; }
    public BigDecimal getPlanCost() { return planCost; }
    public BigDecimal getDiscountRate() { return discountRate; }
}