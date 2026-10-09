package ui;

import model.BandwidthPlan;
import model.InstitutionOption;
import service.PaymentService;
import service.ServiceException;
import service.SubscriptionService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class SubscriptionForm extends JFrame {

    // The form holds services only. It never creates a DAO.
    private final SubscriptionService subscriptionService = new SubscriptionService();
    private final PaymentService paymentService = new PaymentService();

    private final JComboBox<InstitutionOption> institutionBox = new JComboBox<>();
    private final JComboBox<BandwidthPlan> planBox = new JComboBox<>();
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField monthField = new JTextField(YearMonth.now().toString(), 7);

    // Each JButton is an event source: a click fires an ActionEvent.
    private final JButton subscribeButton = new JButton("Subscribe");
    private final JButton upgradeButton = new JButton("Upgrade");
    private final JButton chargeButton = new JButton("Monthly charge");

    // Every message to the clerk goes here, never to the console.
    private final JLabel statusLabel = new JLabel(" ");

    public SubscriptionForm() {
        super("Azani - Subscription and upgrade");
        buildLayout();
        loadLists();

        // Registration: the button keeps a reference to the listener object.
        subscribeButton.addActionListener(new SubscribeHandler());  // named inner class
        upgradeButton.addActionListener(e -> upgrade());             // lambda implements ActionListener
        chargeButton.addActionListener(e -> showCharge());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);  // closes this window, leaves MainFrame running later
        pack();
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 6));  // 0 rows means "as many as needed"
        fields.add(new JLabel("Institution"));
        fields.add(institutionBox);
        fields.add(new JLabel("Bandwidth plan"));
        fields.add(planBox);
        fields.add(new JLabel("Start or upgrade date (yyyy-mm-dd)"));
        fields.add(dateField);
        fields.add(new JLabel("Billing month (yyyy-mm)"));
        fields.add(monthField);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(subscribeButton);
        buttons.add(upgradeButton);
        buttons.add(chargeButton);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(fields, BorderLayout.NORTH);
        root.add(buttons, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);

    }

    private void loadLists() {
        try {
            for (InstitutionOption option : paymentService.listInstitutions()) {
                institutionBox.addItem(option);
            }
            for (BandwidthPlan plan : subscriptionService.listPlans()) {
                planBox.addItem(plan);  // prices come from bandwidth_plans, never from the form
            }
        } catch (ServiceException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    // A named inner class makes the interface visible: it implements ActionListener.
    private class SubscribeHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            InstitutionOption inst = (InstitutionOption) institutionBox.getSelectedItem();
            BandwidthPlan plan = (BandwidthPlan) planBox.getSelectedItem();
            if (inst == null || plan == null) {
                statusLabel.setText("Choose an institution and a plan.");
                return;
            }
            try {
                LocalDate start = LocalDate.parse(dateField.getText().trim());
                // The service checks the fees, the ready visit and the one-subscription rule.
                subscriptionService.subscribe(inst.getId(), plan.getPlanId(), start);
                statusLabel.setText("Subscribed " + inst + " to " + plan.getMbps()
                        + " Mbps from " + start + ".");
            } catch (DateTimeParseException ex) {
                statusLabel.setText("Enter the date as yyyy-mm-dd.");
            } catch (ServiceException ex) {
                statusLabel.setText(ex.getMessage());
            }
        }
    }

    private void upgrade() {
        InstitutionOption inst = (InstitutionOption) institutionBox.getSelectedItem();
        BandwidthPlan plan = (BandwidthPlan) planBox.getSelectedItem();
        if (inst == null || plan == null) {
            statusLabel.setText("Choose an institution and the new plan.");
            return;
        }
        try {
            LocalDate when = LocalDate.parse(dateField.getText().trim());
            // The service refuses a move to a lower or equal Mbps; the form does not repeat that rule.
            subscriptionService.upgrade(inst.getId(), plan.getPlanId(), when);
            YearMonth month = YearMonth.from(when);
            BigDecimal charge = subscriptionService.monthlyCharge(inst.getId(), month);
            statusLabel.setText("Upgraded " + inst + " to " + plan.getMbps() + " Mbps. Charge for "
                    + month + ": KSh " + charge.toPlainString());
        } catch (DateTimeParseException ex) {
            statusLabel.setText("Enter the date as yyyy-mm-dd.");
        } catch (ServiceException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void showCharge() {
        InstitutionOption inst = (InstitutionOption) institutionBox.getSelectedItem();
        if (inst == null) {
            statusLabel.setText("Choose an institution.");
            return;
        }
        try {
            YearMonth month = YearMonth.parse(monthField.getText().trim());
            BigDecimal charge = subscriptionService.monthlyCharge(inst.getId(), month);
            statusLabel.setText("Charge for " + inst + " in " + month + ": KSh " + charge.toPlainString());
        } catch (DateTimeParseException ex) {
            statusLabel.setText("Enter the month as yyyy-mm.");
        } catch (ServiceException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        // Swing builds and changes components on one thread, the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new SubscriptionForm().setVisible(true));
    }
}