package dao;

import model.BandwidthPlan;
import model.Subscription;

// A quick check of the read methods. It writes nothing, so you can rerun it freely.
public class ServiceDAOCheck {

    public static void main(String[] args) throws Exception {
        ServiceDAO dao = new ServiceDAO();

        for (BandwidthPlan p : dao.listPlans()) {
            System.out.println(p.getPlanId() + "  " + p);
        }

        Subscription s = dao.findByInstitution(1);
        System.out.println(s == null
                ? "Institution 1 has no subscription yet."
                : "Institution 1 holds plan " + s.getPlanId() + ", status " + s.getStatus());
    }
}