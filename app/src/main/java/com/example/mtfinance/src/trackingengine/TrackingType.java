package com.example.mtfinance.src.trackingengine;

import java.util.HashSet;
import java.util.Set;

public enum TrackingType {
    EXPENSE("expense", "debit", "withdrawal", "direct debit", "eftpos purchase"),
    INCOME("income", "credit", "deposit", "journal credit", "direct credit"),
    ACCOUNT_TRANSFERS("account transfers", "transfer", "money transfer", "savings", "credit transfer", "debit transfer"),
    OTHER("other", "unknown"); // for errors

    private final Set<String> otherNames = new HashSet<>();

    TrackingType(String... otherNames) {
        this.otherNames.addAll(Set.of(otherNames));
    }

    public static TrackingType fromString(String name) {
        if (name == null) {
            return OTHER;
        }
        String cleanName = name.trim().toLowerCase();
        for (TrackingType type : values()) {
            if (type.name().equalsIgnoreCase(cleanName) || type.otherNames.contains(cleanName)) {
                return type;
            }
        }
        return OTHER;
    }


}
