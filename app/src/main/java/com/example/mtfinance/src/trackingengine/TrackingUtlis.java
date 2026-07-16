package com.example.mtfinance.src.trackingengine;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public class TrackingUtlis {
    // Static fields
    private static final AtomicLong categoryCounter = new AtomicLong(0);
    private static final AtomicLong transactionCounter = new AtomicLong(0);

    public static final String EMPTY_DESCRIPTION = "No description";



    private TrackingUtlis() {
        // Private constructor to prevent instantiation
    }

    // STATIC METHODS

    /**
     * to ensure the amount/budget is a non-zero positive number. Use enums to distinguish between Expenses and income.
     * @param amount - the amount/budget to check.
     * @throws IllegalArgumentException - if the amount is not a non-zero positive number.
     */
    public static void checkAmount(BigDecimal amount) throws IllegalArgumentException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount);
        }
    }

    /**
    * This is to force any null/empty description to a default (EMPTY_DESCRIPTION) for consistency’s sake.
    * returns the same description otherwise.
    *
    *
    * @param description - the description to check.
    *
    *
    *
    *
     */
    public static String determineDescription(String description) {
        String returningDescription = description;
        if (description == null || description.isEmpty()) {
            returningDescription = EMPTY_DESCRIPTION;
        }
        return returningDescription;
    }


    /**
     * To generate a unique ID for each category upon initialisation.
     *
     * @return returns the next unique ID.
     */
    public static long getNextCategoryCounterId() {
        return categoryCounter.incrementAndGet();
    }

    /**
     * To reset the category counter for ID generation. (ONLY MEANT FOR TESTING PURPOSES)
     */
    public static void resetCategoryCounter() {
        categoryCounter.set(0);
    }

    /**
     * To generate a unique ID for each Transaction upon initialisation.
     *
     * @return returns the next unique ID.
     */
    public static long getNextTransactionCounterId() {
        return transactionCounter.incrementAndGet();

    }

    /**
     * To reset the Transaction counter for ID generation. (ONLY MEANT FOR TESTING PURPOSES)
     */
    public static void resetTransactionCounter() {
        transactionCounter.set(0);
    }


    public static TrackingType determineTypeByAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return TrackingType.INCOME;
        }
        else {
            return TrackingType.EXPENSE;
        }
    }





}
