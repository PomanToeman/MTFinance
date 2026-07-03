package com.example.mtfinance.src;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public class TrackingUtlis {
    private static final AtomicLong categoryCounter = new AtomicLong(0);
    private static final AtomicLong transactionCounter = new AtomicLong(0);

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



    public static long getNextCategoryCounterId() {
        return categoryCounter.incrementAndGet();
    }

    public static void resetCategoryCounter() {
        categoryCounter.set(0);
    }

    public static long getNextTransactionCounterId() {
        return transactionCounter.incrementAndGet();

    }

    public static void resetTransactionCounter() {
        transactionCounter.set(0);
    }




}
