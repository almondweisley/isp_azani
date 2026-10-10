package service;

import model.Payment;

import java.time.LocalDate;
import java.time.YearMonth;

public class PaymentServiceTest {

    public static void main(String[] args) {
        PaymentService service = new PaymentService();
        LocalDate today = LocalDate.now();
        int testAcademy = 33;   // your step 3 test row
        int kmtc = 19;          // Kenya Medical Training College in the sample data
        int lakeview = 30;      // Lakeview Primary School in the sample data

        run(service, "1. Test Academy registration", testAcademy, "registration", today, null);
        run(service, "2. Test Academy registration again", testAcademy, "registration", today, null);
        run(service, "3. Lakeview registration", lakeview, "registration", today, null);
        run(service, "4. KMTC April 2025 bill", kmtc, "monthly", today, YearMonth.of(2025, 4));
        run(service, "5. KMTC May 2025 bill", kmtc, "monthly", today, YearMonth.of(2025, 5));
        run(service, "6. Test Academy October 2026 bill", testAcademy, "monthly", today, YearMonth.of(2026, 10));
    }

    private static void run(PaymentService service, String label, int institutionId,
                            String type, LocalDate paidOn, YearMonth month) {
        try {
            Payment p = service.record(institutionId, type, paidOn, month);
            System.out.println(label + ": saved, KSh " + p.amountDue().toPlainString());
        } catch (ServiceException e) {
            System.out.println(label + ": refused. " + e.getMessage());
        }
    }
}