package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

// Step 10: the one window the user starts. It opens every other screen.
// Run this class for the demonstration; each form's own main() stays as a test shortcut.
public class MainFrame extends JFrame {

    // The six screens, in the order the business works: register, visit, pay, subscribe, bill, report.
    // The dashboard numbers them from this order (ordinal() + 1), so reordering the list renumbers them.
    // Supplier<JFrame> holds "how to build the window"; RegistrationForm::new is a constructor
    // reference, so no window is built until the user asks for it.
    private enum Screen {
        REGISTRATION("Onboarding", "Register institution", "Institution and contact person (task 1)", RegistrationForm::new),
        SITE_VISIT("Onboarding", "Site visit and equipment", "Readiness, computers and LAN nodes", SiteVisitForm::new),
        PAYMENT("Money", "Capture payment", "Registration, installation, monthly (task 2)", PaymentForm::new),
        SUBSCRIPTION("Onboarding", "Subscription and upgrade", "Bandwidth plan, 10% upgrade discount", SubscriptionForm::new),
        BILLING("Money", "Billing cycle", "Fines, disconnection, reconnection", BillingForm::new),
        REPORTS("Reports", "Reports", "Tasks 3, 4 and 5: view, export, print", ReportsForm::new);

        final String menu;
        final String title;
        final String hint;
        final Supplier<JFrame> factory;

        Screen(String menu, String title, String hint, Supplier<JFrame> factory) {
            this.menu = menu;
            this.title = title;
            this.hint = hint;
            this.factory = factory;
        }
    }

    // One window per screen: a second click brings the open window forward instead of a copy.
    private final Map<Screen, JFrame> openWindows = new EnumMap<>(Screen.class);
    private final JLabel statusLabel = new JLabel(" ");

    public MainFrame() {
        super("Azani Internet Service Provider Information System");

        setJMenuBar(buildMenuBar());

        JLabel heading = new JLabel("Azani ISP", SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subheading = new JLabel("Internet services for primary, junior and senior schools and colleges",
                SwingConstants.CENTER);
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        header.add(heading);
        header.add(subheading);

        // The dashboard: one button per screen, same order as the menu.
        JPanel dashboard = new JPanel(new GridLayout(0, 2, 12, 12));
        dashboard.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        for (Screen screen : Screen.values()) {
            // HTML in a button label gives a bold title and a smaller second line.
            int step = screen.ordinal() + 1;                                  // position in the enum, from 0
            JButton button = new JButton("<html><b>" + step + ". " + screen.title + "</b><br><small>"
                    + screen.hint + "</small></html>");                       // event source: JButton
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(e -> open(screen));                       // lambda as ActionListener
            dashboard.add(button);
        }

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 16, 10, 16));
        showInfo("Ready. Today is " + LocalDate.now() + ".");

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(dashboard, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Closing the main window must ask first, so Swing must not close it on its own.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        // WindowListener has seven methods; WindowAdapter supplies empty ones,
        // so this anonymous subclass overrides only the one it needs.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {                         // @Override catches a misspelling
                confirmExit();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);                                       // Alt+F opens it
        JMenuItem exit = new JMenuItem("Exit");                                // event source: JMenuItem
        exit.addActionListener(e -> confirmExit());
        file.add(exit);
        bar.add(file);

        // One menu per group, created the first time a screen names it.
        Map<String, JMenu> menus = new java.util.LinkedHashMap<>();
        for (Screen screen : Screen.values()) {
            JMenu menu = menus.computeIfAbsent(screen.menu, JMenu::new);
            JMenuItem item = new JMenuItem(screen.title);                      // event source: JMenuItem
            item.setToolTipText(screen.hint);
            item.addActionListener(e -> open(screen));
            menu.add(item);
        }
        for (JMenu menu : menus.values()) {
            bar.add(menu);
        }

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Azani ISP Information System\nSCO 200 Object Oriented Programming II\n"
                        + "Kenyatta University, September to November 2026",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(help);
        return bar;
    }

    // Opens a screen, or brings it to the front if it is already open.
    private void open(Screen screen) {
        JFrame window = openWindows.get(screen);
        if (window != null && window.isDisplayable()) {                        // false once disposed
            window.setState(NORMAL);                                           // un-minimise
            window.toFront();
            showInfo(screen.title + " is already open.");
            return;
        }
        try {
            window = screen.factory.get();                                     // calls the constructor now
        } catch (RuntimeException ex) {
            showError("Could not open " + screen.title + ". Check the database connection.");
            return;
        }
        // A form may set EXIT_ON_CLOSE for its own test run; inside the app,
        // closing one form must close only that form.
        window.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        openWindows.put(screen, window);
        window.setVisible(true);
        showInfo("Opened " + screen.title + ".");
    }

    private void confirmExit() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Close Azani and every open window?", "Exit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            for (JFrame window : openWindows.values()) {
                window.dispose();
            }
            dispose();
            System.exit(0);                                                    // ends the program
        }
    }

    private void showInfo(String message) {
        statusLabel.setForeground(new Color(0, 110, 0));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}