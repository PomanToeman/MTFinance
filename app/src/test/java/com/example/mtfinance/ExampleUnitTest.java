package com.example.mtfinance;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.example.mtfinance.src.Category;
import com.example.mtfinance.src.Transaction;

import java.math.BigDecimal;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

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
    }


    @Test
    public void testing_total_with_sub_of_sub() {
        Category subscriptions = new Category("Subscriptions", "", BigDecimal.ZERO);
        Category streaming = new Category("Streaming", "", BigDecimal.ZERO);
        Category netflix = new Category("Netflix", "", BigDecimal.ZERO);

        streaming.setParent(subscriptions);
        netflix.setParent(streaming);

        Transaction t1 = new Transaction.Builder("Netflix", BigDecimal.valueOf(15.99)).build();
        Transaction t2 = new Transaction.Builder("Disney+", BigDecimal.valueOf(13.99)).build();
        Transaction t3 = new Transaction.Builder("Prime", BigDecimal.valueOf(8.99)).build();

        netflix.addTransaction(t1);
        streaming.addTransaction(t2);
        subscriptions.addTransaction(t3);

        // Should include all levels when includeSub = true
        assertEquals(BigDecimal.valueOf(38.97), subscriptions.findTotal(true));

        // Only direct transactions when false
        assertEquals(BigDecimal.valueOf(8.99), subscriptions.findTotal(false));
    }

    @Test
    public void testing_total_emptyCategory() {
        Category empty = new Category("Empty", "", BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, empty.findTotal(false));
        assertEquals(BigDecimal.ZERO, empty.findTotal(true));
    }

    @Test
    public void testing_total_with_only_subcategories() {
        Category main = new Category("Main", "", BigDecimal.ZERO);
        Category sub = new Category("Sub", "", BigDecimal.ZERO);
        sub.setParent(main);

        Transaction t = new Transaction.Builder("Only in sub", BigDecimal.valueOf(25.50)).build();
        sub.addTransaction(t);

        assertEquals(BigDecimal.ZERO, main.findTotal(false));
        assertEquals(BigDecimal.valueOf(25.50), main.findTotal(true));
    }

    @Test
    public void testing_total_with_negative_and_zero_transactions() {
        Category cat = new Category("Test", "", BigDecimal.ZERO);

        Transaction positive = new Transaction.Builder("Pos", BigDecimal.valueOf(10.00)).build();
        Transaction negative = new Transaction.Builder("Refund", BigDecimal.valueOf(-3.50)).build();
        Transaction zero = new Transaction.Builder("Free", BigDecimal.ZERO).build();

        cat.addTransaction(positive);
        cat.addTransaction(negative);
        cat.addTransaction(zero);

        assertEquals(BigDecimal.valueOf(6.50), cat.findTotal(false));
    }

    @Test
    public void testing_total_deep_nesting() {
        Category root = new Category("Root", "", BigDecimal.ZERO);
        Category level1 = new Category("L1", "", BigDecimal.ZERO);
        Category level2 = new Category("L2", "", BigDecimal.ZERO);
        Category level3 = new Category("L3", "", BigDecimal.ZERO);

        level1.setParent(root);
        level2.setParent(level1);
        level3.setParent(level2);

        level3.addTransaction(new Transaction.Builder("Deep", BigDecimal.valueOf(100)).build());

        assertEquals(BigDecimal.valueOf(100), root.findTotal(true));
        assertEquals(BigDecimal.ZERO, root.findTotal(false));
    }



}