package service;

import db.DBConnection;
import model.BandwidthPlan;
import model.Payment;
import model.Subscription;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// Walks one institution through July to September 2026, then deletes its own rows.
public class BillingServiceTest {

    private static final int INSTITUTION_ID = 33;   // replace before running

    public static void main(String[] args) {
        SubscriptionService subs = new SubscriptionService();
        BillingService billing = new BillingService();
        Subscription sub = null;
        List<Integer> paymentIds = new ArrayList<>();
        try {
            sub = subs.subscribe(INSTITUTION_ID, planId(subs, 10), LocalDate.of(2026, 7, 1));
            System.out.println("0. Subscribed from 2026-07-01, subscription " + sub.getSubscriptionId());

            Payment july = billing.payMonthly(INSTITUTION_ID, YearMonth.of(2026, 7), LocalDate.of(2026, 7, 28));
            paymentIds.add(july.getPaymentId());
            System.out.println("1. July paid on time: " + july.amountDue());

            print("2. Run to 2026-09-05", billing.runBillingFor(INSTITUTION_ID, LocalDate.of(2026, 9, 5)));
            print("3. Same run again", billing.runBillingFor(INSTITUTION_ID, LocalDate.of(2026, 9, 5)));
            print("4. Run to 2026-09-11", billing.runBillingFor(INSTITUTION_ID, LocalDate.of(2026, 9, 11)));

            try {
                billing.reconnect(INSTITUTION_ID, LocalDate.of(2026, 9, 11));
                System.out.println("5. FAIL: reconnected with the August bill unpaid");
            } catch (ServiceException e) {
                System.out.println("5. Refused as expected: " + e.getMessage());
            }

            Payment aug = billing.payMonthly(INSTITUTION_ID, YearMonth.of(2026, 8), LocalDate.of(2026, 9, 12));
            paymentIds.add(aug.getPaymentId());
            System.out.println("6. August paid late: " + aug.amountDue());

            Payment back = billing.reconnect(INSTITUTION_ID, LocalDate.of(2026, 9, 12));
            paymentIds.add(back.getPaymentId());
            System.out.println("7. Reconnected: " + back.amountDue());

            print("8. Run to 2026-09-15", billing.runBillingFor(INSTITUTION_ID, LocalDate.of(2026, 9, 15)));

        } catch (ServiceException e) {
            System.out.println("Unexpected: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
            }
        } finally {
            cleanUp(sub, paymentIds);
        }
    }

    private static void print(String label, BillingService.BillingRun run) {
        System.out.println(label + ": " + run.finesAdded() + " fine(s), "
                + run.disconnections() + " disconnection(s)");
    }

    private static int planId(SubscriptionService service, int mbps) throws ServiceException {
        for (BandwidthPlan p : service.listPlans()) {
            if (p.getMbps() == mbps) {
                return p.getPlanId();
            }
        }
        throw new ServiceException("bandwidth_plans holds no " + mbps + " Mbps row.");
    }

    // Deletes only this run's rows; the institution's registration and installation payments stay.
    private static void cleanUp(Subscription sub, List<Integer> paymentIds) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement delPay = con.prepareStatement("DELETE FROM payments WHERE payment_id = ?");
             PreparedStatement delFines = con.prepareStatement(
                     "DELETE FROM overdue_fines WHERE institution_id = ? AND billing_month BETWEEN '2026-07' AND '2026-09'");
             PreparedStatement delCuts = con.prepareStatement(
                     "DELETE FROM disconnections WHERE institution_id = ? AND disconnected_on BETWEEN ? AND ?");
             PreparedStatement delUpg = con.prepareStatement("DELETE FROM upgrades WHERE subscription_id = ?");
             PreparedStatement delSub = con.prepareStatement("DELETE FROM subscriptions WHERE subscription_id = ?")) {
            for (int id : paymentIds) {
                delPay.setInt(1, id);
                delPay.executeUpdate();
            }
            delFines.setInt(1, INSTITUTION_ID);
            delFines.executeUpdate();
            delCuts.setInt(1, INSTITUTION_ID);
            delCuts.setDate(2, Date.valueOf("2026-07-01"));
            delCuts.setDate(3, Date.valueOf("2026-09-30"));
            delCuts.executeUpdate();
            if (sub != null && sub.getSubscriptionId() != 0) {
                delUpg.setInt(1, sub.getSubscriptionId());      // child rows before the parent
                delUpg.executeUpdate();
                delSub.setInt(1, sub.getSubscriptionId());
                delSub.executeUpdate();
            }
            System.out.println("Clean-up: test rows removed.");
        } catch (Exception e) {
            System.out.println("Clean-up failed: " + e.getMessage());
        }
    }
}