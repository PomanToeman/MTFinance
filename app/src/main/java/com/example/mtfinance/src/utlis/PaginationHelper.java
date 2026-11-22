package com.example.mtfinance.src.utlis;

import java.util.Collections;
import java.util.List;

/**
 * Utility class for pagination calculations and sublist slicing.
 */
public class PaginationHelper {

    private int pageSize;
    private int currentPage;
    private int totalItems;

    public PaginationHelper(int pageSize) {
        this.pageSize = pageSize > 0 ? pageSize : 10;
        this.currentPage = 1;
        this.totalItems = 0;
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

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = Math.max(1, Math.min(page, Math.max(1, calculateTotalPages())));
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize > 0 ? pageSize : 10;
        setCurrentPage(this.currentPage);
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = Math.max(0, totalItems);
        setCurrentPage(this.currentPage);
    }

    public int calculateTotalPages() {
        return calculateTotalPages(totalItems, pageSize);
    }

    public boolean canNavigateForward() {
        return currentPage < calculateTotalPages();
    }

    public boolean canNavigateBackward() {
        return currentPage > 1;
    }

    public void navigateForward() {
        if (canNavigateForward()) {
            currentPage++;
        }
    }

    public void navigateBackward() {
        if (canNavigateBackward()) {
            currentPage--;
        }
    }

    public <T> List<T> getPageSlice(List<T> items) {
        if (items == null) {
            this.totalItems = 0;
            return Collections.emptyList();
        }
        this.totalItems = items.size();
        return getPageSlice(items, currentPage, pageSize);
    }
}
