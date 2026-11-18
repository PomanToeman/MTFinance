package com.example.mtfinance.src.trackingengine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Integration tests for real Category and Transaction object interactions, cross-references, and total calculations.
 */
public class CategoryTransactionIntegrationTest {

    @Test
    public void categoryWithTransactions_addsTransactions_calculatesTotalAmountCorrectly() {
        Category category = new Category("Groceries", "Food expenses", BigDecimal.valueOf(200.0), TrackingType.EXPENSE);
        category.setCategoryId(1L);

        Transaction t1 = new Transaction.Builder("Milk", BigDecimal.valueOf(4.50)).build();
        Transaction t2 = new Transaction.Builder("Bread", BigDecimal.valueOf(3.20)).build();

        CategoryWithTransactions cwt = new CategoryWithTransactions();
        cwt.category = category;
        cwt.transactions.add(t1);
        cwt.transactions.add(t2);

        BigDecimal expectedTotal = BigDecimal.valueOf(7.70);

        assertAll(
            () -> assertNotNull(cwt.category),
            () -> assertEquals(2, cwt.transactions.size()),
            () -> assertEquals(0, expectedTotal.compareTo(cwt.findTotal()))
        );
    }
}
