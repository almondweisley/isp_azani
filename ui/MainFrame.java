package ui;

import service.DashboardService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The one window of the application, laid out:
 * a teal stripe and grey banner along the top, a white logo block,
 * a navy menu on the left, a white page header, and a content area
 * that swaps pages with a CardLayout. Run this class to start the app.
 */
public class MainFrame extends JFrame {

    // Page keys: one String per screen. Other classes use them to ask for a page.
    public static final String DASHBOARD    = "dashboard";
    public static final String REGISTER     = "register";
    public static final String PAYMENTS     = "payments";
    public static final String SITE         = "site";
    public static final String SUBSCRIPTION = "subscription";
    public static final String BILLING      = "billing";
    public static final String REPORTS      = "reports";

    /** What MainFrame knows about each page: its labels and how to build it. */
    private static final class Page {
        final String navText, title, subtitle;
        final Supplier<Container> factory;          // null for the dashboard, which is built once
        Page(String navText, String title, String subtitle, Supplier<Container> factory) {
            this.navText = navText; this.title = title; this.subtitle = subtitle; this.factory = factory;
        }
    }

    // LinkedHashMap keeps insertion order, so the sidebar lists pages in this order.
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Component> builtPages = new LinkedHashMap<>();

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel pageTitle = new JLabel();
    private final JLabel pageSubtitle = new JLabel();
    private final DashboardPanel dashboard;

    public MainFrame() {
        this(new DashboardService());               // normal start: the real service
    }

    /** Takes the service from outside (dependency injection), so a test can pass a fake one. */
    public MainFrame(DashboardService dashboardService) {
        super("Azani ISP Information System");

        // Supplier<Container> + constructor reference: "here is HOW to make the form", not the form itself.
        // If one of your classes has a different name, change it on its line below.
        pages.put(DASHBOARD,    new Page("Dashboard", "Dashboard",
                "Overview of institutions, subscriptions and this month's collections", null));
        pages.put(REGISTER,     new Page("Register institution", "Register institution",
                "Task 1: an institution and its contact person", RegistrationForm::new));
        pages.put(PAYMENTS,     new Page("Payments", "Capture payment",
                "Task 2: registration, installation and monthly payments", PaymentForm::new));
        pages.put(SITE,         new Page("Site visits", "Site visit and equipment",
                "Readiness assessment, computers and LAN nodes", SiteVisitForm::new));
        pages.put(SUBSCRIPTION, new Page("Subscriptions", "Subscriptions and upgrades",
                "Bandwidth plans and the upgrade discount", SubscriptionForm::new));
        pages.put(BILLING,      new Page("Billing", "Billing cycle",
                "Overdue fines, disconnections and reconnection", BillingForm::new));
        pages.put(REPORTS,      new Page("Reports", "Reports",
                "Tasks 3 to 5: lists, computations and printable reports", ReportsForm::new));

        dashboard = new DashboardPanel(dashboardService, this::showPage);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.PAGE_GREY);
        main.add(buildHeader(), BorderLayout.NORTH);
        content.setBackground(Theme.PAGE_GREY);
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        main.add(content, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(main, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildBanner(), BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1280, 800);                      // the size it returns to if the user restores it
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // fill the whole screen, taskbar still visible

        showPage(DASHBOARD);
    }

    // ------------------------------------------------------------------ layout pieces

