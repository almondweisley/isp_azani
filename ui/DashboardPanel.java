package ui;

import model.DashboardStats;
import model.DashboardStats.RecentPayment;
import service.DashboardService;
import service.ServiceException;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * The home screen: four KPI cards, a recent-payments table and quick actions.
 * It is a JPanel, not a JFrame, so MainFrame can place it inside its own window.
 */
public class DashboardPanel extends JPanel {

    private final DashboardService service;
    private final Consumer<String> navigator;      // MainFrame's showPage method, handed in

    private final KpiCard institutionsCard  = new KpiCard("Registered institutions", Theme.BUTTON_BLUE);
    private final KpiCard subscriptionsCard = new KpiCard("Active subscriptions", Theme.AID_GREEN);
    private final KpiCard collectedCard     = new KpiCard("Collected this month", Theme.NAV_NAVY);
    private final KpiCard disconnectedCard  = new KpiCard("Open disconnections", Theme.STRIPE_TEAL);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Date", "Institution", "Type", "Amount (KSh)"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }  // read-only table
    };
    private final JButton refreshButton = new JButton("Refresh");   // event source: refresh
    private final JLabel statusLabel = new JLabel(" ");

    public DashboardPanel(DashboardService service, Consumer<String> navigator) {
        super(new BorderLayout(0, 24));
        this.service = service;
        this.navigator = navigator;
        setOpaque(false);                                  // let the grey page background show

        // ---- Row 1: KPI cards, four equal columns ----
        JPanel kpis = new JPanel(new GridLayout(1, 4, 24, 0));
        kpis.setOpaque(false);
        kpis.add(institutionsCard);
        kpis.add(subscriptionsCard);
        kpis.add(collectedCard);
        kpis.add(disconnectedCard);
        add(kpis, BorderLayout.NORTH);

        // ---- Row 2: table on the left, quick actions on the right ----
        JPanel row2 = new JPanel(new BorderLayout(24, 0));
        row2.setOpaque(false);
        row2.add(buildRecentPaymentsCard(), BorderLayout.CENTER);
        row2.add(buildQuickActionsCard(), BorderLayout.EAST);
        add(row2, BorderLayout.CENTER);

        statusLabel.setFont(Theme.SMALL);
        statusLabel.setForeground(Theme.LOGO_GREY);
        add(statusLabel, BorderLayout.SOUTH);

        // Event source: refreshButton. Listener interface: ActionListener (written as a lambda).
        Theme.styleButton(refreshButton);
        refreshButton.addActionListener(e -> refresh());
    }

    private JPanel buildRecentPaymentsCard() {
        JTable table = new JTable(tableModel);
        Theme.styleTable(table);                          // shared look, defined once in Theme

        // Money reads best right-aligned so the digits line up.
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(cardTitle("Recent payments"), BorderLayout.WEST);
        top.add(refreshButton, BorderLayout.EAST);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.WHITE);
        card.setBorder(Theme.cardBorder());
        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQuickActionsCard() {
       
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));   // stack children top to bottom
        card.setBackground(Theme.WHITE);
        card.setBorder(Theme.cardBorder());
        card.setPreferredSize(new Dimension(290, 0));

        JLabel title = cardTitle("Quick actions");
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(16));

        // Each button names a page key that MainFrame understands.
        card.add(actionButton("Register an institution", MainFrame.REGISTER));
        card.add(Box.createVerticalStrut(12));
        card.add(actionButton("Record a payment", MainFrame.PAYMENTS));
        card.add(Box.createVerticalStrut(12));
        card.add(actionButton("Record a site visit", MainFrame.SITE));
        card.add(Box.createVerticalStrut(12));
        card.add(actionButton("Open reports", MainFrame.REPORTS));
        card.add(Box.createVerticalGlue());                       // pushes everything to the top
        return card;
    }

    private static JLabel cardTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(Theme.HEADING_F);
        title.setForeground(Theme.HEADING);
        return title;
    }

    private JButton actionButton(String text, String pageKey) {
        JButton b = new JButton(text);
        Theme.styleButton(b);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));   // full card width, fixed height
        b.addActionListener(e -> navigator.accept(pageKey));      // ActionListener again, one per button
        return b;
    }

    /**
     * Loads the numbers on a background thread so the window never freezes
     * while the query travels to the Aiven server and back.
     */
    public void refresh() {
        refreshButton.setEnabled(false);
        statusLabel.setForeground(Theme.LOGO_GREY);
        statusLabel.setText("Loading…");

        new SwingWorker<DashboardStats, Void>() {
            @Override
            protected DashboardStats doInBackground() throws Exception {
                return service.load();           // background thread: no Swing calls allowed here
            }

            @Override
            protected void done() {              // back on the Event Dispatch Thread: Swing calls are safe
                try {
                    show(get());                 // get() hands over the result, or rethrows the failure
                    statusLabel.setForeground(Theme.LOGO_GREY);
                    statusLabel.setText("Last updated at "
                            + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();          // whatever doInBackground threw
                    statusLabel.setForeground(Theme.ERROR);
                    statusLabel.setText(cause instanceof ServiceException
                            ? cause.getMessage()
                            : "Could not load the dashboard: " + cause);  // shows the class name too
                    cause.printStackTrace();   // console only, for debugging; remove before submission
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void show(DashboardStats s) {
        institutionsCard.setValue(String.valueOf(s.getInstitutions()));
        subscriptionsCard.setValue(String.valueOf(s.getActiveSubscriptions()));
        collectedCard.setValue(money(s.getCollectedThisMonth()));
        disconnectedCard.setValue(String.valueOf(s.getOpenDisconnections()));

        tableModel.setRowCount(0);                            // clear old rows before adding new ones
        for (RecentPayment p : s.getRecentPayments()) {
            tableModel.addRow(new Object[] {
                    p.getPaidOn(), p.getInstitution(), capitalise(p.getType()),
                    String.format("%,.2f", p.getAmount())});
        }
    }

    private static String money(BigDecimal amount) {
        return "KSh " + String.format("%,.0f", amount);       // 12500.00 -> KSh 12,500
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** A white card with a coloured stripe on the left, a caption and a big number. */
    private static class KpiCard extends JPanel {
        private final JLabel value = new JLabel("—");

        KpiCard(String caption, Color accent) {
            super(new BorderLayout(0, 6));
            setBackground(Theme.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 5, 0, 0, accent),                        // the coloured stripe
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 0, 1, 1, Theme.BORDER),
                            BorderFactory.createEmptyBorder(18, 20, 18, 20))));

            JLabel label = new JLabel(caption.toUpperCase());
            label.setFont(Theme.CAPTION);
            label.setForeground(Theme.LOGO_GREY);

            value.setFont(Theme.KPI);
            value.setForeground(accent);

            add(label, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setValue(String text) { value.setText(text); }
    }
}