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

    @Test
    public void test_transaction() {
        Transaction transaction = new Transaction.Builder("Lollies", BigDecimal.valueOf(1.0)).build();
        System.out.println(transaction.toString());
        assertEquals(BigDecimal.valueOf(1.0), transaction.getAmount());
    }

    @Test
    public void testing_categories_hierarchy() {
        Category categoryOne = new Category("Subcriptions", "Test 3", BigDecimal.valueOf(20));
        Category categoryTwo = new Category("Netflix", "Sub category of subcriptions", BigDecimal.TEN);
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
        Category child = new Category("Netflix", "Streaming sub", BigDecimal.TEN);

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
        child.setParent(newParent); // re-parenting

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
        cat.setParent(cat); // should not allow self-parent

        assertNull(cat.getParent());
    }

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

    @Test
    public void testing_minimum_budget_handling() {
        Category root = new Category("Root", "", DEFAULT_BUDGET);
        Category level1 = new Category("L1", "", BigDecimal.ONE);
        level1.setParent(root);

        assertEquals(BigDecimal.ONE, root.getBudget());
        assertEquals(BigDecimal.ONE, root.determineMinimumBudget());
    }

    // ... (the rest of your budget tests updated similarly)

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
    public void testing_getDetails_noChildren() {
        Category category = new Category("Netflix", "Streaming subscription", BigDecimal.valueOf(15.99));

        String details = category.getDetails();

        assertTrue(details.contains("Name: Netflix"));
        assertTrue(details.contains("Description: Streaming subscription"));
        assertTrue(details.contains("Sub Categories: []"));
    }

    // ... (other getDetails tests remain the same)
}