    /** top strip: a 4-pixel teal line, then the grey "official website" banner. */
    private JPanel buildBanner() {
        JLabel left = new JLabel("Azani Internet Service Provider  ·  Information System");
        left.setFont(Theme.BANNER);
        left.setForeground(Theme.LOGO_GREY);

        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        date.setFont(Theme.BANNER);
        date.setForeground(Theme.LOGO_GREY);

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Theme.BANNER_GREY);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, Theme.STRIPE_TEAL),   // the teal stripe
                BorderFactory.createEmptyBorder(6, 24, 6, 32)));
        banner.add(left, BorderLayout.WEST);
        banner.add(date, BorderLayout.EAST);
        return banner;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.NAV_NAVY);
        side.setPreferredSize(new Dimension(260, 0));

        // Logo block, styled  grey word, then a bold green word.
        JLabel word1 = new JLabel("Azani ");
        word1.setFont(Theme.LOGO);
        word1.setForeground(Theme.LOGO_GREY);
        JLabel word2 = new JLabel("ISP");
        word2.setFont(Theme.LOGO_BOLD);
        word2.setForeground(Theme.AID_GREEN);
        JPanel logoLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoLine.setOpaque(false);
        logoLine.add(word1);
        logoLine.add(word2);

        JLabel tagline = new JLabel("INTERNET SERVICE PROVIDER");
        tagline.setFont(Theme.CAPTION.deriveFont(9.5f));
        tagline.setForeground(Theme.LOGO_GREY);

        JPanel brand = new JPanel(new BorderLayout(0, 2));
        brand.setBackground(Theme.WHITE);
        brand.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(18, 24, 18, 16)));
        brand.add(logoLine, BorderLayout.CENTER);
        brand.add(tagline, BorderLayout.SOUTH);
        side.add(brand, BorderLayout.NORTH);

        // One NavButton per page, stacked.
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        JLabel menu = new JLabel("MENU");
        menu.setFont(Theme.CAPTION);
        menu.setForeground(Theme.BORDER);
        menu.setBorder(BorderFactory.createEmptyBorder(0, 24, 10, 0));
        nav.add(menu);
        for (Map.Entry<String, Page> entry : pages.entrySet()) {
            String key = entry.getKey();
            NavButton b = new NavButton(entry.getValue().navText);
            // Event source: the NavButton. Listener interface: ActionListener.
            b.addActionListener(e -> showPage(key));
            navButtons.put(key, b);
            nav.add(b);
        }
        side.add(nav, BorderLayout.CENTER);

        JButton exit = new NavButton("Exit");
        exit.addActionListener(e -> confirmExit());
        JLabel footer = new JLabel("SCO 200  ·  Kenyatta University");
        footer.setFont(Theme.SMALL);
        footer.setForeground(Theme.BORDER);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 24, 18, 0));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(exit, BorderLayout.NORTH);
        bottom.add(footer, BorderLayout.SOUTH);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildHeader() {
        pageTitle.setFont(Theme.TITLE);
        pageTitle.setForeground(Theme.HEADING);
        pageSubtitle.setFont(Theme.BODY);
        pageSubtitle.setForeground(Theme.TEXT);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(pageTitle);
        titles.add(pageSubtitle);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),   
                BorderFactory.createEmptyBorder(20, 32, 20, 32)));
        header.add(titles, BorderLayout.WEST);
        return header;
    }

    // ------------------------------------------------------------------ navigation

    /** Shows the page with this key. Forms are rebuilt each visit so their dropdowns are current. */
    public void showPage(String key) {
        Page page = pages.get(key);
        if (page == null) return;

        Component old = builtPages.remove(key);
        if (old != null) content.remove(old);

        Component view;
        if (page.factory == null) {
            view = dashboard;
            dashboard.refresh();
        } else {
            view = wrapInCard(contentOf(page.factory.get()));
        }
        builtPages.put(key, view);
        content.add(view, key);          // the key is the card's name inside the CardLayout
        cards.show(content, key);

        pageTitle.setText(page.title);
        pageSubtitle.setText(page.subtitle);
        navButtons.forEach((k, b) -> b.setActive(k.equals(key)));
        content.revalidate();
        content.repaint();
    }

    /**
     * Your forms are JFrames. This lifts the panel out of a frame so it can live inside MainFrame,
     * then throws the empty frame away. A form that is already a JPanel passes straight through.
     */
    private static JComponent contentOf(Container form) {
        if (form instanceof RootPaneContainer frame) {          // pattern matching for instanceof (Java 16+)
            Container body = frame.getContentPane();
            frame.setContentPane(new JPanel());                 // detach the body from the frame
            if (form instanceof Window w) w.dispose();          // free the frame's native resources
            return (JComponent) body;
        }
        return (JComponent) form;
    }

    /** Puts a form on a white card. Forms with a table fill the page; short forms keep their size. */
    private static JComponent wrapInCard(JComponent body) {
        applyTheme(body);
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.WHITE);
        card.setBorder(Theme.cardBorder());
        card.add(body, BorderLayout.CENTER);

        if (containsTable(body)) {
            return card;                                        // BorderLayout.CENTER of content: fills
        }
        Dimension pref = card.getPreferredSize();
        card.setPreferredSize(new Dimension(Math.max(pref.width, 640), pref.height));

        JPanel holder = new JPanel(new GridBagLayout());        // keeps the card at its preferred size
        holder.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1;
        g.weighty = 1;
        holder.add(card, g);
        return holder;
    }

    /**
     * Walks the component tree of a form and gives every part the LOOK
     * white panels,label font and colour, blue buttons, styled tables.
     */
    private static void applyTheme(Component c) {
        if (c instanceof JPanel || c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setBackground(Theme.WHITE);
        }
        if (c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setForeground(Theme.TEXT);
        }
        if (c instanceof JLabel label) {
            label.setFont(Theme.BODY);
            label.setForeground(Theme.TEXT);   // a status label recolours itself after Save, from Theme
        }
        if (c instanceof JButton button) {
            Theme.styleButton(button);
        }
        if (c instanceof JTable table) {
            Theme.styleTable(table);
        }
        if (c instanceof JViewport viewport) {
            viewport.setBackground(Theme.WHITE);
        }
        if (c instanceof Container parent && !(c instanceof JTable) && !(c instanceof JComboBox)) {
            for (Component child : parent.getComponents()) applyTheme(child);
        }
    }

    private static boolean containsTable(Component c) {
        if (c instanceof JTable) return true;
        if (c instanceof Container parent) {
            for (Component child : parent.getComponents()) {
                if (containsTable(child)) return true;
            }
        }
        return false;
    }

    private void confirmExit() {
        // PLAIN_MESSAGE: no built-in icon, keeping the dialog visually neutral.
        int answer = JOptionPane.showConfirmDialog(this, "Close the Azani ISP system?",
                "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) System.exit(0);
    }

    // ------------------------------------------------------------------ sidebar button

    /**
     * A flat sidebar button. It paints its own background, so the colours are exact.
     * Hover uses a MouseAdapter: see the listener-versus-adapter note in the lesson.
     */
    private static class NavButton extends JButton {
        private boolean active;
        private boolean hover;

        NavButton(String text) {
            super(text);
            setFont(Theme.NAV);
            setForeground(Theme.BANNER_GREY);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(13, 24, 13, 16));
            setContentAreaFilled(false);     // stop the look and feel painting its own button shape
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            setAlignmentX(LEFT_ALIGNMENT);

            // MouseAdapter is a CLASS with empty bodies for all five MouseListener methods.
            // We extend it and override only the two we need.
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Theme.WHITE : Theme.BANNER_GREY);
            setFont(active ? Theme.NAV_ACTIVE : Theme.NAV);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color fill = active ? Theme.BUTTON_BLUE : hover ? Theme.LINK_BLUE : Theme.NAV_NAVY;
            g.setColor(fill);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (active) {
                g.setColor(Theme.AID_GREEN);
                g.fillRect(0, 0, 5, getHeight());          // "Aid" green marker on the selected page
            }
            super.paintComponent(g);                       // then draw the text on top
        }
    }

    // ------------------------------------------------------------------ start-up

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();                // must run BEFORE any component is created
            new MainFrame().setVisible(true);
        });
    }
}