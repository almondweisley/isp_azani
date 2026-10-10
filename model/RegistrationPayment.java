package model; 

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegistrationPayment extends Payment{
    public RegistrationPayment(int institutionId, LocalDate paidOn) {
        super(institutionId, paidOn);
    }
    @Override
    public String getType(){
        return "Registration";
    }

    @Override
    public BigDecimal amountDue(){
        return money(Fees.REGISTRATION);
    }
    
}
