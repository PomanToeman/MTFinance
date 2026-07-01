package com.example.mtfinance.src;

import java.math.BigDecimal;

public class TrackingUtlis {
    public static void checkAmount(BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount);
        }
    }




}
