package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import model.ReportTable;
import model.ReportType;
import service.ReportService;
import service.ServiceException;

// Task 5: pick a report, see it in a JTable, then export it to CSV or print it.
// Event sources: reportBox, showButton, exportButton, printButton.
// Every one of them fires an ActionEvent, so one ActionListener (this form) catches all four.
public class ReportsForm extends JFrame implements ActionListener {

    private final ReportService reportService = new ReportService();

    private final JComboBox<ReportType> reportBox = new JComboBox<>(ReportType.values()); // event source 1
    private final JButton showButton = new JButton("Show");                              // event source 2
    private final JButton exportButton = new JButton("Export CSV");                      // event source 3
    private final JButton printButton = new JButton("Print");                            // event source 4
    private final JTable table = new JTable();
    private final JLabel statusLabel = new JLabel(" ");

    // The report on screen now, kept so Export and Print use exactly what the user sees.
    private ReportTable currentReport;
    private ReportType currentType;

    public ReportsForm() {
        super("Azani - Reports");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Report:"));
        top.add(reportBox);
        top.add(showButton);
        top.add(exportButton);
        top.add(printButton);

        // One renderer draws every cell: money with two decimals, numbers on the right, TOTAL in bold.
        table.setDefaultRenderer(Object.class, new ReportCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);   // wide reports scroll sideways instead of squashing
        table.setFillsViewportHeight(true);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);  // the scroll pane also shows the column headings
        add(statusLabel, BorderLayout.SOUTH);

        // Registration: this form listens to all four sources.
        reportBox.addActionListener(this);
        showButton.addActionListener(this);
        exportButton.addActionListener(this);
        printButton.addActionListener(this);

        exportButton.setEnabled(false);                    // nothing to export or print yet
        printButton.setEnabled(false);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 560);
        setLocationRelativeTo(null);

        showReport();                                      // open on the first report, 3(a)
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == reportBox || source == showButton) {
            showReport();                                  // Show re-reads the database: a refresh
        } else if (source == exportButton) {
            exportReport();
        } else if (source == printButton) {
            printReport();
        }
    }

    private void showReport() {
        ReportType type = (ReportType) reportBox.getSelectedItem();
        try {
            ReportTable report = reportService.load(type);
            // Anonymous inner class: a one-off subclass of DefaultTableModel that makes every cell read-only.
            DefaultTableModel model = new DefaultTableModel(report.rowArray(), report.columnArray()) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table.setModel(model);
            fitColumns();
            currentReport = report;
            currentType = type;
            exportButton.setEnabled(true);
            printButton.setEnabled(true);
            showInfo(report.getTitle() + ": " + report.getRowCount() + " row(s).");
        } catch (ServiceException ex) {
            table.setModel(new DefaultTableModel());       // never leave an old report under a new title
            currentReport = null;
            currentType = null;
            exportButton.setEnabled(false);
            printButton.setEnabled(false);
            showError(ex.getMessage());
        }
    }

    private void exportReport() {
        if (currentReport == null) {
            showError("Show a report before exporting it.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentType.getFileName() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            showInfo("Export cancelled.");
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        if (!file.getFileName().toString().toLowerCase().endsWith(".csv")) {
            file = file.resolveSibling(file.getFileName() + ".csv");
        }
        try {
            reportService.exportCsv(currentReport, file);
            showInfo("Saved " + file.getFileName() + " in " + file.getParent());
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void printReport() {
        if (currentReport == null) {
            showError("Show a report before printing it.");
            return;
        }
        try {
            // MessageFormat treats ' as special, so each ' is doubled; {0} becomes the page number.
            MessageFormat header = new MessageFormat(currentReport.getTitle().replace("'", "''"));
            MessageFormat footer = new MessageFormat("Azani ISP - page {0}");
            // FIT_WIDTH shrinks a wide table onto the page; print() opens the system print dialog.
            boolean printed = table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
            showInfo(printed ? "Sent " + currentReport.getTitle() + " to the printer." : "Printing cancelled.");
        } catch (PrinterException ex) {
            showError("Could not print: " + ex.getMessage());
        }
    }

    // Sets each column wide enough for its heading and its widest cell, up to 400 pixels.
    private void fitColumns() {
        for (int c = 0; c < table.getColumnCount(); c++) {
            TableColumn column = table.getColumnModel().getColumn(c);
            Component heading = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, c);
            int width = heading.getPreferredSize().width;
            for (int r = 0; r < table.getRowCount(); r++) {
                Component cell = table.prepareRenderer(table.getCellRenderer(r, c), r, c);
                width = Math.max(width, cell.getPreferredSize().width);
            }
            column.setPreferredWidth(Math.min(width + 16, 400));
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

    // Static nested class: draws one cell. Swing reuses one renderer for every cell,
    // so each call must set every property it changes (alignment, font) from scratch.
    private static class ReportCellRenderer extends DefaultTableCellRenderer {

        private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Object shown = value;
            if (value == null) {
                shown = "-";
            } else if (value instanceof BigDecimal) {
                shown = MONEY.format(value);              // 820000.00 shows as 820,000.00
            }
            super.getTableCellRendererComponent(table, shown, isSelected, hasFocus, row, column);
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);

            // withTotalRow puts the word TOTAL in column 0 (4(d)) or column 1 (the others).
            boolean totalRow = "TOTAL".equals(table.getValueAt(row, 0))
                    || (table.getColumnCount() > 1 && "TOTAL".equals(table.getValueAt(row, 1)));
            setFont(totalRow ? table.getFont().deriveFont(Font.BOLD) : table.getFont());
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReportsForm().setVisible(true));
    }
}