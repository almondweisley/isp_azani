package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// One finished report: a title, column headings and rows of cells.
// The DAO fills it; step 9 hands columnArray() and rowArray() straight to a JTable,
// so the data layer never needs to know Swing exists.
public class ReportTable {

    private final String title;
    private final List<String> columns;
    private final List<Object[]> rows;

    public ReportTable(String title, List<String> columns, List<Object[]> rows) {
        this.title = title;
        // Defensive copy, then read-only: no caller can add or remove rows later.
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public String getTitle()        { return title; }
    public List<String> getColumns() { return columns; }
    public List<Object[]> getRows()  { return rows; }
    public int getRowCount()         { return rows.size(); }

    // DefaultTableModel(Object[][] data, Object[] columnNames) takes exactly these two shapes.
    public String[] columnArray()  { return columns.toArray(new String[0]); }
    public Object[][] rowArray()   { return rows.toArray(new Object[0][]); }

    // Returns a NEW report with one extra row that adds up every BigDecimal column.
    // Integer ids, text, dates and percentages stay blank: adding them means nothing.
    // The original object is untouched, so a ReportTable never changes after it is built.
    public ReportTable withTotalRow(String label, int labelColumn) {
        int n = columns.size();
        Object[] total = new Object[n];
        for (Object[] row : rows) {
            for (int c = 0; c < n; c++) {
                if (row[c] instanceof BigDecimal) {
                    BigDecimal value = (BigDecimal) row[c];
                    total[c] = (total[c] == null) ? value : ((BigDecimal) total[c]).add(value);
                }
            }
        }
        total[labelColumn] = label;
        List<Object[]> withTotal = new ArrayList<>(rows);
        withTotal.add(total);
        return new ReportTable(title, columns, withTotal);
    }

    // Plain-text layout: the console now, a .txt export in step 9.
    public String toText() {
        int n = columns.size();
        int[] width = new int[n];
        for (int c = 0; c < n; c++) {
            width[c] = columns.get(c).length();
        }
        for (Object[] row : rows) {                         // widest cell decides the column width
            for (int c = 0; c < n; c++) {
                width[c] = Math.max(width[c], cell(row[c]).length());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" (").append(rows.size())
                .append(rows.size() == 1 ? " row)" : " rows)").append('\n');
        appendLine(sb, columns.toArray(), width);
        for (int c = 0; c < n; c++) {                       // underline under the headings
            sb.append("-".repeat(width[c]));
            if (c < n - 1) {
                sb.append("-+-");
            }
        }
        sb.append('\n');
        for (Object[] row : rows) {
            appendLine(sb, row, width);
        }
        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, Object[] cells, int[] width) {
        for (int c = 0; c < width.length; c++) {
            String text = cell(cells[c]);
            // Numbers line up on the right, text on the left, as in a ledger.
            String pattern = (cells[c] instanceof Number) ? "%" + width[c] + "s" : "%-" + width[c] + "s";
            sb.append(String.format(pattern, text));
            if (c < width.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append('\n');
    }

    // A SQL NULL prints as "-". BigDecimal prints as 820000.00, never as 8.2E+5.
    private static String cell(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        return value.toString();
    }
}