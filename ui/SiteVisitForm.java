package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import model.InstitutionOption;
import model.ReadinessAssessment;
import service.ServiceException;
import service.SiteService;

public class SiteVisitForm extends JFrame implements ActionListener {

    private final SiteService siteService = new SiteService();

    private final JComboBox<InstitutionOption> institutionBox = new JComboBox<>();
    private final JTextField visitDateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField userCountField = new JTextField(10);
    private final JCheckBox computersBox = new JCheckBox("Has enough computers");   // event source 1
    private final JCheckBox lanBox = new JCheckBox("Has a working LAN");            // event source 2
    private final JTextField computerQtyField = new JTextField("0", 10);
    private final JTextField lanNodesField = new JTextField("0", 10);
    private final JButton saveButton = new JButton("Save site visit");             // event source 3
    private final JLabel statusLabel = new JLabel(" ");

    public SiteVisitForm() {
        super("Azani - Site Visit");

        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        fields.add(new JLabel("Institution:"));
        fields.add(institutionBox);
        fields.add(new JLabel("Visit date (YYYY-MM-DD):"));
        fields.add(visitDateField);
        fields.add(new JLabel("Number of users:"));
        fields.add(userCountField);
        fields.add(computersBox);
        fields.add(lanBox);
        fields.add(new JLabel("Computers to buy:"));
        fields.add(computerQtyField);
        fields.add(new JLabel("LAN nodes to buy (2 to 100):"));
        fields.add(lanNodesField);
        fields.add(new JLabel(""));
        fields.add(saveButton);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        setLayout(new BorderLayout());
        add(fields, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // A JCheckBox fires an ActionEvent on every click, so the same listener serves it.
        computersBox.addActionListener(this);
        lanBox.addActionListener(this);
        saveButton.addActionListener(this);

        loadInstitutions();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void loadInstitutions() {
        try {
            for (InstitutionOption option : siteService.listInstitutions()) {
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
        if (source == computersBox || source == lanBox) {
            updatePurchaseFields();
        } else if (source == saveButton) {
            saveVisit();
        }
    }

    // An institution buys only what it lacks.
    private void updatePurchaseFields() {
        computerQtyField.setEnabled(!computersBox.isSelected());
        if (computersBox.isSelected()) computerQtyField.setText("0");
        lanNodesField.setEnabled(!lanBox.isSelected());
        if (lanBox.isSelected()) lanNodesField.setText("0");
    }

    private void saveVisit() {
        InstitutionOption chosen = (InstitutionOption) institutionBox.getSelectedItem();
        if (chosen == null) {
            showError("Choose an institution first.");
            return;
        }

        LocalDate visitDate;
        int users;
        int qty;
        int nodes;
        try {
            visitDate = LocalDate.parse(visitDateField.getText().trim());
            users = parseCount(userCountField.getText());
            qty = parseCount(computerQtyField.getText());
            nodes = parseCount(lanNodesField.getText());
        } catch (DateTimeParseException ex) {
            showError("Enter the visit date as YYYY-MM-DD, for example 2026-10-09.");
            return;
        } catch (NumberFormatException ex) {
            showError("Users, computers and nodes must be whole numbers.");
            return;
        }

        try {
            ReadinessAssessment result = siteService.recordVisit(chosen.getId(), visitDate, users,
                    computersBox.isSelected(), lanBox.isSelected(), qty, nodes);
            statusLabel.setForeground(new Color(0, 110, 0));
            statusLabel.setText(chosen + ": " + result.summary());
        } catch (ServiceException ex) {
            statusLabel.setForeground(Color.RED);
            // The user sees a readable reason instead of a guess about the connection.
            statusLabel.setText("Could not save the site visit: " + rootMessage(ex));
            ex.printStackTrace();   // console only, for you while debugging; delete before submission
        }
    }

    // A blank box counts as zero; anything else must be a whole number.
    private int parseCount(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : Integer.parseInt(trimmed);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SiteVisitForm().setVisible(true));
    }

    // Walks down the exception chain and returns the deepest message,
// which is usually MySQL's own explanation of the failure.
    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) {   // each getCause() steps one level down the chain
            t = t.getCause();
        }
        return t.getMessage();
    }
}