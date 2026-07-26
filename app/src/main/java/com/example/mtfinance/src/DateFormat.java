package com.example.mtfinance.src;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public enum DateFormat {

    ISO("yyyy-MM-dd"),
    DD_MM_YYYY("dd/MM/yyyy"),
    DD_MM_YYYY_HH_MM("dd/MM/yyyy HH:mm"),
    MM_DD_YYYY("MM/dd/yyyy"),
    YYYY_MM_DD("yyyy/MM/dd"),
    D_M_YYYY("d/M/yyyy"),
    M_D_YYYY("M/d/yyyy"),

    DD_MM_YYYY_DASH("dd-MM-yyyy"),
    MM_DD_YYYY_DASH("MM-dd-yyyy"),
    YYYY_MM_DD_DASH("yyyy-MM-dd"),
    D_M_YYYY_DASH("d-M-yyyy"),
    M_D_YYYY_DASH("M-d-yyyy"),
    DD_MM_YY("dd/MM/yy"),
    MM_DD_YY("MM/dd/yy");

    private final String pattern;

    DateFormat(String pattern) {
        this.pattern = pattern;
    }

    /** The pattern string itself (useful for display / persistence). */
    public String getString() {
        return pattern;
    }

    /** Convenience: obtain a ready-to-use formatter. */
    public DateTimeFormatter toFormatter() {
        return DateTimeFormatter.ofPattern(pattern);
    }

    /**
     * Looks up an enum constant by its pattern string.
     * Throws IllegalArgumentException if no match is found.
     */
    public static DateFormat fromString(String pattern) {
        return Arrays.stream(values())
                .filter(f -> f.pattern.equals(pattern))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown date format pattern: " + pattern));
    }

    @Override
    public String toString() {
        return pattern;   // so printing the enum shows the pattern
    }
}