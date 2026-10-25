package service;

import dao.DashboardDAO;
import model.DashboardStats;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * The dashboard's only door to the data. The form calls this; this calls the DAO.
 * It turns a technical SQLException into a ServiceException with a sentence a user can read.
 */
public class DashboardService {

    private static final int RECENT_ROWS = 8;
    private final DashboardDAO dao = new DashboardDAO();

    public DashboardStats load() throws ServiceException {
        try {
            return dao.load(LocalDate.now(), RECENT_ROWS);
                } catch (SQLException e) {
            // Pass MySQL's own reason through, so the status label says what actually went wrong.
            throw new ServiceException("Could not load the dashboard: " + e.getMessage(), e);
        }
    }
}