package com.example.mtfinance;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

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
        Category categoryOne = new Category("Subscriptions", "Test 3", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category categoryTwo = new Category("Netflix", "Sub category of subscriptions", DEFAULT_BUDGET, TrackingType.EXPENSE);

        categoryTwo.setParent(categoryOne);
        categoryOne.setParent(categoryTwo); // cycle prevention check

        assertEquals(categoryOne, categoryTwo.getParent());
        assertNotEquals(categoryTwo, categoryOne.getParent());
        assertFalse(categoryOne.isDescendantOf(categoryTwo));
        assertTrue(categoryTwo.isDescendantOf(categoryOne));
    }

    @Test
    public void testing_categories_hierarchy_basic() {
        Category parent = new Category("Subscriptions", "Main category", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Netflix", "Streaming sub", DEFAULT_BUDGET, TrackingType.EXPENSE);

        child.setParent(parent);

        assertEquals(parent, child.getParent());
        assertTrue(child.isDescendantOf(parent));
        assertFalse(parent.isDescendantOf(child));
    }

    @Test
    public void testing_categories_setParent_preventsCycle() {
        Category sub = new Category("Subscriptions", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category netflix = new Category("Netflix", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        netflix.setParent(sub);
        sub.setParent(netflix); // This should NOT create a cycle

        assertEquals(sub, netflix.getParent());
        assertNull(sub.getParent());
        assertTrue(netflix.isDescendantOf(sub));
        assertFalse(sub.isDescendantOf(netflix));
    }

    @Test
    public void testing_categories_deep_hierarchy() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level1 = new Category("Level 1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level2 = new Category("Level 2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level3 = new Category("Level 3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

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
        Category subscriptions = new Category("Subscriptions", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category netflix = new Category("Netflix", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category spotify = new Category("Spotify", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category youtube = new Category("YouTube", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

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
        Category oldParent = new Category("Old Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category newParent = new Category("New Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        child.setParent(oldParent);
        child.setParent(newParent);

        assertEquals(newParent, child.getParent());
        assertFalse(child.isDescendantOf(oldParent));
        assertTrue(child.isDescendantOf(newParent));
    }

    @Test
    public void testing_categories_noParent() {
        Category orphan = new Category("Orphan", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        assertNull(orphan.getParent());
        assertFalse(orphan.isDescendantOf(new Category("Anything", "", DEFAULT_BUDGET, TrackingType.EXPENSE)));
    }

    @Test
    public void testing_categories_selfParent_prevention() {
        Category cat = new Category("Self", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        cat.setParent(cat);

        assertNull(cat.getParent());
    }

    // ========================================
    // BUDGET TESTS
    // ========================================

    @Test
    public void testing_minimum_budget_handling() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level1 = new Category("L1", "", BigDecimal.ONE, TrackingType.EXPENSE);
        level1.setParent(root);

        assertEquals(BigDecimal.ONE, root.getMonthlyBudget());
        assertEquals(BigDecimal.ONE, root.determineMinimumBudget());
    }

    @Test
    public void testing_minimum_budget_with_multiple_children() {
        Category subscriptions = new Category("Subscriptions", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category netflix = new Category("Netflix", "", BigDecimal.valueOf(15.99), TrackingType.EXPENSE);
        Category spotify = new Category("Spotify", "", BigDecimal.valueOf(10.99), TrackingType.EXPENSE);
        Category youtube = new Category("YouTube", "", BigDecimal.valueOf(13.99), TrackingType.EXPENSE);

        netflix.setParent(subscriptions);
        spotify.setParent(subscriptions);
        youtube.setParent(subscriptions);

        BigDecimal expectedMin = BigDecimal.valueOf(40.97);

        assertEquals(expectedMin, subscriptions.getMonthlyBudget());
        assertEquals(expectedMin, subscriptions.determineMinimumBudget());
    }

    @Test
    public void testing_minimum_budget_deep_hierarchy() {
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category l1 = new Category("L1", "", BigDecimal.valueOf(5), TrackingType.EXPENSE);
        Category l2 = new Category("L2", "", BigDecimal.valueOf(10), TrackingType.EXPENSE);
        Category l3 = new Category("L3", "", BigDecimal.valueOf(15), TrackingType.EXPENSE);

        l1.setParent(root);
        l2.setParent(l1);
        l3.setParent(l2);

        BigDecimal expected = BigDecimal.valueOf(15);

        assertEquals(expected, root.getMonthlyBudget());
        assertEquals(expected, root.determineMinimumBudget());
    }

    @Test
    public void testing_setBudget_respectsMinimum() {
        Category parent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BigDecimal.valueOf(25.50), TrackingType.EXPENSE);
        child.setParent(parent);

        parent.setMonthlyBudget(BigDecimal.valueOf(10));

        assertEquals(BigDecimal.valueOf(25.50), parent.getMonthlyBudget());
    }

    @Test
    public void testing_setBudget_aboveMinimum() {
        Category parent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BigDecimal.valueOf(20), TrackingType.EXPENSE);
        child.setParent(parent);

        parent.setMonthlyBudget(BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(50), parent.getMonthlyBudget());
    }

    @Test
    public void testing_minimumBudget_afterAddingNewChild() {
        Category root = new Category("Root", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category child1 = new Category("Child1", "", BigDecimal.valueOf(30), TrackingType.EXPENSE);
        child1.setParent(root);

        Category child2 = new Category("Child2", "", BigDecimal.valueOf(80), TrackingType.EXPENSE);
        child2.setParent(root);

        assertEquals(BigDecimal.valueOf(110), root.getMonthlyBudget());
    }

    @Test
    public void testing_minimumBudget_noChildren() {
        Category alone = new Category("Alone", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        assertEquals(DEFAULT_BUDGET, alone.getMonthlyBudget());
        assertEquals(BigDecimal.ZERO, alone.determineMinimumBudget());
    }

    // ========================================
    // GET DETAILS TESTS
    // ========================================

    @Test
    public void testing_getDetails_noChildren() {
        Category category = new Category("Netflix", "Streaming subscription", DEFAULT_BUDGET, TrackingType.EXPENSE);

        String details = category.getDetails();

        assertTrue(details.contains("Name: Netflix"));
        assertTrue(details.contains("Description: Streaming subscription"));
        assertTrue(details.contains("Sub Categories: []"));
    }

    @Test
    public void testing_getDetails_withChildren() {
        Category parent = new Category("Subscriptions", "All subs", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category netflix = new Category("Netflix", "Video", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category spotify = new Category("Spotify", "Music", DEFAULT_BUDGET, TrackingType.EXPENSE);

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
        Category cat = new Category("EmptyDesc", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        String details = cat.getDetails();

        assertTrue(details.contains("Name: EmptyDesc"));
        assertTrue(details.contains("Description: "));
        assertTrue(details.contains("Sub Categories: []"));
    }

    @Test
    public void testing_getDetails_nullSafety() {
        Category cat = new Category("Test", null, DEFAULT_BUDGET, TrackingType.EXPENSE);

        String details = cat.getDetails();
        assertNotNull(details);
        assertTrue(details.contains("Name: Test"));
    }


    @Test
    public void testing_category_budget_validation_rejectsInvalidAmounts() {
        // Test 1: Creating category with negative budget
        assertThrows(IllegalArgumentException.class, () -> {
            new Category("Invalid Negative", "Test", BigDecimal.valueOf(-10.50), TrackingType.EXPENSE);
        });

        // Test 2: Creating category with zero budget
        assertThrows(IllegalArgumentException.class, () -> {
            new Category("Invalid Zero", "Test", BigDecimal.ZERO, TrackingType.EXPENSE);
        });

        // Test 3: Setting negative budget on existing category
        Category validCategory = new Category("Valid Category", "Test", DEFAULT_BUDGET, TrackingType.EXPENSE);
        assertThrows(IllegalArgumentException.class, () -> {
            validCategory.setMonthlyBudget(BigDecimal.valueOf(-5.00));
        });

        // Test 4: Setting zero budget on existing category
        assertThrows(IllegalArgumentException.class, () -> {
            validCategory.setMonthlyBudget(BigDecimal.ZERO);
        });

        // Test 5: Creating child with invalid budget should also fail
        assertThrows(IllegalArgumentException.class, () -> {
            Category parent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
            Category invalidChild = new Category("Bad Child", "", BigDecimal.valueOf(-1), TrackingType.EXPENSE);
            invalidChild.setParent(parent);
        });

        // Verify valid category still works normally
        Category goodCategory = new Category("Good Category", "Test", DEFAULT_BUDGET, TrackingType.EXPENSE);
        assertEquals(DEFAULT_BUDGET, goodCategory.getMonthlyBudget());
    }

    // ========================================
    // NULL SAFETY TESTS
    // ========================================

    @Test
    public void testing_category_null_constructor_parameters() {
        // Implementation currently doesn't throw NPE, it just assigns null (or default via TrackingUtlis)
        Category catNameNull = new Category(null, "desc", DEFAULT_BUDGET, TrackingType.EXPENSE);
        assertNull(catNameNull.getName());

        assertThrows(IllegalArgumentException.class, () -> new Category("name", "desc", null, TrackingType.EXPENSE));

        // description can be null as it's passed to TrackingUtlis.determineDescription
        Category catDescNull = new Category("name", null, DEFAULT_BUDGET, TrackingType.EXPENSE);
        assertEquals(TrackingUtlis.EMPTY_DESCRIPTION, catDescNull.getDescription());
    }

    @Test
    public void testing_category_setBudget_null() {
        Category cat = new Category("name", "desc", DEFAULT_BUDGET, TrackingType.EXPENSE);
        // setBudget calls TrackingUtlis.checkAmount(budget) which doesn't check for null specifically before use
        assertThrows(Exception.class, () -> cat.setMonthlyBudget(null));
    }

    @Test
    public void testing_category_isDescendantOf_null() {
        Category cat = new Category("name", "desc", DEFAULT_BUDGET, TrackingType.EXPENSE);
        // Current implementation: if (this.getParent() == null || category.equals(this))
        // category.equals(this) will be false if category is null.
        // then category.equals(this.getParent()) will be false if getParent is null.
        // So it should return false.
        assertFalse(cat.isDescendantOf(null));
    }

    @Test
    public void testing_transaction_builder_null_parameters() {
        // Builder doesn't check for null name, but checkAmount might NPE on null amount
        Transaction tNameNull = new Transaction.Builder(null, BigDecimal.ONE).build();
        assertNull(tNameNull.getName());

        assertThrows(IllegalArgumentException.class, () -> new Transaction.Builder("name", null));

        Transaction.Builder builder = new Transaction.Builder("name", BigDecimal.ONE);


        // description handles null in constructor via TrackingUtlis
        Transaction tDescNull = builder.description(null).build();
        assertEquals(TrackingUtlis.EMPTY_DESCRIPTION, tDescNull.getDescription());
    }
    
    
    
    // ========================================
    // CONGRUENCY / DELETION TESTS
    // ========================================

    @Test
    public void testing_makeChildrenCongruent_basic() {
        Category grandparent = new Category("Grandparent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category parent = new Category("Parent", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        parent.setParent(grandparent);
        child.setParent(parent);

        // Pre-condition
        assertEquals(parent, child.getParent());
        assertTrue(parent.getChildren(false).contains(child));

        parent.makeChildrenCongruent();

        // Post-condition: child's parent should now be grandparent
        assertEquals(grandparent, child.getParent());
        assertTrue(grandparent.getChildren(false).contains(child));
        assertFalse(parent.getChildren(false).contains(child));
    }

    @Test
    public void testing_deleteCategory_logic() {
        // Since CategoryRepository.deleteCategory calls makeChildrenCongruent,
        // we can test the logic flow here.
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category toDelete = new Category("ToDelete", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Child", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        toDelete.setParent(root);
        child.setParent(toDelete);

        // Simulate Repository.deleteCategory logic
        toDelete.makeChildrenCongruent();
        // (In repository, categoryDao.delete(toDelete) would follow)

        assertEquals(root, child.getParent());
        assertTrue(root.getChildren(false).contains(child));
    }
}
