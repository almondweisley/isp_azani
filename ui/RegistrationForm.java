package ui;

import service.RegistrationService;
import service.ServiceException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegistrationForm extends JFrame {
    private final JTextField nameField = new JTextField(25);
    private final JComboBox<String> typeBox = new JComboBox<> (new String[] {"primary", "junior", "senior", "college"});
    private final JTextField addressField = new JTextField(25);
    private final JTextField contactField = new JTextField(25);
    private final JTextField phoneField = new JTextField(25);
    private final JTextField emailField = new JTextField(25);
    
    private final JButton saveButton = new JButton("Save");
    private final JButton clearButton = new JButton("clear");
    private final JLabel statusLabel = new JLabel(" ");

    private final RegistrationService service = new RegistrationService();


    public RegistrationForm() {
        super("Azani: Register Institution");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel fields = new JPanel(new GridLayout(0,2,8,8));
        fields.add(new JLabel("Institution Name *")); fields.add(nameField);
        fields.add(new JLabel("Type *")); fields.add(typeBox);
        fields.add(new JLabel("Address")); fields.add(addressField);
        fields.add(new JLabel("Contact Person *")); fields.add(contactField);
        fields.add(new JLabel("Phone")); fields.add(phoneField);
        fields.add(new JLabel("Email")); fields.add(emailField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(clearButton);
        buttons.add(saveButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.WEST);
        south.add(buttons, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(BorderFactory.createEmptyBorder(12,12,12, 12));
        root.add(fields, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
         // Register listeners with their event sources
        saveButton.addActionListener(new SaveListener());       // named inner class
        clearButton.addActionListener(e -> {                     // lambda: same interface, shorter syntax
            clearFields();
            showStatus(" ", false);
        });
        getRootPane().setDefaultButton(saveButton);              // Enter key clicks Save

        pack();
        setLocationRelativeTo(null);                             // centre on screen
    }

    // Catches the ActionEvent that saveButton fires on each click
    private class SaveListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int id = service.register(
                        nameField.getText(),
                        (String) typeBox.getSelectedItem(),
                        addressField.getText(),
                        contactField.getText(),
                        phoneField.getText(),
                        emailField.getText());
                clearFields();
                showStatus("Saved. Institution id " + id + ".", false);
            } catch (ServiceException ex) {
                // The user sees the plain message; the stack trace stays out of sight
                showStatus(ex.getMessage(), true);
            }
        }
    }

    private void clearFields() {
        nameField.setText("");
        typeBox.setSelectedIndex(0);
        addressField.setText("");
        contactField.setText("");
        phoneField.setText("");
        emailField.setText("");
        nameField.requestFocusInWindow();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setForeground(isError ? new Color(180, 0, 0) : new Color(0, 128, 0));
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegistrationForm().setVisible(true));
    
    }
}