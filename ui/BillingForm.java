package ui;

import model.Payment;
import service.BillingService;
import service.ServiceException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Runs billing for one institution, takes monthly payments, reconnects. Rules stay in BillingService. */
public class BillingForm extends JFrame {

    private final BillingService billingService = new BillingService();   // field: every method can see it

    private final JTextField institutionIdField = new JTextField(6);
    // Machine date as the default until you choose the business time zone (open decision)
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField billingMonthField = new JTextField(7);
    private final JButton runButton = new JButton("Run billing");             // event source 1
    private final JButton payButton = new JButton("Pay monthly bill");        // event source 2
    private final JButton reconnectButton = new JButton("Reconnect");         // event source 3
    private final JTextArea resultArea = new JTextArea(10, 50);
    private final JLabel statusLabel = new JLabel(" ");                       // all user messages go here

    public BillingForm() {
        super("Azani ISP - Billing cycle");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);   // closes this window only

        JPanel fields = new JPanel(new GridLayout(3, 2, 8, 8));
        fields.add(new JLabel("Institution ID:"));
        fields.add(institutionIdField);
        fields.add(new JLabel("Date (YYYY-MM-DD):"));          // run date, payment date or reconnection date
        fields.add(dateField);
        fields.add(new JLabel("Billing month (YYYY-MM):"));    // used only by Pay monthly bill
        fields.add(billingMonthField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(runButton);
        buttons.add(payButton);
        buttons.add(reconnectButton);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(fields, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        resultArea.setEditable(false);
        setLayout(new BorderLayout(8, 8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        runButton.addActionListener(new RunBillingListener());   // named inner class
        payButton.addActionListener(e -> payMonthly());          // lambdas: Java builds an ActionListener
        reconnectButton.addActionListener(e -> reconnect());

        pack();
        setLocationRelativeTo(null);
    }

    /** Catches clicks on runButton. ActionListener has one method, so there is no adapter for it. */
    private class RunBillingListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Integer institutionId = readInstitutionId();
            if (institutionId == null) return;
            LocalDate runDate = readDate();
            if (runDate == null) return;
            try {
                // The return type must match the service exactly: the nested BillingRun type
                BillingService.BillingRun run = billingService.runBillingFor(institutionId, runDate);
                resultArea.append("Institution " + institutionId + ", run to " + runDate + ": "
                        + run.finesAdded() + " fine(s), " + run.disconnections() + " disconnection(s)\n");
                statusLabel.setText("Billing run complete.");
            } catch (ServiceException ex) {
                statusLabel.setText(ex.getMessage());
            }
        }
    }

    private void payMonthly() {
        Integer institutionId = readInstitutionId();
        if (institutionId == null) return;
        LocalDate paidOn = readDate();
        if (paidOn == null) return;

        YearMonth month;
        try {
            month = YearMonth.parse(billingMonthField.getText().trim());   // strict YYYY-MM
        } catch (DateTimeParseException ex) {
            statusLabel.setText("Use the month form YYYY-MM, for example 2026-09.");
            return;
        }

        try {
            Payment paid = billingService.payMonthly(institutionId, month, paidOn);   // service adds any fine
            resultArea.append("Institution " + institutionId + ", " + month + " paid on " + paidOn
                    + ": KSh " + paid.amountDue() + "\n");
            statusLabel.setText("Payment saved.");
        } catch (ServiceException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void reconnect() {
        Integer institutionId = readInstitutionId();
        if (institutionId == null) return;
        LocalDate onDate = readDate();
        if (onDate == null) return;

        try {
            Payment fee = billingService.reconnect(institutionId, onDate);   // records the KSh 1,000 fee
            resultArea.append("Institution " + institutionId + " reconnected on " + onDate
                    + ", reconnection fee KSh " + fee.amountDue() + "\n");
            statusLabel.setText("Reconnection saved.");
        } catch (ServiceException ex) {
            statusLabel.setText(ex.getMessage());   // e.g. the bill or fine is still unpaid
        }
    }

    /** Returns the ID, or null after setting a status message. Integer (not int) so it can be null. */
    private Integer readInstitutionId() {
        try {
            return Integer.parseInt(institutionIdField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Institution ID must be a whole number, for example 33.");
            return null;
        }
    }

    /** Format check only. LocalDate has no clock time, so it cannot shift. */
    private LocalDate readDate() {
        try {
            return LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException ex) {
            statusLabel.setText("Use the date form YYYY-MM-DD, for example 2026-10-14.");
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BillingForm().setVisible(true));   // build Swing on the EDT
    }
}