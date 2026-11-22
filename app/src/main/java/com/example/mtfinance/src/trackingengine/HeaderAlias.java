package com.example.mtfinance.src.trackingengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates common CSV column header name aliases for automatic header detection.
 * <p>
 * This prevents hardcoding literal strings across ViewModel and service classes.
 */
public enum HeaderAlias {

    NAME(Arrays.asList("name", "payee", "merchant", "title", "counterparty", "account name", "transaction name")),
    DESCRIPTION(Arrays.asList("description", "notes", "memo", "details", "extended description", "narrative", "purpose", "remittance")),
    AMOUNT(Arrays.asList("amount", "transaction amount", "sum", "value", "total", "debit/credit amount")),
    DATE(Arrays.asList("date", "transaction date", "posting date", "effective date", "trans date", "valuedate", "booking date")),
    TYPE(Arrays.asList("type", "transaction type", "dr/cr", "debit/credit", "category type", "entry type"));

    private final List<String> aliases;

    HeaderAlias(List<String> aliases) {
        this.aliases = Collections.unmodifiableList(aliases);
    }

    /**
     * Gets the list of alias strings for this header category.
     * @return unmodifiable list of string aliases
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Checks if a raw CSV header matches any alias in this category (case-insensitive).
     * @param header raw header name from CSV file
     * @return true if header matches any alias in this category
     */
    public boolean matches(String header) {
        if (header == null || header.trim().isEmpty()) {
            return false;
        }
        String normalized = header.trim().toLowerCase();
        for (String alias : aliases) {
            if (normalized.equals(alias) || normalized.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the best matching {@link HeaderAlias} for a given raw CSV header string.
     * @param header raw header name from CSV file
     * @return matching {@link HeaderAlias}, or {@code null} if no confident match is found
     */
    public static HeaderAlias findMatch(String header) {
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        for (HeaderAlias aliasEnum : values()) {
            if (aliasEnum.matches(header)) {
                return aliasEnum;
            }
        }
        return null;
    }
}
