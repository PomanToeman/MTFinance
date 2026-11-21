package com.example.mtfinance.src.trackingengine;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;

/**
 * Centralized default application settings constants for easy editing and maintenance.
 */
public final class DefaultSettings {
    public static final String DEFAULT_THEME_MODE = "SYSTEM";
    public static final CurrencyFormat DEFAULT_CURRENCY_FORMAT = CurrencyFormat.NZD;
    public static final DateFormat DEFAULT_DATE_FORMAT = DateFormat.DD_MM_YYYY;
    public static final int DEFAULT_START_DAY_OF_MONTH = 1;
    public static final boolean DEFAULT_INCLUDE_PROVISIONAL = true;
    public static final int MIN_START_DAY = 1;
    public static final int MAX_START_DAY = 31;

    private DefaultSettings() {
        // Private constructor to prevent instantiation
    }
}
