package com.example.mtfinance.src.trackingengine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Unit tests for Category tracking engine hierarchy, cycle prevention, and stale edge cleanup.
 */
public class CategoryTrackingEngineTest {

    @Test
    public void shouldRemoveStaleEdgesWhenReassigningParent() {
        Category x = new Category("X", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category y = new Category("Y", "", BigDecimal.TEN, TrackingType.EXPENSE);
        Category z = new Category("Z", "", BigDecimal.TEN, TrackingType.EXPENSE);
        x.setCategoryId(1L);
        y.setCategoryId(2L);
        z.setCategoryId(3L);

        // y becomes child of x
        assertDoesNotThrow(() -> y.setParent(x));
        assertTrue(x.getChildren(true).contains(y));

        // y moves from x to z (stale edge removal test: x should no longer contain y)
        assertDoesNotThrow(() -> y.setParent(z));
        assertFalse(x.getChildren(true).contains(y));
        assertTrue(z.getChildren(true).contains(y));

        // Ensure tree traversal succeeds without StackOverflowError
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

        // Attempting to set child as parent of parent (creating a cycle) should be ignored
        assertDoesNotThrow(() -> parent.setParent(child));
        assertNotEquals(child, parent.getParent());
    }
}
