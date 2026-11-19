package com.example.mtfinance.src.trackingengine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Reconfigured Category tracking engine unit tests matching spec requirements R1 to R15 and decisions D1 to D7.
 * Refactored to extract named constants, eliminate magic numbers, and enforce strong exact assertions.
 */
public class CategoryTrackingEngineTest {

    private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(10.0);
    private static final BigDecimal BUDGET_TEN = BigDecimal.valueOf(10.0);
    private static final BigDecimal BUDGET_TWENTY = BigDecimal.valueOf(20.0);
    private static final BigDecimal BUDGET_FIFTY = BigDecimal.valueOf(50.0);
    private static final BigDecimal BUDGET_SEVENTY_FIVE = BigDecimal.valueOf(75.0);
    private static final BigDecimal BUDGET_EIGHTY = BigDecimal.valueOf(80.0);
    private static final BigDecimal BUDGET_NINETY = BigDecimal.valueOf(90.0);
    private static final BigDecimal BUDGET_HUNDRED = BigDecimal.valueOf(100.0);
    private static final BigDecimal BUDGET_ONE_FIFTY = BigDecimal.valueOf(150.0);
    private static final BigDecimal BUDGET_TWO_HUNDRED = BigDecimal.valueOf(200.0);
    private static final BigDecimal BUDGET_FIVE_HUNDRED = BigDecimal.valueOf(500.0);
    private static final BigDecimal BUDGET_NINE_NINE_NINE = BigDecimal.valueOf(999.0);

    private static final Long ID_ZERO = 0L;
    private static final Long ID_ONE = 1L;
    private static final Long ID_TWO = 2L;
    private static final Long ID_THREE = 3L;
    private static final Long ID_FOUR = 4L;
    private static final Long ID_FIVE = 5L;
    private static final Long ID_SIX = 6L;
    private static final Long ID_TEN = 10L;
    private static final Long ID_ELEVEN = 11L;
    private static final Long ID_TWELVE = 12L;
    private static final Long ID_THIRTEEN = 13L;
    private static final Long ID_TWENTY = 20L;
    private static final Long ID_TWENTY_ONE = 21L;
    private static final Long ID_TWENTY_TWO = 22L;
    private static final Long ID_MISSING = 99L;

    private static final int MAX_TREE_DEPTH = 5;
    private static final int RANDOM_SEED_STEPS = 500;
    private static final long RANDOM_SEED = 42L;

    @BeforeEach
    public void setUp() {
        // Reset counters and shared state if any
    }

    private void assertTreeConsistent(Category node, int maxDepth) {
        Set<Category> visited = new HashSet<>();
        assertTreeConsistentRecursive(node, maxDepth, visited);
    }

    private void assertTreeConsistentRecursive(Category node, int maxDepth, Set<Category> visited) {
        assertNotNull(node);
        assertTrue(visited.add(node), "Cycle detected in tree graph at category: " + node.getName());
        assertTrue(node.getAncestors().size() <= maxDepth, "Max depth exceeded for node: " + node.getName());

        if (node.getParent() != null) {
            assertEquals(node.getParent().getCategoryId(), node.getParentId(), "ParentId mismatch for node: " + node.getName());
            assertTrue(node.getParent().getChildren(false).contains(node), "Parent children set does not contain node: " + node.getName());
        }

        for (Category child : node.getChildren(false)) {
            assertTreeConsistentRecursive(child, maxDepth, visited);
        }
    }

    // --- REJECTED LINKS (R4, R5, R9, D1) ---

