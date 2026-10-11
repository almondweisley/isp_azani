package service;

import dao.BillingDAO;
import dao.PaymentDAO;
import dao.ServiceDAO;
import model.Disconnection;
import model.Fees;
import model.LateMonthlyPayment;
import model.MonthlyPayment;
import model.OverdueFine;
import model.Payment;
import model.ReconnectionPayment;
import model.Subscription;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

// The billing cycle: fines, disconnections, late payments and reconnection.
public class BillingService {

    // A record: a small immutable class whose constructor, getters,
    // equals() and toString() Java writes for you.
    public record BillingRun(int finesAdded, int disconnections) {
        BillingRun plus(BillingRun other) {
            return new BillingRun(finesAdded + other.finesAdded, disconnections + other.disconnections);
        }
    }

    private final BillingDAO billingDAO = new BillingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final PaymentService paymentService = new PaymentService();

    // Decision D3a: the clerk's Run billing button calls this.
    public BillingRun runBilling(LocalDate asOf) throws ServiceException {
        checkDate(asOf, "billing date");
        try {
            BillingRun total = new BillingRun(0, 0);
            for (int id : billingDAO.listSubscribedInstitutions()) {
                total = total.plus(bill(id, asOf));
            }
            return total;
        } catch (SQLException e) {
            throw new ServiceException("Could not run billing.", e);
        }
    }

    // The same run for one institution; the test uses it so other rows stay untouched.
    public BillingRun runBillingFor(int institutionId, LocalDate asOf) throws ServiceException {
        checkDate(asOf, "billing date");
        try {
            return bill(institutionId, asOf);
        } catch (SQLException e) {
            throw new ServiceException("Could not run billing.", e);
        }
    }

    // Walks every month that has ended by asOf. Safe to repeat: existing rows stop duplicates.
    private BillingRun bill(int institutionId, LocalDate asOf) throws SQLException {
        Subscription sub = serviceDAO.findByInstitution(institutionId);
        if (sub == null) {
            return new BillingRun(0, 0);
        }
        int fines = 0;
        int cuts = 0;
        YearMonth lastEnded = YearMonth.from(asOf).minusMonths(1);   // the current month is not yet due
        for (YearMonth m = YearMonth.from(sub.getStartDate()); !m.isAfter(lastEnded); m = m.plusMonths(1)) {
            if (billingDAO.cutOffOn(institutionId, m.atDay(1))) {
                continue;                                            // D5a: no bill for this month
            }
            if (billingDAO.monthlyPaidBy(institutionId, m, m.atEndOfMonth())) {
                continue;                                            // paid on time
            }
            if (billingDAO.findFine(institutionId, m) == null) {     // the 15 percent applies once
                billingDAO.insertFine(OverdueFine.forUnpaidBill(institutionId, m, billFor(institutionId, m)));
                fines++;
            }
            LocalDate graceEnd = m.plusMonths(1).atDay(Fees.DISCONNECT_DAY);   // the 10th of M+1
            LocalDate cutDate = graceEnd.plusDays(1);                          // D2a: the 11th
            if (asOf.isBefore(cutDate) || billingDAO.monthlyPaidBy(institutionId, m, graceEnd)) {
                continue;                                            // still in grace, or cleared in time
            }
            if (billingDAO.findOpenDisconnection(institutionId) != null
                    || billingDAO.hasDisconnectionOn(institutionId, cutDate)) {
                continue;                                            // already cut off, or already recorded
            }
            billingDAO.disconnect(new Disconnection(institutionId, cutDate), sub.getSubscriptionId());
            cuts++;
        }
        return new BillingRun(fines, cuts);
    }

    // On time: PaymentService records the plain bill. Late: bill plus fine (D1a).
    public Payment payMonthly(int institutionId, YearMonth month, LocalDate paidOn) throws ServiceException {
        if (month == null) {
            throw new ServiceException("Enter the billing month as yyyy-MM, for example 2026-08.");
        }
        checkDate(paidOn, "payment date");
        if (!paidOn.isAfter(month.atEndOfMonth())) {
            return paymentService.record(institutionId, "monthly", paidOn, month);
        }
        try {
            Subscription sub = serviceDAO.findByInstitution(institutionId);
            if (sub == null) {
                throw new ServiceException("This institution has no subscription, so it owes no monthly bill.");
            }
            if (month.isBefore(YearMonth.from(sub.getStartDate()))) {
                throw new ServiceException("The subscription starts after " + month + ".");
            }
            if (billingDAO.cutOffOn(institutionId, month.atDay(1))) {
                throw new ServiceException("The service was cut off when " + month + " began, so that month has no bill.");
            }
            if (paymentDAO.monthlyPaid(institutionId, month)) {
                throw new ServiceException("The " + month + " bill for this institution is already paid.");
            }
            PaymentDAO.BillBasis basis = paymentDAO.findBillBasis(institutionId, month);
            OverdueFine fine = billingDAO.findFine(institutionId, month);
            if (fine == null) {
                fine = OverdueFine.forUnpaidBill(institutionId, month, billFor(institutionId, month));
            }
            fine.markSettled();
            LateMonthlyPayment p = new LateMonthlyPayment(institutionId, paidOn, month,
                    basis.planCost, basis.discountRate, fine.getFineAmount());
            billingDAO.recordLatePayment(p, fine);
            return p;
        } catch (SQLException e) {
            throw new ServiceException("Could not save the late payment.", e);
        }
    }

    // Brief: reconnection follows payment of the bill and the fine, plus the reconnection fee.
    public Payment reconnect(int institutionId, LocalDate paidOn) throws ServiceException {
        checkDate(paidOn, "reconnection date");
        try {
            Disconnection open = billingDAO.findOpenDisconnection(institutionId);
            if (open == null) {
                throw new ServiceException("This institution has no open disconnection.");
            }
            if (paidOn.isBefore(open.getDisconnectedOn())) {
                throw new ServiceException("The reconnection date falls before the disconnection on "
                        + open.getDisconnectedOn() + ".");
            }
            bill(institutionId, paidOn);       // catch-up: fine any month that fell due since the last run
            if (billingDAO.countUnsettledFines(institutionId) > 0) {
                throw new ServiceException("Clear every overdue bill and fine before reconnecting.");
            }
            Subscription sub = serviceDAO.findByInstitution(institutionId);
            ReconnectionPayment p = new ReconnectionPayment(institutionId, paidOn);
            open.reconnect(paidOn);
            billingDAO.reconnect(open, p, sub.getSubscriptionId());
            return p;
        } catch (SQLException e) {
            throw new ServiceException("Could not save the reconnection.", e);
        }
    }

    // Reuses MonthlyPayment's formula, so the bill rule lives in one class.
    private BigDecimal billFor(int institutionId, YearMonth m) throws SQLException {
        PaymentDAO.BillBasis b = paymentDAO.findBillBasis(institutionId, m);
        return new MonthlyPayment(institutionId, m.atEndOfMonth(), m, b.planCost, b.discountRate).amountDue();
    }

    private static void checkDate(LocalDate d, String what) throws ServiceException {
        if (d == null) {
            throw new ServiceException("Enter the " + what + ".");
        }
        if (d.isAfter(LocalDate.now())) {
            throw new ServiceException("The " + what + " cannot fall after today.");
        }
    }
}