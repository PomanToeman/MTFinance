package com.example.mtfinance.src;

import java.math.BigDecimal;

public class TrackingUtlis {

    public static final String EMPTY_DESCRIPTION = "No description";
    public static void checkAmount(BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount);
        }
    }


    public static String determineDescription(String description) {
        String returningDescription = description;
        if (description == null || description.isEmpty()) {
            returningDescription = EMPTY_DESCRIPTION;
        }
        return returningDescription;
    }




}
