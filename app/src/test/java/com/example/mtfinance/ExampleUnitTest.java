package com.example.mtfinance;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.example.mtfinance.src.Category;
import com.example.mtfinance.src.TrackingUtlis;
import com.example.mtfinance.src.Transaction;

import java.math.BigDecimal;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    public static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(0.1);

    @Before
    public void before() {
    }

    // ========================================
    // TRANSACTION TESTS
    // ========================================

    @Test
    public void test_transaction() {
        Transaction transaction = new Transaction.Builder("Lollies", BigDecimal.valueOf(1.0)).build();
        assertEquals(BigDecimal.valueOf(1.0), transaction.getAmount());
    }

    @Test
    public void testing_checkAmount_negative_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> TrackingUtlis.checkAmount(BigDecimal.valueOf(-5.99)));

        assertThrows(IllegalArgumentException.class,
                () -> TrackingUtlis.checkAmount(BigDecimal.valueOf(-0.01)));

        assertThrows(IllegalArgumentException.class,
                () -> TrackingUtlis.checkAmount(BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class,
                () -> new Transaction.Builder("Negative transaction", BigDecimal.valueOf(-88)).build());
    }

    // ========================================
    // CATEGORY HIERARCHY TESTS
    // ========================================

    @Test
    public void testing_categories_hierarchy() {
        Category categoryOne = new Category("Subscriptions", "Test 3", DEFAULT_BUDGET);
        Category categoryTwo = new Category("Netflix", "Sub category of subscriptions", DEFAULT_BUDGET);

        categoryTwo.setParent(categoryOne);
        categoryOne.setParent(categoryTwo); // cycle prevention check

        assertEquals(categoryOne, categoryTwo.getParent());
        assertNotEquals(categoryTwo, categoryOne.getParent());
        assertFalse(categoryOne.isDescendantOf(categoryTwo));
        assertTrue(categoryTwo.isDescendantOf(categoryOne));
    }

    @Test
    public void testing_categories_hierarchy_basic() {
        Category parent = new Category("Subscriptions", "Main category", DEFAULT_BUDGET);
        Category child = new Category("Netflix", "Streaming sub", DEFAULT_BUDGET);

        child.setParent(parent);

        assertEquals(parent, child.getParent());
        assertTrue(child.isDescendantOf(parent));
        assertFalse(parent.isDescendantOf(child));
    }

    @Test
    public void testing_categories_setParent_preventsCycle() {
        Category sub = new Category("Subscriptions", "", DEFAULT_BUDGET);
        Category netflix = new Category("Netflix", "", DEFAULT_BUDGET);

        netflix.setParent(sub);
        sub.setParent(netflix); // This should NOT create a cycle

        assertEquals(sub, netflix.getParent());
        assertNull(sub.getParent());
        assertTrue(netflix.isDescendantOf(sub));
        assertFalse(sub.isDescendantOf(netflix));
    }

    @Test
    public void testing_categories_deep_hierarchy() {
        Category root = new Category("Root", "", DEFAULT_BUDGET);
        Category level1 = new Category("Level 1", "", DEFAULT_BUDGET);
        Category level2 = new Category("Level 2", "", DEFAULT_BUDGET);
        Category level3 = new Category("Level 3", "", DEFAULT_BUDGET);

        level1.setParent(root);
        level2.setParent(level1);
        level3.setParent(level2);

        assertTrue(level3.isDescendantOf(root));
        assertTrue(level3.isDescendantOf(level1));
        assertTrue(level2.isDescendantOf(root));
        assertFalse(root.isDescendantOf(level3));
    }

    @Test
    public void testing_categories_multiple_children() {
        Category subscriptions = new Category("Subscriptions", "", DEFAULT_BUDGET);
        Category netflix = new Category("Netflix", "", DEFAULT_BUDGET);
        Category spotify = new Category("Spotify", "", DEFAULT_BUDGET);
        Category youtube = new Category("YouTube", "", DEFAULT_BUDGET);

        netflix.setParent(subscriptions);
        spotify.setParent(subscriptions);
        youtube.setParent(subscriptions);

        assertEquals(subscriptions, netflix.getParent());
        assertEquals(subscriptions, spotify.getParent());
        assertEquals(subscriptions, youtube.getParent());

        assertEquals(3, subscriptions.getChildren(true).size());
    }

    @Test
    public void testing_categories_changeParent() {
        Category oldParent = new Category("Old Parent", "", DEFAULT_BUDGET);
        Category newParent = new Category("New Parent", "", DEFAULT_BUDGET);
        Category child = new Category("Child", "", DEFAULT_BUDGET);

        child.setParent(oldParent);
        child.setParent(newParent);

        assertEquals(newParent, child.getParent());
        assertFalse(child.isDescendantOf(oldParent));
        assertTrue(child.isDescendantOf(newParent));
    }

    @Test
    public void testing_categories_noParent() {
        Category orphan = new Category("Orphan", "", DEFAULT_BUDGET);

        assertNull(orphan.getParent());
        assertFalse(orphan.isDescendantOf(new Category("Anything", "", DEFAULT_BUDGET)));
    }

    @Test
    public void testing_categories_selfParent_prevention() {
        Category cat = new Category("Self", "", DEFAULT_BUDGET);
        cat.setParent(cat);

        assertNull(cat.getParent());
    }

    // ========================================
    // MIXED: TRANSACTIONS + CATEGORIES
    // ========================================

    @Test
    public void testing_total_with_sub_of_sub() {
        Category subscriptions = new Category("Subscriptions", "", DEFAULT_BUDGET);
        Category streaming = new Category("Streaming", "", DEFAULT_BUDGET);
        Category netflix = new Category("Netflix", "", DEFAULT_BUDGET);

        streaming.setParent(subscriptions);
        netflix.setParent(streaming);

        Transaction t1 = new Transaction.Builder("Netflix", BigDecimal.valueOf(15.99)).build();
        Transaction t2 = new Transaction.Builder("Disney+", BigDecimal.valueOf(13.99)).build();
        Transaction t3 = new Transaction.Builder("Prime", BigDecimal.valueOf(8.99)).build();

        netflix.addTransaction(t1);
        streaming.addTransaction(t2);
        subscriptions.addTransaction(t3);

        assertEquals(BigDecimal.valueOf(38.97), subscriptions.findTotal(true));
        assertEquals(BigDecimal.valueOf(8.99), subscriptions.findTotal(false));
    }

    @Test
    public void testing_total_emptyCategory() {
        Category empty = new Category("Empty", "", DEFAULT_BUDGET);
        assertEquals(BigDecimal.ZERO, empty.findTotal(false));
        assertEquals(BigDecimal.ZERO, empty.findTotal(true));
    }

    @Test
    public void testing_total_with_only_subcategories() {
        Category main = new Category("Main", "", DEFAULT_BUDGET);
        Category sub = new Category("Sub", "", DEFAULT_BUDGET);
        sub.setParent(main);

        Transaction t = new Transaction.Builder("Only in sub", BigDecimal.valueOf(25.50)).build();
        sub.addTransaction(t);

        assertEquals(BigDecimal.ZERO, main.findTotal(false));
        assertEquals(BigDecimal.valueOf(25.50), main.findTotal(true));
    }

    @Test
    public void testing_total_deep_nesting() {
        Category root = new Category("Root", "", DEFAULT_BUDGET);
        Category level1 = new Category("L1", "", DEFAULT_BUDGET);
        Category level2 = new Category("L2", "", DEFAULT_BUDGET);
        Category level3 = new Category("L3", "", DEFAULT_BUDGET);

        level1.setParent(root);
        level2.setParent(level1);
        level3.setParent(level2);

        level3.addTransaction(new Transaction.Builder("Deep", BigDecimal.valueOf(100)).build());

        assertEquals(BigDecimal.valueOf(100), root.findTotal(true));
        assertEquals(BigDecimal.ZERO, root.findTotal(false));
    }

    // ========================================
    // BUDGET TESTS
    // ========================================

    @Test
    public void testing_minimum_budget_handling() {
        Category root = new Category("Root", "", DEFAULT_BUDGET);
        Category level1 = new Category("L1", "", BigDecimal.ONE);
        level1.setParent(root);

        assertEquals(BigDecimal.ONE, root.getBudget());
        assertEquals(BigDecimal.ONE, root.determineMinimumBudget());
    }

    @Test
    public void testing_minimum_budget_with_multiple_children() {
        Category subscriptions = new Category("Subscriptions", "", DEFAULT_BUDGET);
        Category netflix = new Category("Netflix", "", BigDecimal.valueOf(15.99));
        Category spotify = new Category("Spotify", "", BigDecimal.valueOf(10.99));
        Category youtube = new Category("YouTube", "", BigDecimal.valueOf(13.99));

        netflix.setParent(subscriptions);
        spotify.setParent(subscriptions);
        youtube.setParent(subscriptions);

        BigDecimal expectedMin = BigDecimal.valueOf(40.97);

        assertEquals(expectedMin, subscriptions.getBudget());
        assertEquals(expectedMin, subscriptions.determineMinimumBudget());
    }

    @Test
    public void testing_minimum_budget_deep_hierarchy() {
        Category root = new Category("Root", "", DEFAULT_BUDGET);
        Category l1 = new Category("L1", "", BigDecimal.valueOf(5));
        Category l2 = new Category("L2", "", BigDecimal.valueOf(10));
        Category l3 = new Category("L3", "", BigDecimal.valueOf(15));

        l1.setParent(root);
        l2.setParent(l1);
        l3.setParent(l2);

        BigDecimal expected = BigDecimal.valueOf(15);

        assertEquals(expected, root.getBudget());
        assertEquals(expected, root.determineMinimumBudget());
    }

    @Test
    public void testing_setBudget_respectsMinimum() {
        Category parent = new Category("Parent", "", DEFAULT_BUDGET);
        Category child = new Category("Child", "", BigDecimal.valueOf(25.50));
        child.setParent(parent);

        parent.setBudget(BigDecimal.valueOf(10));

        assertEquals(BigDecimal.valueOf(25.50), parent.getBudget());
    }

    @Test
    public void testing_setBudget_aboveMinimum() {
        Category parent = new Category("Parent", "", DEFAULT_BUDGET);
        Category child = new Category("Child", "", BigDecimal.valueOf(20));
        child.setParent(parent);

        parent.setBudget(BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(50), parent.getBudget());
    }

    @Test
    public void testing_minimumBudget_afterAddingNewChild() {
        Category root = new Category("Root", "", BigDecimal.valueOf(100));
        Category child1 = new Category("Child1", "", BigDecimal.valueOf(30));
        child1.setParent(root);

        Category child2 = new Category("Child2", "", BigDecimal.valueOf(80));
        child2.setParent(root);

        assertEquals(BigDecimal.valueOf(110), root.getBudget());
    }

    @Test
    public void testing_minimumBudget_noChildren() {
        Category alone = new Category("Alone", "", DEFAULT_BUDGET);

        assertEquals(DEFAULT_BUDGET, alone.getBudget());
        assertEquals(BigDecimal.ZERO, alone.determineMinimumBudget());
    }

    // ========================================
    // GET DETAILS TESTS
    // ========================================

    @Test
    public void testing_getDetails_noChildren() {
        Category category = new Category("Netflix", "Streaming subscription", DEFAULT_BUDGET);

        String details = category.getDetails();

        assertTrue(details.contains("Name: Netflix"));
        assertTrue(details.contains("Description: Streaming subscription"));
        assertTrue(details.contains("Sub Categories: []"));
    }

    @Test
    public void testing_getDetails_withChildren() {
        Category parent = new Category("Subscriptions", "All subs", DEFAULT_BUDGET);
        Category netflix = new Category("Netflix", "Video", DEFAULT_BUDGET);
        Category spotify = new Category("Spotify", "Music", DEFAULT_BUDGET);

        netflix.setParent(parent);
        spotify.setParent(parent);

        String details = parent.getDetails();

        assertTrue(details.contains("Name: Subscriptions"));
        assertTrue(details.contains("Description: All subs"));
        assertTrue(details.contains("Sub Categories:"));
        assertTrue(details.contains("Netflix") || details.contains("Spotify"));
    }

    @Test
    public void testing_getDetails_emptyDescription() {
        Category cat = new Category("EmptyDesc", "", DEFAULT_BUDGET);

        String details = cat.getDetails();

        assertTrue(details.contains("Name: EmptyDesc"));
        assertTrue(details.contains("Description: "));
        assertTrue(details.contains("Sub Categories: []"));
    }

    @Test
    public void testing_getDetails_nullSafety() {
        Category cat = new Category("Test", null, DEFAULT_BUDGET);

        String details = cat.getDetails();
        assertNotNull(details);
        assertTrue(details.contains("Name: Test"));
    }


    @Test
    public void testing_category_budget_validation_rejectsInvalidAmounts() {
        // Test 1: Creating category with negative budget
        assertThrows(IllegalArgumentException.class, () -> {
            new Category("Invalid Negative", "Test", BigDecimal.valueOf(-10.50));
        });

        // Test 2: Creating category with zero budget
        assertThrows(IllegalArgumentException.class, () -> {
            new Category("Invalid Zero", "Test", BigDecimal.ZERO);
        });

        // Test 3: Setting negative budget on existing category
        Category validCategory = new Category("Valid Category", "Test", DEFAULT_BUDGET);
        assertThrows(IllegalArgumentException.class, () -> {
            validCategory.setBudget(BigDecimal.valueOf(-5.00));
        });

        // Test 4: Setting zero budget on existing category
        assertThrows(IllegalArgumentException.class, () -> {
            validCategory.setBudget(BigDecimal.ZERO);
        });

        // Test 5: Creating child with invalid budget should also fail
        assertThrows(IllegalArgumentException.class, () -> {
            Category parent = new Category("Parent", "", DEFAULT_BUDGET);
            Category invalidChild = new Category("Bad Child", "", BigDecimal.valueOf(-1));
            invalidChild.setParent(parent);
        });

        // Verify valid category still works normally
        Category goodCategory = new Category("Good Category", "Test", DEFAULT_BUDGET);
        assertEquals(DEFAULT_BUDGET, goodCategory.getBudget());
    }
}