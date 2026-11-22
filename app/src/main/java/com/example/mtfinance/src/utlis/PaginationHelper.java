package com.example.mtfinance.src.utlis;

import java.util.Collections;
import java.util.List;

/**
 * Static independent utility class for pagination calculations and sublist slicing.
 */
public final class PaginationHelper {

    private PaginationHelper() {
        // Private constructor to prevent instantiation
    }

    public static int calculateTotalPages(int totalItems, int pageSize) {
        if (pageSize <= 0) return 1;
        return Math.max(1, (totalItems + pageSize - 1) / pageSize);
    }

    public static <T> List<T> getPageSlice(List<T> items, int page, int pageSize) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        int size = Math.max(1, pageSize);
        int totalPages = calculateTotalPages(items.size(), size);
        int clampedPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (clampedPage - 1) * size;
        if (fromIndex >= items.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
