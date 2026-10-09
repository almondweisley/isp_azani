package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import model.InstitutionOption;
import model.Payment;
import service.PaymentService;
import service.ServiceException;

public class PaymentForm extends JFrame implements ActionListener {

    // Task 2 names three payment types. Reconnection belongs to BillingForm (step 7).
    private static final String[] TYPES = {"registration", "installation", "monthly"};

    private final PaymentService paymentService = new PaymentService();

    private final JComboBox<InstitutionOption> institutionBox = new JComboBox<>();
    private final JComboBox<String> typeBox = new JComboBox<>(TYPES);        // event source 1
    private final JTextField paidOnField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField monthField = new JTextField(10);
    private final JButton saveButton = new JButton("Record payment");        // event source 2
    private final JLabel statusLabel = new JLabel(" ");

    public PaymentForm() {
        super("Azani - Capture Payment");

        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        fields.add(new JLabel("Institution:"));
        fields.add(institutionBox);
        fields.add(new JLabel("Payment type:"));
        fields.add(typeBox);
        fields.add(new JLabel("Paid on (YYYY-MM-DD):"));
        fields.add(paidOnField);
        fields.add(new JLabel("Billing month (YYYY-MM):"));
        fields.add(monthField);
        fields.add(new JLabel(""));
        fields.add(saveButton);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        setLayout(new BorderLayout());
        add(fields, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Registration: each source gets this form as its listener.
        typeBox.addActionListener(this);
        saveButton.addActionListener(this);

        loadInstitutions();
        updateMonthField();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void loadInstitutions() {
        try {
            for (InstitutionOption option : paymentService.listInstitutions()) {
                institutionBox.addItem(option);
            }
        } catch (ServiceException ex) {
            showError(ex.getMessage());
            saveButton.setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == typeBox) {
            updateMonthField();
        } else if (source == saveButton) {
            savePayment();
        }
    }

    // Only a monthly payment carries a billing month.
    private void updateMonthField() {
        boolean monthly = "monthly".equals(typeBox.getSelectedItem());
        monthField.setEnabled(monthly);
        if (!monthly) {
            monthField.setText("");
        }
    }

    private void savePayment() {
        InstitutionOption chosen = (InstitutionOption) institutionBox.getSelectedItem();
        if (chosen == null) {
            showError("Choose an institution first.");
            return;
        }
        String type = (String) typeBox.getSelectedItem();

        // The form converts text into Java types; the service applies the rules.
        LocalDate paidOn;
        YearMonth billingMonth = null;               // stays null for registration and installation
        try {
            paidOn = LocalDate.parse(paidOnField.getText().trim());          // expects 2025-04-25
            if (monthField.isEnabled()) {
                billingMonth = YearMonth.parse(monthField.getText().trim()); // expects 2025-04
            }
        } catch (DateTimeParseException ex) {
            showError("Enter the date as YYYY-MM-DD and the month as YYYY-MM, for example 2025-04-25 and 2025-04.");
            return;
        }

        try {
            // record returns the saved Payment object; the form reads its amount for the label.
            Payment saved = paymentService.record(chosen.getId(), type, paidOn, billingMonth);
            statusLabel.setForeground(new Color(0, 110, 0));
            statusLabel.setText("Saved: " + type + " payment of KSh " + saved.amountDue()
                    + " for " + chosen);
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaymentForm().setVisible(true));
    }
}