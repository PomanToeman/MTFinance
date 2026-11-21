package com.example.mtfinance.src;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Locale;

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
    MM_DD_YY("MM/dd/yy"),
    FULL_DATE("EEEE, dd MMMM yyyy");

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

    /**
     * Attempts to parse a raw date string input into a {@link LocalDateTime}.
     * Checks ISO formats, system preference format, dd/MM/yyyy, and remaining enum patterns.
     *
     * @param dateStr raw date string input to parse
     * @param systemFormat system preference {@link DateFormat}, or null
     * @return parsed {@link LocalDateTime}, or null if input is unparseable or blank
     */
    public static LocalDateTime parse(String dateStr, DateFormat systemFormat) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateStr.trim();

        try {
            return LocalDateTime.parse(trimmed);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(trimmed).atStartOfDay();
        } catch (Exception ignored) {
        }

        if (systemFormat != null) {
            LocalDateTime systemParsed = parseWithPattern(trimmed, systemFormat.pattern);
            if (systemParsed != null) {
                return systemParsed;
            }
        }

        LocalDateTime ddMmYyyyParsed = parseWithPattern(trimmed, DD_MM_YYYY.pattern);
        if (ddMmYyyyParsed != null) {
            return ddMmYyyyParsed;
        }

        for (DateFormat format : values()) {
            if (format == systemFormat || format == DD_MM_YYYY) {
                continue;
            }
            LocalDateTime parsed = parseWithPattern(trimmed, format.pattern);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    /**
     * Convenience method to parse a date string using default system format matching.
     *
     * @param dateStr raw date string input to parse
     * @return parsed {@link LocalDateTime}, or null if unparseable
     */
    public static LocalDateTime parse(String dateStr) {
        return parse(dateStr, null);
    }

    private static LocalDateTime parseWithPattern(String text, String pattern) {
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.ENGLISH);
            if (pattern.contains("HH:mm")) {
                return LocalDateTime.parse(text, formatter);
            } else {
                return LocalDate.parse(text, formatter).atStartOfDay();
            }
        } catch (Exception ignored) {
        }
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.getDefault());
            if (pattern.contains("HH:mm")) {
                return LocalDateTime.parse(text, formatter);
            } else {
                return LocalDate.parse(text, formatter).atStartOfDay();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String toString() {
        return pattern;   // so printing the enum shows the pattern
    }
}