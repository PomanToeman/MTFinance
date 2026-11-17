package com.example.mtfinance.src;

import java.util.Arrays;

/**
 * Enum defining supported display currencies and their respective symbols.
 */
public enum CurrencyFormat {

    NZD("$", "NZD - New Zealand Dollar ($)"),
    USD("$", "USD - US Dollar ($)"),
    EUR("€", "EUR - Euro (€)"),
    GBP("£", "GBP - British Pound (£)"),
    JPY("¥", "JPY - Japanese Yen (¥)"),
    AUD("A$", "AUD - Australian Dollar (A$)"),
    CAD("C$", "CAD - Canadian Dollar (C$)");

    private final String symbol;
    private final String displayName;

    CurrencyFormat(String symbol, String displayName) {
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CurrencyFormat fromString(String nameOrSymbol) {
        if (nameOrSymbol == null || nameOrSymbol.trim().isEmpty()) {
            return NZD;
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(nameOrSymbol) || c.symbol.equals(nameOrSymbol))
                .findFirst()
                .orElse(NZD);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
