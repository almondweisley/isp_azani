package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InstallationPayment extends Payment {
    public InstallationPayment(int institutionId, LocalDate paidOn) {
        super(institutionId, paidOn);

    }

    @Override
    public String getType() {
        return "installation";
    }

    @Override
    public BigDecimal amountDue() {
        return money(Fees.INSTALLATION);
    }
    
}
