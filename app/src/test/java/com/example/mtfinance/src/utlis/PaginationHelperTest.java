package com.example.mtfinance.src.utlis;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PaginationHelper class adhering to testing protocol.
 */
public class PaginationHelperTest {

    private static final int DEFAULT_PAGE_SIZE = 2;
    private static final int FOUR_ITEMS = 4;
    private static final int PAGE_ONE = 1;
    private static final int PAGE_TWO = 2;
    private static final int PAGE_THREE = 3;

    @Test
    public void calculateTotalPages_validItemsAndPageSize_returnsCorrectPageCount() {
        int totalPages = PaginationHelper.calculateTotalPages(FOUR_ITEMS, DEFAULT_PAGE_SIZE);
        assertEquals(PAGE_TWO, totalPages);
    }

    @Test
    public void getPageSlice_validPageAndItems_returnsCorrectSublist() {
        List<String> items = Arrays.asList("A", "B", "C", "D");
        List<String> slice = PaginationHelper.getPageSlice(items, PAGE_ONE, DEFAULT_PAGE_SIZE);
        assertEquals(2, slice.size());
        assertEquals("A", slice.get(0));
        assertEquals("B", slice.get(1));
    }

    @Test
    public void getPageSlice_pageExceedsBounds_clampsToValidPage() {
        List<String> items = Arrays.asList("A", "B", "C", "D");
        List<String> slice = PaginationHelper.getPageSlice(items, PAGE_THREE, DEFAULT_PAGE_SIZE);
        assertEquals(2, slice.size());
        assertEquals("C", slice.get(0));
        assertEquals("D", slice.get(1));
    }

    @Test
    public void navigateForward_validCurrentPage_incrementsPage() {
        PaginationHelper helper = new PaginationHelper(DEFAULT_PAGE_SIZE);
        helper.setTotalItems(FOUR_ITEMS);
        assertTrue(helper.canNavigateForward());
        helper.navigateForward();
        assertEquals(PAGE_TWO, helper.getCurrentPage());
    }

    @Test
    public void navigateBackward_validCurrentPage_decrementsPage() {
        PaginationHelper helper = new PaginationHelper(DEFAULT_PAGE_SIZE);
        helper.setTotalItems(FOUR_ITEMS);
        helper.setCurrentPage(PAGE_TWO);
        assertTrue(helper.canNavigateBackward());
        helper.navigateBackward();
        assertEquals(PAGE_ONE, helper.getCurrentPage());
    }

    @Test
    public void canNavigateForward_atLastPage_returnsFalse() {
        PaginationHelper helper = new PaginationHelper(DEFAULT_PAGE_SIZE);
        helper.setTotalItems(FOUR_ITEMS);
        helper.setCurrentPage(PAGE_TWO);
        assertFalse(helper.canNavigateForward());
    }
}
