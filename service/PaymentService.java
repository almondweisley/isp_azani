package service;

import dao.InstitutionDAO;
import dao.PaymentDAO;
import model.InstallationPayment;
import model.InstitutionOption;
import model.MonthlyPayment;
import model.Payment;
import model.RegistrationPayment;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final InstitutionDAO institutionDAO = new InstitutionDAO();

    /** The form gets its drop-down list from here, never straight from a DAO. */
    public List<InstitutionOption> listInstitutions() throws ServiceException {
        try {
            return institutionDAO.listOptions();
        } catch (SQLException e) {
            throw new ServiceException("Could not load institutions: " + e.getMessage());
        }
    }

    /** Checks the business rules, builds the right Payment subclass and saves it. */
    public Payment record(int institutionId, String type, LocalDate paidOn, YearMonth billingMonth)
            throws ServiceException {
        if (paidOn == null) {
            throw new ServiceException("Enter the payment date.");
        }
        if (paidOn.isAfter(LocalDate.now())) {
            throw new ServiceException("The payment date cannot fall after today.");
        }
        try {
            Payment payment = build(institutionId, type, paidOn, billingMonth);
            paymentDAO.insert(payment);
            return payment;
        } catch (SQLException e) {
            throw new ServiceException("The database refused the payment: " + e.getMessage());
        }
    }

    /** Factory method: one switch decides which subclass to create. */
    private Payment build(int institutionId, String type, LocalDate paidOn, YearMonth billingMonth)
            throws ServiceException, SQLException {
        switch (type) {
            case "registration":
                if (paymentDAO.countByType(institutionId, "registration") > 0) {
                    throw new ServiceException("This institution has already paid its registration fee.");
                }
                return new RegistrationPayment(institutionId, paidOn);
            case "installation":
                if (paymentDAO.countByType(institutionId, "installation") > 0) {
                    throw new ServiceException("This institution has already paid its installation fee.");
                }
                return new InstallationPayment(institutionId, paidOn);
            case "monthly":
                return buildMonthly(institutionId, paidOn, billingMonth);
            default:
                throw new ServiceException("Choose a payment type from the list.");
        }
    }

    private Payment buildMonthly(int institutionId, LocalDate paidOn, YearMonth billingMonth)
            throws ServiceException, SQLException {
        if (billingMonth == null) {
            throw new ServiceException("Enter the billing month as yyyy-MM, for example 2025-05.");
        }
        if (billingMonth.isAfter(YearMonth.now())) {
            throw new ServiceException("The billing month cannot fall in the future.");
        }
        PaymentDAO.BillBasis basis = paymentDAO.findBillBasis(institutionId, billingMonth);
        if (basis == null) {
            throw new ServiceException("This institution has no subscription yet, so it owes no monthly bill.");
        }
        YearMonth firstMonth = YearMonth.from(basis.subscriptionStart);
        if (billingMonth.isBefore(firstMonth)) {
            throw new ServiceException("The subscription started in " + firstMonth
                    + ", after the billing month you entered.");
        }
        if (paymentDAO.monthlyPaid(institutionId, billingMonth)) {
            throw new ServiceException("The " + billingMonth + " bill for this institution is already paid.");
        }
        // A disconnected institution may still clear an old bill, so the status gets no check here.
        return new MonthlyPayment(institutionId, paidOn, billingMonth, basis.planCost, basis.discountRate);
    }

    public List<InstitutionOption> ListInstitutions() throws ServiceException {
        try {
            return institutionDAO.listOptions();
        } catch (SQLException e) {
            throw new ServiceException("Could not load institutions. Check the Database Connection.");
        }
    }
}