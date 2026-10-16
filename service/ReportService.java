package service;

import dao.ReportDAO;
import model.ReportTable;
import model.ReportType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

// Task 5: the form asks for a report here; this class asks ReportDAO.
// The form never sees ReportDAO or an SQLException, only a ReportTable or a
// ServiceException whose message is safe to show the user.
public class ReportService {

    private final ReportDAO dao = new ReportDAO();

    public ReportTable load(ReportType type) throws ServiceException {
        if (type == null) {
            throw new ServiceException("Choose a report.");
        }
        try {
            // Switch expression: each case yields a value. It covers every constant,
            // so adding a tenth report without a case here stops the compile.
            return switch (type) {
                case REGISTERED_INSTITUTIONS -> dao.registeredInstitutions();
                case DEFAULTERS              -> dao.defaulters();
                case DISCONNECTION_ISSUES    -> dao.disconnectionIssues();
                case INFRASTRUCTURE          -> dao.infrastructureRequirements();
                case INSTALLATION_COST       -> dao.installationCost();
                case EQUIPMENT_COST          -> dao.equipmentCost();
                case UPGRADED_CHARGES        -> dao.upgradedCharges();
                case CHARGES_BY_CATEGORY     -> dao.chargesByCategory();
                case AMOUNT_PER_SERVICE      -> dao.amountPerService();
            };
        } catch (SQLException e) {
            throw new ServiceException("Could not load " + type + ". Check the database connection.", e);
        }
    }

    // Writes the report as CSV: one heading line, then one line per row.
    // Excel and Google Sheets open the file directly.
    public void exportCsv(ReportTable table, Path file) throws ServiceException {
        if (table == null) {
            throw new ServiceException("Show a report before exporting it.");
        }
        if (file == null) {
            throw new ServiceException("Choose where to save the file.");
        }
        // try-with-resources closes (and flushes) the writer even if a write fails.
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeLine(out, table.getColumns().toArray());
            for (Object[] row : table.getRows()) {
                writeLine(out, row);
            }
        } catch (IOException e) {
            throw new ServiceException("Could not save " + file.getFileName()
                    + ". Close it if it is open in Excel, then try again.", e);
        }
    }

    private static void writeLine(BufferedWriter out, Object[] cells) throws IOException {
        for (int c = 0; c < cells.length; c++) {
            if (c > 0) {
                out.write(',');
            }
            out.write(csvField(cells[c]));
        }
        out.newLine();
    }

    // CSV rule: a field holding a comma, a quote or a line break goes inside quotes,
    // and each quote inside it is doubled. "Nakuru, Nakuru County" needs this.
    private static String csvField(Object value) {
        if (value == null) {
            return "";                                          // empty cell, not the word null
        }
        String text = (value instanceof BigDecimal)
                ? ((BigDecimal) value).toPlainString()          // 820000.00, never 8.2E+5
                : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}