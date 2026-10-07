package model;

import java.math.BigDecimal;

public class FeeTest {
    public static void main(String[] args){
        BigDecimal bill = new BigDecimal("7000.00");
        BigDecimal fine = bill.multiply(Fees.FINE_RATE).setScale(Fees.MONEY_SCALE, Fees.ROUNDING);
        System.out.println("Fine on 7000.00 " + fine);

        BigDecimal planCost = new BigDecimal("4000.00");
        BigDecimal discount = planCost.multiply(Fees.UPGRADE_DISCOUNT).setScale(Fees.MONEY_SCALE, Fees.ROUNDING);
        System.out.println("Pay for 25 MBPS after upgrade: " + planCost.subtract(discount));

        System.out.println("Twenty PCs: " + Fees.PC_PRICE.multiply(BigDecimal.valueOf(20)));
    }
}