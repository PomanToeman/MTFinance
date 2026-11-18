package com.example.mtfinance.src.trackingengine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Unit tests for Category tracking engine hierarchy, cycle prevention, depth limits, and stale edge cleanup.
 */
public class CategoryTrackingEngineTest {

    private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(0.1);

    @Test
    public void categoryHierarchy_ParentChildRelationshipAndCyclePrevention_SetsParentAndValidatesDescendant() {
        Category categoryOne = new Category("Subscriptions", "Test 3", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category categoryTwo = new Category("Netflix", "Sub category of subscriptions", DEFAULT_BUDGET, TrackingType.EXPENSE);

        categoryTwo.setParent(categoryOne);
        categoryOne.setParent(categoryTwo); // Cycle prevention check

        assertAll(
            () -> assertEquals(categoryOne, categoryTwo.getParent()),
            () -> assertNotEquals(categoryTwo, categoryOne.getParent()),
            () -> assertFalse(categoryOne.isDescendantOf(categoryTwo)),
            () -> assertTrue(categoryTwo.isDescendantOf(categoryOne))
        );
    }

    @Test
    public void categoryHierarchy_BasicParentChild_SetsParentSuccessfully() {
        Category parent = new Category("Subscriptions", "Main category", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Netflix", "Streaming sub", DEFAULT_BUDGET, TrackingType.EXPENSE);

        child.setParent(parent);

        assertAll(
            () -> assertEquals(parent, child.getParent()),
            () -> assertTrue(child.isDescendantOf(parent)),
            () -> assertFalse(parent.isDescendantOf(child))
        );
    }

    @Test
    public void categoryHierarchy_DeepHierarchy_ValidatesMultiLevelDescendants() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level1 = new Category("Level 1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level2 = new Category("Level 2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level3 = new Category("Level 3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        level1.setParent(root);
        level2.setParent(level1);
        level3.setParent(level2);

        assertAll(
            () -> assertTrue(level3.isDescendantOf(root)),
            () -> assertTrue(level3.isDescendantOf(level1)),
            () -> assertTrue(level2.isDescendantOf(root)),
            () -> assertFalse(root.isDescendantOf(level3))
        );
    }

    @Test
    public void categoryHierarchy_MaxDepthExceeded_ThrowsCategoryViolationException() {
        Category c0 = new Category("C0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c3 = new Category("C3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c4 = new Category("C4", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c5 = new Category("C5", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        c1.setParent(c0);
        c2.setParent(c1);
        c3.setParent(c2);
        c4.setParent(c3);
        c5.setParent(c4);

        Category c6 = new Category("C6", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        assertThrows(CategoryViolationException.class, () -> c6.setParent(c5));
    }

    @Test
    public void shouldRemoveStaleEdgesWhenReassigningParent() {
        Category x = new Category("X", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category y = new Category("Y", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category z = new Category("Z", "", BigDecimal.TEN, TrackingType.EXPENSE);
        x.setCategoryId(1L);
        y.setCategoryId(2L);
        z.setCategoryId(3L);

        assertDoesNotThrow(() -> y.setParent(x));
        assertTrue(x.getChildren(true).contains(y));

        assertDoesNotThrow(() -> y.setParent(z));
        assertFalse(x.getChildren(true).contains(y));
        assertTrue(z.getChildren(true).contains(y));

        assertDoesNotThrow(() -> {
            x.getChildren(true);
            z.getChildren(true);
        });
    }

    @Test
    public void shouldPreventCycleWhenSettingDescendantAsParent() {
        Category parent = new Category("Parent", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BigDecimal.TEN, TrackingType.EXPENSE);
        parent.setCategoryId(10L);
        child.setCategoryId(20L);

        assertDoesNotThrow(() -> child.setParent(parent));
        assertEquals(parent, child.getParent());

        assertDoesNotThrow(() -> parent.setParent(child));
        assertNotEquals(child, parent.getParent());
    }
}
