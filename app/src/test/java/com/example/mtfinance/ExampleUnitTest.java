package com.example.mtfinance;


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
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
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
    public void testing_totalTracking_without_sub() {
        Category categorytest = new Category("Subcriptions", "Test 3", BigDecimal.valueOf(30));

        Transaction transactionOne = new Transaction.Builder("Netflix", BigDecimal.valueOf(4.99)).build();
        Transaction transactionTwo = new Transaction.Builder("Evil Sub", BigDecimal.valueOf(9.95)).build();
        Transaction transactionThree = new Transaction.Builder("Youtube Premium", BigDecimal.valueOf(7.49)).build();

        categorytest.addTransaction(transactionOne);
        categorytest.addTransaction(transactionTwo);
        categorytest.addTransaction(transactionThree);

        assertEquals(BigDecimal.valueOf(22.43), categorytest.findTotal(true));

    }



}