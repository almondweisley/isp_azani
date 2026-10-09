package service;

import db.DBConnection;
import model.BandwidthPlan;
import model.Subscription;
import model.Upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.YearMonth;

// Exercises subscribe, upgrade and monthlyCharge, then deletes its own rows.
public class SubscriptionServiceTest {

    private static final int INSTITUTION_ID = 0;   // replace with the ID from the query

    public static void main(String[] args) {
        SubscriptionService service = new SubscriptionService();
        Subscription sub = null;
        try {
            int plan4 = planId(service, 4);
            int plan10 = planId(service, 10);
            int plan25 = planId(service, 25);

            sub = service.subscribe(INSTITUTION_ID, plan10, LocalDate.of(2026, 10, 1));
            System.out.println("1. Subscribed: subscription " + sub.getSubscriptionId());

            try {
                service.subscribe(INSTITUTION_ID, plan10, LocalDate.of(2026, 10, 1));
                System.out.println("2. FAIL: a second subscription was accepted");
            } catch (ServiceException e) {
                System.out.println("2. Refused as expected: " + e.getMessage());
            }

            try {
                service.upgrade(INSTITUTION_ID, plan4, LocalDate.of(2026, 11, 3));
                System.out.println("3. FAIL: a downgrade was accepted");
            } catch (ServiceException e) {
                System.out.println("3. Refused as expected: " + e.getMessage());
            }

            Upgrade u = service.upgrade(INSTITUTION_ID, plan25, LocalDate.of(2026, 11, 3));
            System.out.println("4. Upgraded: upgrade " + u.getUpgradeId()
                    + ", discount rate " + u.getDiscountRate());

            System.out.println("5. October 2026:  " + service.monthlyCharge(INSTITUTION_ID, YearMonth.of(2026, 10)));
            System.out.println("6. November 2026: " + service.monthlyCharge(INSTITUTION_ID, YearMonth.of(2026, 11)));
            System.out.println("7. December 2026: " + service.monthlyCharge(INSTITUTION_ID, YearMonth.of(2026, 12)));

        } catch (ServiceException e) {
            System.out.println("Unexpected: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
            }
        } finally {
            cleanUp(sub);                          // runs even when a step above fails
        }
    }

    private static int planId(SubscriptionService service, int mbps) throws ServiceException {
        for (BandwidthPlan p : service.listPlans()) {
            if (p.getMbps() == mbps) {
                return p.getPlanId();
            }
        }
        throw new ServiceException("bandwidth_plans holds no " + mbps + " Mbps row.");
    }

    // Deletes this run's rows so the next run starts from the same state.
    private static void cleanUp(Subscription sub) {
        if (sub == null || sub.getSubscriptionId() == 0) {
            return;                                // nothing was saved
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement delUpgrades = con.prepareStatement(
                     "DELETE FROM upgrades WHERE subscription_id = ?");
             PreparedStatement delSub = con.prepareStatement(
                     "DELETE FROM subscriptions WHERE subscription_id = ?")) {
            // Child rows go first; the foreign key would block deleting the parent.
            delUpgrades.setInt(1, sub.getSubscriptionId());
            delUpgrades.executeUpdate();
            delSub.setInt(1, sub.getSubscriptionId());
            delSub.executeUpdate();
            System.out.println("Clean-up: test rows removed.");
        } catch (Exception e) {
            System.out.println("Clean-up failed: " + e.getMessage());
        }
    }
}