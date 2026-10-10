package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReconnectionPayment extends Payment {

    public ReconnectionPayment(int institutionId, LocalDate paidOn) {
        super(institutionId, paidOn);
    }

    @Override 
    public String getType() {
        return "reconnection";
    }
    @Override 
    public BigDecimal amountDue(){
        return money(Fees.RECONNECTION);
    }
}
