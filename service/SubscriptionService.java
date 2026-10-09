package service;

import dao.ServiceDAO;
import model.BandwidthPlan;
import model.Fees;                 // change or delete to match where your Fees class lives
import model.Subscription;
import model.Upgrade;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// Business rules for subscribing, upgrading and pricing a month of service.
public class SubscriptionService {

    // Must match the CHECK constraint on payments.payment_type.
    private static final String REGISTRATION = "registration";
    private static final String INSTALLATION = "installation";

    private final ServiceDAO dao = new ServiceDAO();

    // For the form's combo box in part 3.
    public List<BandwidthPlan> listPlans() throws ServiceException {
        try {
            return dao.listPlans();
        } catch (SQLException e) {
            throw new ServiceException("Could not load the bandwidth plans.", e);
        }
    }

    // Decision C: registration fee, installation fee, a ready latest visit, a valid plan.
    public Subscription subscribe(int institutionId, int planId, LocalDate startDate)
            throws ServiceException {
        if (startDate == null) {
            throw new ServiceException("Enter a start date.");
        }
        try {
            if (!dao.hasPayment(institutionId, REGISTRATION)) {
                throw new ServiceException("Record the registration fee before subscribing.");
            }
            if (!dao.hasPayment(institutionId, INSTALLATION)) {
                throw new ServiceException("Record the installation fee before subscribing.");
            }
            Boolean ready = dao.latestVisitReady(institutionId);
            if (ready == null) {
                throw new ServiceException("Record a site visit before subscribing.");
            }
            if (!ready) {
                throw new ServiceException("The latest site visit did not find the institution ready.");
            }
            if (dao.findByInstitution(institutionId) != null) {
                throw new ServiceException("This institution already has a subscription. Use Upgrade instead.");
            }
            if (dao.findPlan(planId) == null) {
                throw new ServiceException("Choose a bandwidth plan.");
            }
            Subscription s = new Subscription(institutionId, planId, startDate);
            dao.insertSubscription(s);          // fills in the new subscription_id
            return s;
        } catch (SQLException e) {
            throw new ServiceException("Could not save the subscription.", e);
        }
    }

    // Brief: an upgrade moves to a higher bandwidth and carries the 10 percent discount.
    public Upgrade upgrade(int institutionId, int newPlanId, LocalDate upgradedOn)
            throws ServiceException {
        if (upgradedOn == null) {
            throw new ServiceException("Enter an upgrade date.");
        }
        try {
            Subscription sub = dao.findByInstitution(institutionId);
            if (sub == null) {
                throw new ServiceException("This institution has no subscription to upgrade.");
            }
            if (!Subscription.ACTIVE.equals(sub.getStatus())) {
                throw new ServiceException("Reconnect the institution before upgrading.");
            }
            if (upgradedOn.isBefore(sub.getStartDate())) {
                throw new ServiceException("The upgrade date falls before the subscription start date.");
            }
            BandwidthPlan current = dao.findPlan(sub.getPlanId());
            BandwidthPlan target = dao.findPlan(newPlanId);
            if (target == null) {
                throw new ServiceException("Choose a bandwidth plan.");
            }
            if (target.getMbps() <= current.getMbps()) {
                throw new ServiceException("An upgrade must move above " + current.getMbps() + " Mbps.");
            }
            Upgrade u = new Upgrade(sub.getSubscriptionId(), current.getPlanId(),
                    target.getPlanId(), Fees.UPGRADE_DISCOUNT, upgradedOn);
            dao.recordUpgrade(u);                // one transaction: history row plus plan change
            return u;
        } catch (SQLException e) {
            throw new ServiceException("Could not save the upgrade.", e);
        }
    }

    // The bill for one month. Decision B1: the discount applies in the upgrade month only.
    public BigDecimal monthlyCharge(int institutionId, YearMonth month) throws ServiceException {
        try {
            Subscription sub = dao.findByInstitution(institutionId);
            if (sub == null) {
                throw new ServiceException("This institution has no subscription.");
            }
            if (month.isBefore(YearMonth.from(sub.getStartDate()))) {
                throw new ServiceException("The subscription starts after " + month + ".");
            }

            // Replay the upgrades to find the plan held in this month.
            List<Upgrade> upgrades = dao.listUpgrades(sub.getSubscriptionId());
            int planId = upgrades.isEmpty() ? sub.getPlanId() : upgrades.get(0).getOldPlanId();
            Upgrade upgradeThisMonth = null;
            LocalDate monthEnd = month.atEndOfMonth();
            for (Upgrade u : upgrades) {
                if (u.getUpgradedOn().isAfter(monthEnd)) {
                    break;                       // later upgrades cannot change this month
                }
                planId = u.getNewPlanId();
                upgradeThisMonth = YearMonth.from(u.getUpgradedOn()).equals(month) ? u : null;
            }

            BigDecimal charge = dao.findPlan(planId).getMonthlyCost();
            if (upgradeThisMonth != null) {
                // Uses the rate stored on the row, so a future change to Fees leaves old bills alone.
                BigDecimal keep = BigDecimal.ONE.subtract(upgradeThisMonth.getDiscountRate());
                charge = charge.multiply(keep);
            }
            return charge.setScale(Fees.MONEY_SCALE, Fees.ROUNDING);
        } catch (SQLException e) {
            throw new ServiceException("Could not compute the monthly charge.", e);
        }
    }
}