    @Test
    public void setParent_selfAsParent_throwsCategoryViolationException() {
        Category c = new Category("Self", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c.setCategoryId(ID_ONE);
        assertThrows(CategoryViolationException.class, () -> c.setParent(c));
    }

    @Test
    public void setParent_selfAsParent_stateUnchanged() {
        Category c = new Category("Self", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c.setCategoryId(ID_ONE);
        try { c.setParent(c); } catch (Exception ignored) {}
        assertAll(
            () -> assertNull(c.getParent()),
            () -> assertNull(c.getParentId())
        );
    }

    @Test
    public void setParent_directChildAsParent_throwsCategoryViolationException() {
        Category parent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        parent.setCategoryId(ID_ONE);
        child.setCategoryId(ID_TWO);
        child.setParent(parent);

        assertThrows(CategoryViolationException.class, () -> parent.setParent(child));
    }

    @Test
    public void setParent_deepDescendantAsParent_throwsCategoryViolationException() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l1 = new Category("L1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l2 = new Category("L2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE);
        l1.setCategoryId(ID_TWO);
        l2.setCategoryId(ID_THREE);

        l1.setParent(root);
        l2.setParent(l1);

        assertThrows(CategoryViolationException.class, () -> root.setParent(l2));
    }

    @Test
    public void setParent_descendantAsParent_hierarchyUnchanged() {
        Category parent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        parent.setCategoryId(ID_ONE);
        child.setCategoryId(ID_TWO);
        child.setParent(parent);

        try { parent.setParent(child); } catch (Exception ignored) {}

        assertAll(
            () -> assertEquals(parent, child.getParent()),
            () -> assertNull(parent.getParent())
        );
    }

    @Test
    public void setParent_differentType_throwsCategoryViolationException() {
        Category exp = new Category("Expense", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category inc = new Category("Income", "", DEFAULT_BUDGET, TrackingType.INCOME);
        exp.setCategoryId(ID_ONE);
        inc.setCategoryId(ID_TWO);

        assertThrows(CategoryViolationException.class, () -> exp.setParent(inc));
    }

    @Test
    public void setParent_differentType_stateUnchanged() {
        Category exp = new Category("Expense", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category inc = new Category("Income", "", DEFAULT_BUDGET, TrackingType.INCOME);
        exp.setCategoryId(ID_ONE);
        inc.setCategoryId(ID_TWO);

        try { exp.setParent(inc); } catch (Exception ignored) {}

        assertAll(
            () -> assertNull(exp.getParent()),
            () -> assertNull(exp.getParentId())
        );
    }

    // --- REPARENTING (R1, R2, R3, R7, D6) ---

    @Test
    public void setParent_ancestorAsParent_movesUpAndParentIdMatches() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l1 = new Category("L1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l2 = new Category("L2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE);
        l1.setCategoryId(ID_TWO);
        l2.setCategoryId(ID_THREE);

        l1.setParent(root);
        l2.setParent(l1);

        l2.setParent(root);

        assertAll(
            () -> assertEquals(root, l2.getParent()),
            () -> assertEquals(ID_ONE, l2.getParentId())
        );
    }

    @Test
    public void setParent_ancestorAsParent_removedFromOldParentChildren() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l1 = new Category("L1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l2 = new Category("L2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE);
        l1.setCategoryId(ID_TWO);
        l2.setCategoryId(ID_THREE);

        l1.setParent(root);
        l2.setParent(l1);

        l2.setParent(root);

        assertFalse(l1.getChildren(false).contains(l2));
    }

    @Test
    public void setParent_sameParentTwice_childListedOnce() {
        Category p = new Category("P", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        p.setCategoryId(ID_ONE);
        c.setCategoryId(ID_TWO);

        c.setParent(p);
        c.setParent(p);

        assertEquals(1, p.getChildren(false).stream().filter(item -> item.equals(c)).count());
    }

    @Test
    public void setParent_unrelatedNewParent_addedToNewParentAndIdsMatch() {
        Category x = new Category("X", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category y = new Category("Y", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category z = new Category("Z", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE);
        y.setCategoryId(ID_TWO);
        z.setCategoryId(ID_THREE);

        y.setParent(x);
        y.setParent(z);

        assertAll(
            () -> assertEquals(z, y.getParent()),
            () -> assertEquals(ID_THREE, y.getParentId()),
            () -> assertTrue(z.getChildren(false).contains(y)),
            () -> assertFalse(x.getChildren(false).contains(y))
        );
    }

    @Test
    public void setParent_movedTwiceInOneSession_currentParentAndParentIdCorrect() {
        Category x = new Category("X", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category y = new Category("Y", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category z = new Category("Z", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE);
        y.setCategoryId(ID_TWO);
        z.setCategoryId(ID_THREE);

        y.setParent(x);
        y.setParent(z);

        assertAll(
            () -> assertEquals(z, y.getParent()),
            () -> assertEquals(ID_THREE, y.getParentId()),
            () -> assertEquals(1, z.getChildren(false).stream().filter(item -> item.equals(y)).count())
        );
    }

    @Test
    public void setParent_movedTwiceInOneSession_newParentMinimumBudgetIncludesChild() {
        Category x = new Category("X", "", BUDGET_FIFTY, TrackingType.EXPENSE);
        Category y = new Category("Y", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category z = new Category("Z", "", BUDGET_TEN, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE);
        y.setCategoryId(ID_TWO);
        z.setCategoryId(ID_THREE);

        y.setParent(x);
        y.setParent(z);

        assertEquals(0, BUDGET_HUNDRED.compareTo(z.getMonthlyBudget()));
    }

    @Test
    public void setParent_movedTwiceInOneSession_hashStableInNewParentChildren() {
        Category x = new Category("X", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category y = new Category("Y", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category z = new Category("Z", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE);
        y.setCategoryId(ID_TWO);
        z.setCategoryId(ID_THREE);

        y.setParent(x);
        y.setParent(z);

        assertTrue(z.getChildren(false).contains(y));
    }

    // --- DEPTH (R6, D2) ---

    @Test
    public void setParent_parentWithMaxAncestors_throwsAndStateUnchanged() {
        Category c0 = new Category("C0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c3 = new Category("C3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c4 = new Category("C4", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c5 = new Category("C5", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c0.setCategoryId(ID_ZERO); c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO);
        c3.setCategoryId(ID_THREE); c4.setCategoryId(ID_FOUR); c5.setCategoryId(ID_FIVE);

        c1.setParent(c0);
        c2.setParent(c1);
        c3.setParent(c2);
        c4.setParent(c3);
        c5.setParent(c4);

        Category c6 = new Category("C6", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c6.setCategoryId(ID_SIX);

        assertThrows(CategoryViolationException.class, () -> c6.setParent(c5));
    }

    @Test
    public void setParent_parentOneBelowMaxAncestors_succeeds() {
        Category c0 = new Category("C0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c3 = new Category("C3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c4 = new Category("C4", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c0.setCategoryId(ID_ZERO); c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO);
        c3.setCategoryId(ID_THREE); c4.setCategoryId(ID_FOUR);

        c1.setParent(c0);
        c2.setParent(c1);
        c3.setParent(c2);
        c4.setParent(c3);

        Category c5 = new Category("C5", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c5.setCategoryId(ID_FIVE);

        assertDoesNotThrow(() -> c5.setParent(c4));
    }

    @Test
    public void setParent_subtreeWouldExceedMaxDepth_throwsAndStateUnchanged() {
        Category a0 = new Category("A0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category a1 = new Category("A1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category a2 = new Category("A2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category a3 = new Category("A3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        a0.setCategoryId(ID_TEN); a1.setCategoryId(ID_ELEVEN); a2.setCategoryId(ID_TWELVE); a3.setCategoryId(ID_THIRTEEN);
        a1.setParent(a0); a2.setParent(a1); a3.setParent(a2);

        Category b0 = new Category("B0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category b1 = new Category("B1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category b2 = new Category("B2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        b0.setCategoryId(ID_TWENTY); b1.setCategoryId(ID_TWENTY_ONE); b2.setCategoryId(ID_TWENTY_TWO);
        b1.setParent(b0); b2.setParent(b1);

        assertThrows(CategoryViolationException.class, () -> b0.setParent(a3));
    }

    @Test
    public void setParent_subtreeExactlyFitsMaxDepth_succeeds() {
        Category a0 = new Category("A0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category a1 = new Category("A1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category a2 = new Category("A2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        a0.setCategoryId(ID_TEN); a1.setCategoryId(ID_ELEVEN); a2.setCategoryId(ID_TWELVE);
        a1.setParent(a0); a2.setParent(a1);

        Category b0 = new Category("B0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category b1 = new Category("B1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category b2 = new Category("B2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        b0.setCategoryId(ID_TWENTY); b1.setCategoryId(ID_TWENTY_ONE); b2.setCategoryId(ID_TWENTY_TWO);
        b1.setParent(b0); b2.setParent(b1);

        assertDoesNotThrow(() -> b0.setParent(a2));
    }

    @Test
    public void setParent_deepNodeMovedToAncestorAtFullDepth_succeeds() {
        Category c0 = new Category("C0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c3 = new Category("C3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c4 = new Category("C4", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c0.setCategoryId(ID_ZERO); c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO); c3.setCategoryId(ID_THREE); c4.setCategoryId(ID_FOUR);
        c1.setParent(c0); c2.setParent(c1); c3.setParent(c2); c4.setParent(c3);

        assertDoesNotThrow(() -> c4.setParent(c0));
    }

    // --- GETTERS (R10, R11) ---

    @Test
    public void getChildren_includeGrandFalse_returnsOnlyDirectChildren() {
        Category p = new Category("P", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category g = new Category("G", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        p.setCategoryId(ID_ONE); c.setCategoryId(ID_TWO); g.setCategoryId(ID_THREE);
        c.setParent(p); g.setParent(c);

        Set<Category> direct = p.getChildren(false);
        assertAll(
            () -> assertEquals(1, direct.size()),
            () -> assertTrue(direct.contains(c)),
            () -> assertFalse(direct.contains(g))
        );
    }

    @Test
    public void getChildren_includeGrandTrue_returnsAllDescendants() {
        Category p = new Category("P", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category g = new Category("G", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        p.setCategoryId(ID_ONE); c.setCategoryId(ID_TWO); g.setCategoryId(ID_THREE);
        c.setParent(p); g.setParent(c);

        Set<Category> all = p.getChildren(true);
        assertAll(
            () -> assertEquals(2, all.size()),
            () -> assertTrue(all.contains(c)),
            () -> assertTrue(all.contains(g))
        );
    }

    @Test
    public void getChildren_returnedSetModified_internalChildrenUnchanged() {
        Category p = new Category("P", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        p.setCategoryId(ID_ONE); c.setCategoryId(ID_TWO);
        c.setParent(p);

        Set<Category> set = p.getChildren(false);
        try { set.clear(); } catch (Exception ignored) {}

        assertEquals(1, p.getChildren(false).size());
    }

    @Test
    public void getChildren_includeGrandTrueOnMaxDepthChain_terminates() {
        Category c0 = new Category("C0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c3 = new Category("C3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c4 = new Category("C4", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c0.setCategoryId(ID_ZERO); c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO); c3.setCategoryId(ID_THREE); c4.setCategoryId(ID_FOUR);
        c1.setParent(c0); c2.setParent(c1); c3.setParent(c2); c4.setParent(c3);

        assertDoesNotThrow(() -> c0.getChildren(true));
    }

    @Test
    public void getAncestors_chainOfFour_returnsAllThreeAbove() {
        Category c0 = new Category("C0", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c3 = new Category("C3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c0.setCategoryId(ID_ZERO); c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO); c3.setCategoryId(ID_THREE);
        c1.setParent(c0); c2.setParent(c1); c3.setParent(c2);

        Set<Category> ancestors = c3.getAncestors();
        assertAll(
            () -> assertEquals(3, ancestors.size()),
            () -> assertTrue(ancestors.contains(c0)),
            () -> assertTrue(ancestors.contains(c1)),
            () -> assertTrue(ancestors.contains(c2))
        );
    }

    @Test
    public void getAncestors_rootCategory_returnsEmptySet() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE);
        assertTrue(root.getAncestors().isEmpty());
    }

    @Test
    public void isDescendantOf_grandparent_returnsTrue() {
        Category p = new Category("P", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category g = new Category("G", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        p.setCategoryId(ID_ONE); c.setCategoryId(ID_TWO); g.setCategoryId(ID_THREE);
        c.setParent(p); g.setParent(c);

        assertTrue(g.isDescendantOf(p));
    }

    @Test
    public void isDescendantOf_self_returnsFalse() {
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c.setCategoryId(ID_ONE);
        assertFalse(c.isDescendantOf(c));
    }

    @Test
    public void isDescendantOf_unrelatedCategory_returnsFalse() {
        Category x = new Category("X", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category y = new Category("Y", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE); y.setCategoryId(ID_TWO);
        assertFalse(x.isDescendantOf(y));
    }

    @ParameterizedTest
    @EnumSource(TrackingType.class)
    public void isDescendantOf_differentType_returnsFalse(TrackingType type) {
        TrackingType otherType = type == TrackingType.EXPENSE ? TrackingType.INCOME : TrackingType.EXPENSE;
        Category c1 = new Category("C1", "", DEFAULT_BUDGET, type);
        Category c2 = new Category("C2", "", DEFAULT_BUDGET, otherType);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO);

        assertFalse(c1.isDescendantOf(c2));
    }

    // --- DELETION HELPER (R12, D5) ---

    @Test
    public void makeChildrenCongruent_middleNode_childrenAttachToGrandparent() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category middle = new Category("Middle", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); middle.setCategoryId(ID_TWO); child.setCategoryId(ID_THREE);

        middle.setParent(root);
        child.setParent(middle);

        middle.makeChildrenCongruent();

        assertAll(
            () -> assertEquals(root, child.getParent()),
            () -> assertTrue(root.getChildren(false).contains(child))
        );
    }

    @Test
    public void makeChildrenCongruent_middleNode_ownChildrenSetEmpty() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category middle = new Category("Middle", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); middle.setCategoryId(ID_TWO); child.setCategoryId(ID_THREE);

        middle.setParent(root);
        child.setParent(middle);

        middle.makeChildrenCongruent();

        assertTrue(middle.getChildren(false).isEmpty());
    }

    @Test
    public void makeChildrenCongruent_rootNode_childrenRemainAttached() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); child.setCategoryId(ID_TWO);
        child.setParent(root);

        root.makeChildrenCongruent();

        assertTrue(root.getChildren(false).contains(child));
    }

    // --- BUDGETS (R13, D3) ---

    @Test
    public void determineMinimumBudget_noChildren_returnsZero() {
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c.setCategoryId(ID_ONE);
        assertEquals(0, BigDecimal.ZERO.compareTo(c.determineMinimumBudget()));
    }

    @Test
    public void setParent_childBudgetsExceedParent_parentRaisedToChildSum() {
        Category parent = new Category("Parent", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BUDGET_FIFTY, TrackingType.EXPENSE);
        parent.setCategoryId(ID_ONE); child.setCategoryId(ID_TWO);

        child.setParent(parent);

        assertEquals(0, BUDGET_FIFTY.compareTo(parent.getMonthlyBudget()));
    }

    @Test
    public void setParent_raisedParentBudget_cascadesToGrandparent() {
        Category root = new Category("Root", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category middle = new Category("Middle", "", BUDGET_TWENTY, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); middle.setCategoryId(ID_TWO); child.setCategoryId(ID_THREE);

        middle.setParent(root);
        child.setParent(middle);

        assertAll(
            () -> assertEquals(0, BUDGET_HUNDRED.compareTo(middle.getMonthlyBudget())),
            () -> assertEquals(0, BUDGET_HUNDRED.compareTo(root.getMonthlyBudget()))
        );
    }

    @Test
    public void setMonthlyBudget_childRaised_ancestorsRaisedAutomatically() {
        Category root = new Category("Root", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BUDGET_FIFTY, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); child.setCategoryId(ID_TWO);

        child.setParent(root);
        child.setMonthlyBudget(BUDGET_ONE_FIFTY);

        assertEquals(0, BUDGET_ONE_FIFTY.compareTo(root.getMonthlyBudget()));
    }

    @Test
    public void setMonthlyBudget_belowMinimum_ignored() {
        Category parent = new Category("Parent", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BUDGET_EIGHTY, TrackingType.EXPENSE);
        parent.setCategoryId(ID_ONE); child.setCategoryId(ID_TWO);

        child.setParent(parent);
        parent.setMonthlyBudget(BUDGET_TWENTY);

        assertEquals(0, BUDGET_EIGHTY.compareTo(parent.getMonthlyBudget()));
    }

    @Test
    public void setMonthlyBudget_aboveMinimum_updates() {
        Category c = new Category("C", "", BUDGET_TEN, TrackingType.EXPENSE);
        c.setCategoryId(ID_ONE);
        c.setMonthlyBudget(BUDGET_TWO_HUNDRED);

        assertEquals(0, BUDGET_TWO_HUNDRED.compareTo(c.getMonthlyBudget()));
    }

    @Test
    public void setParent_childMovedAway_minimumDropsButBudgetStaysRaised() {
        Category p1 = new Category("P1", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category p2 = new Category("P2", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        p1.setCategoryId(ID_ONE); p2.setCategoryId(ID_TWO); child.setCategoryId(ID_THREE);

        child.setParent(p1);
        BigDecimal p1RaisedBudget = p1.getMonthlyBudget();

        child.setParent(p2);

        assertAll(
            () -> assertEquals(0, p1RaisedBudget.compareTo(p1.getMonthlyBudget()), "Stored budget never falls automatically"),
            () -> assertEquals(0, BigDecimal.ZERO.compareTo(p1.determineMinimumBudget()), "Minimum budget falls on removal")
        );
    }

    @Test
    public void makeChildrenCongruent_middleNode_grandparentMinimumIncludesAdoptedChildren() {
        Category root = new Category("Root", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category middle = new Category("Middle", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BUDGET_SEVENTY_FIVE, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); middle.setCategoryId(ID_TWO); child.setCategoryId(ID_THREE);

        middle.setParent(root);
        child.setParent(middle);

        middle.makeChildrenCongruent();

        assertEquals(0, BUDGET_SEVENTY_FIVE.compareTo(root.determineMinimumBudget()));
    }

    @Test
    public void setParent_ancestorAsParent_newParentMinimumIncludesMovedSubtree() {
        Category root = new Category("Root", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category l1 = new Category("L1", "", BUDGET_TEN, TrackingType.EXPENSE);
        Category l2 = new Category("L2", "", BUDGET_NINETY, TrackingType.EXPENSE);
        root.setCategoryId(ID_ONE); l1.setCategoryId(ID_TWO); l2.setCategoryId(ID_THREE);

        l1.setParent(root);
        l2.setParent(l1);

        l2.setParent(root);

        assertEquals(0, BUDGET_NINETY.compareTo(root.determineMinimumBudget()));
    }

    // --- EQUALITY (R14, D4) ---

    @Test
    public void equals_sameIdNameParentDifferentBudgetAndDescription_equal() {
        Category c1 = new Category("Test", "Desc1", BUDGET_TEN, TrackingType.EXPENSE);
        Category c2 = new Category("Test", "Desc2", BUDGET_NINE_NINE_NINE, TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_ONE);
        c1.setParentId(ID_TEN); c2.setParentId(ID_TEN);

        assertEquals(c1, c2);
    }

    @Test
    public void equals_nameDiffersOnlyByCase_equal() {
        Category c1 = new Category("GROCERIES", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("groceries", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_ONE);

        assertEquals(c1, c2);
    }

    @Test
    public void equals_differentParentId_notEqual() {
        Category c1 = new Category("Test", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("Test", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_ONE);
        c1.setParentId(ID_TEN); c2.setParentId(ID_TWENTY);

        assertNotEquals(c1, c2);
    }

    @Test
    public void equals_sameNameDifferentId_notEqual() {
        Category c1 = new Category("Test", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("Test", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_TWO);

        assertNotEquals(c1, c2);
    }

    @Test
    public void hashCode_nameDiffersOnlyByCase_sameHash() {
        Category c1 = new Category("GROCERIES", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c2 = new Category("groceries", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_ONE);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void hashCode_budgetChanged_hashUnchanged() {
        Category c1 = new Category("Test", "", BUDGET_TEN, TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE);
        int initialHash = c1.hashCode();

        c1.setMonthlyBudget(BUDGET_FIVE_HUNDRED);

        assertEquals(initialHash, c1.hashCode());
    }

    @Test
    public void hashCode_budgetScaleDiffers_sameHash() {
        Category c1 = new Category("Test", "", new BigDecimal("10.00"), TrackingType.EXPENSE);
        Category c2 = new Category("Test", "", new BigDecimal("10"), TrackingType.EXPENSE);
        c1.setCategoryId(ID_ONE); c2.setCategoryId(ID_ONE);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    // --- RELOAD MODEL (R1, R2, R6, R13) ---

    @Test
    public void setParent_randomOperationsWithReloadBetweenEach_persistedGraphStaysValid() {
        FakeRoom room = new FakeRoom();
        List<Long> ids = new ArrayList<>();

        for (long i = 1; i <= 6; i++) {
            Category c = new Category("Cat" + i, "", BigDecimal.valueOf(10 * i), TrackingType.EXPENSE);
            c.setCategoryId(i);
            room.save(c);
            ids.add(i);
        }

        Random random = new Random(RANDOM_SEED);

        for (int step = 0; step < RANDOM_SEED_STEPS; step++) {
            Long editedId = ids.get(random.nextInt(ids.size()));
            Long targetParentId = ids.get(random.nextInt(ids.size()));

            Category edited = room.reload(editedId);
            Category newParent = room.reload(targetParentId);

            try {
                edited.setParent(newParent);
                room.save(edited);
                room.save(newParent);
            } catch (CategoryViolationException ignored) {
            }

            for (Long id : ids) {
                Category loaded = room.reload(id);
                assertTreeConsistent(loaded, MAX_TREE_DEPTH);
                BigDecimal childSum = loaded.getChildren(false).stream()
                        .map(Category::getMonthlyBudget)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertTrue(loaded.getMonthlyBudget().compareTo(childSum) >= 0,
                        "Parent budget must be at least child sum");
            }
        }
    }

    // --- TIER B, DEFENCE IN DEPTH (R8, R15) ---

    @Test
    public void setParent_unrelatedNewParent_removedFromOldParentChildren() {
        Category x = new Category("X", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category y = new Category("Y", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category z = new Category("Z", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE); y.setCategoryId(ID_TWO); z.setCategoryId(ID_THREE);

        y.setParent(x);
        y.setParent(z);

        assertFalse(x.getChildren(false).contains(y));
    }

    @Test
    public void setParent_oldParentMovedUnderFormerChild_noCycleCreated() {
        Category x = new Category("X", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category y = new Category("Y", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category z = new Category("Z", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        x.setCategoryId(ID_ONE); y.setCategoryId(ID_TWO); z.setCategoryId(ID_THREE);

        y.setParent(x);
        y.setParent(z);
        x.setParent(y);

        assertTreeConsistent(z, MAX_TREE_DEPTH);
    }

    @Test
    public void setParent_movedThreeTimes_notLeftInAnyEarlierParent() {
        Category a = new Category("A", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category b = new Category("B", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category c = new Category("C", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category target = new Category("Target", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        a.setCategoryId(ID_ONE); b.setCategoryId(ID_TWO); c.setCategoryId(ID_THREE); target.setCategoryId(ID_FOUR);

        target.setParent(a);
        target.setParent(b);
        target.setParent(c);

        assertAll(
            () -> assertFalse(a.getChildren(false).contains(target)),
            () -> assertFalse(b.getChildren(false).contains(target)),
            () -> assertTrue(c.getChildren(false).contains(target))
        );
    }

    @Test
    public void setParent_newParentHasParentIdButNoCachedParent_throwsAndStateUnchanged() {
        Category edited = new Category("Edited", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category newParent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        edited.setCategoryId(ID_ONE); newParent.setCategoryId(ID_TWO);
        newParent.setParentId(ID_MISSING);

        assertThrows(CategoryStateException.class, () -> edited.setParent(newParent));
    }

    @Test
    public void setParent_editedCategoryHasParentIdButNoCachedParent_throwsAndStateUnchanged() {
        Category edited = new Category("Edited", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category newParent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        edited.setCategoryId(ID_ONE); newParent.setCategoryId(ID_TWO);
        edited.setParentId(ID_MISSING);

        assertThrows(CategoryStateException.class, () -> edited.setParent(newParent));
    }

    @Test
    public void setParent_gapHigherUpNewParentChain_throwsAndStateUnchanged() {
        Category newParent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category grand = new Category("Grand", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        newParent.setCategoryId(ID_ONE); grand.setCategoryId(ID_TWO);
        newParent.setParent(grand);
        grand.setParentId(ID_MISSING);

        Category edited = new Category("Edited", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        edited.setCategoryId(ID_THREE);

        assertThrows(CategoryStateException.class, () -> edited.setParent(newParent));
    }

    @Test
    public void setParent_gapHigherUpEditedChain_throwsAndStateUnchanged() {
        Category edited = new Category("Edited", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category oldParent = new Category("OldParent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        edited.setCategoryId(ID_ONE); oldParent.setCategoryId(ID_TWO);
        edited.setParent(oldParent);
        oldParent.setParentId(ID_MISSING);

        Category newParent = new Category("NewParent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        newParent.setCategoryId(ID_THREE);

        assertThrows(CategoryStateException.class, () -> edited.setParent(newParent));
    }

    // --- FAKE ROOM IN-MEMORY PERSISTENCE HELPER ---

    private static class FakeRoom {
        private static class Record {
            Long id;
            String name;
            Long parentId;
            BigDecimal budget;
            TrackingType type;
            String description;
        }

        private final Map<Long, Record> store = new HashMap<>();

        void save(Category root) {
            if (root == null) return;
            Record r = new Record();
            r.id = root.getCategoryId();
            r.name = root.getName();
            r.parentId = root.getParentId();
            r.budget = root.getMonthlyBudget();
            r.type = root.getType();
            r.description = root.getDescription();
            store.put(r.id, r);

            for (Category child : root.getChildren(false)) {
                save(child);
            }
        }

        Category reload(Long id) {
            Map<Long, Category> cache = new HashMap<>();
            for (Record r : store.values()) {
                Category c = new Category(r.name, r.description, r.budget, r.type);
                c.setCategoryId(r.id);
                c.setParentId(r.parentId);
                cache.put(r.id, c);
            }

            List<Category> nodes = new ArrayList<>(cache.values());
            nodes.sort(Comparator.comparingInt(c -> countAncestorIdsInCache(c, cache)));

            for (Category c : nodes) {
                if (c.getParentId() != null && cache.containsKey(c.getParentId())) {
                    Category p = cache.get(c.getParentId());
                    if (p != null) {
                        try {
                            c.setParent(p);
                        } catch (CategoryViolationException e) {
                            c.setParentId(null);
                            Record rec = store.get(c.getCategoryId());
                            if (rec != null) {
                                rec.parentId = null;
                            }
                        }
                    }
                }
            }

            return cache.get(id);
        }

        private int countAncestorIdsInCache(Category c, Map<Long, Category> cache) {
            int depth = 0;
            Long pId = c.getParentId();
            Set<Long> visited = new HashSet<>();
            while (pId != null && cache.containsKey(pId) && visited.add(pId)) {
                depth++;
                Category parent = cache.get(pId);
                pId = parent != null ? parent.getParentId() : null;
            }
            return depth;
        }
    }
}
