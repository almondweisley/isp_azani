package model;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class Fees {

    private Fees() { }
        public static final BigDecimal REGISTRATION = new BigDecimal("8500.00");
        public static final BigDecimal INSTALLATION = new BigDecimal("10000.00");
        public static final BigDecimal PC_PRICE = new BigDecimal("40000.00");
        public static final BigDecimal RECONNECTION = new BigDecimal("1000.00");
        public static final BigDecimal FINE_RATE = new BigDecimal("0.15");
        public static final BigDecimal UPGRADE_DISCOUNT = new BigDecimal("0.10");

        public static final int DISCONNECT_DAY = 10;

        public static final int MONEY_SCALE = 2;
        public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;  
    
}