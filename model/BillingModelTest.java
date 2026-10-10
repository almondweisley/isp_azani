package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

// Checks the two models with no database. Run it like ModelTest.
public class BillingModelTest {

    public static void main(String[] args) {
        YearMonth oct = YearMonth.of(2026, 10);

        // 10 Mbps bill, 25 Mbps upgrade-month bill (4000 x 0.90), 4 Mbps bill.
        String[] bills = {"2000.00", "3600.00", "1200.00"};
        for (String bill : bills) {
            OverdueFine f = OverdueFine.forUnpaidBill(1, oct, new BigDecimal(bill));
            System.out.println("Fine on " + bill + " = " + f.getFineAmount() + ", settled " + f.isSettled());
        }

        Disconnection d = new Disconnection(1, LocalDate.of(2026, 11, 11));
        System.out.println("Open before reconnect: " + d.isOpen());
        d.reconnect(LocalDate.of(2026, 11, 15));
        System.out.println("Open after reconnect: " + d.isOpen() + ", on " + d.getReconnectedOn());

        try {
            d.reconnect(LocalDate.of(2026, 11, 20));        // second reconnect must fail
        } catch (IllegalStateException e) {
            System.out.println("Refused: " + e.getMessage());
        }
    }
}