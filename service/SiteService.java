package service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import dao.InstitutionDAO;
import dao.SiteDAO;
import model.EquipmentOrder;
import model.InstitutionOption;
import model.LANNodeTier;
import model.ReadinessAssessment;

public class SiteService {

    private final SiteDAO siteDAO = new SiteDAO();

    public List<InstitutionOption> listInstitutions() throws ServiceException {
        try {
            return new InstitutionDAO().listOptions();
        } catch (SQLException e) {
            throw new ServiceException("Could not load institutions. Check the database connection.");
        }
    }

    public ReadinessAssessment recordVisit(int institutionId, LocalDate visitDate, int userCount,
                                           boolean hasComputers, boolean hasLan,
                                           int computerQty, int lanNodes) throws ServiceException {
        // Input rules, checked before any database work.
        if (visitDate.isAfter(LocalDate.now())) {
            throw new ServiceException("The visit date cannot lie in the future.");
        }
        if (userCount <= 0) {
            throw new ServiceException("Enter the number of users; it must be at least 1.");
        }
        if (computerQty < 0 || lanNodes < 0) {
            throw new ServiceException("Quantities cannot be negative.");
        }
        if (hasComputers && computerQty > 0) {
            throw new ServiceException("The institution already has enough computers. Leave the computer quantity at 0.");
        }
        if (hasLan && lanNodes > 0) {
            throw new ServiceException("The institution already has a LAN. Leave the LAN nodes at 0.");
        }

        try {
            // Decision: a new visit is refused only after a visit that found the institution ready.
            if (Boolean.TRUE.equals(siteDAO.latestReadiness(institutionId))) {
                throw new ServiceException("The institution already passed its site visit.");
            }

            LANNodeTier tier = null;
            if (lanNodes > 0) {
                tier = siteDAO.findTierFor(lanNodes);
                if (tier == null) {   // decision 2A: Table 2 covers 2 to 100 nodes only
                    throw new ServiceException("Azani sells LAN packages for 2 to 100 nodes only; "
                            + lanNodes + " nodes falls outside that range.");
                }
            }

            ReadinessAssessment visit = new ReadinessAssessment(
                    institutionId, visitDate, userCount, hasComputers, hasLan);
            if (computerQty > 0 || tier != null) {
                visit.setOrder(new EquipmentOrder(institutionId, computerQty, tier, visitDate));
            }
            siteDAO.saveVisit(visit);
            return visit;
        } catch (SQLException e) {
            throw new ServiceException("Could not save the site visit. ", e);
        }
    }
}