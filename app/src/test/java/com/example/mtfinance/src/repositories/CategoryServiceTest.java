package com.example.mtfinance.src.repositories;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

public class CategoryServiceTest {

    @Mock
    private CategoryDao categoryDao;

    private CategoryService categoryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryService = new CategoryService(categoryDao, false);
    }

    @Test
    public void getCategoryById_validId_returnsCategory() {
        Category category = new Category("Test", "", BigDecimal.TEN, TrackingType.EXPENSE);
        category.setCategoryId(1L);
        when(categoryDao.getById(1L)).thenReturn(category);

        Category result = categoryService.getCategoryById(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("Test", result.getName())
        );
    }

    @Test
    public void categoryExists_existingId_returnsTrue() {
        when(categoryDao.exists(1L)).thenReturn(true);
        assertTrue(categoryService.categoryExists(1L));
    }

    @Test
    public void getCategoryByName_validName_returnsCategory() {
        Category category = new Category("Groceries", "Desc", BigDecimal.TEN, TrackingType.EXPENSE);
        category.setCategoryId(2L);
        when(categoryDao.getByName("Groceries")).thenReturn(category);
        when(categoryDao.getById(2L)).thenReturn(category);

        Category result = categoryService.getCategoryByName("Groceries");

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("Groceries", result.getName())
        );
    }

    @Test
    public void testStaleEdgeCycleStackOverflowScenario() {
        Category x = new Category("X", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category y = new Category("Y", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category z = new Category("Z", "", BigDecimal.TEN, TrackingType.EXPENSE);
        x.setCategoryId(1L);
        y.setCategoryId(2L);
        z.setCategoryId(3L);

        y.setParent(x); // x.children = {y}
        y.setParent(z); // z.children = {y}, but x.children STILL contains y (stale edge)

        try {
            x.setParent(y);
            org.junit.jupiter.api.Assertions.assertThrows(StackOverflowError.class, () -> {
                x.getChildren(true);
            });
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
