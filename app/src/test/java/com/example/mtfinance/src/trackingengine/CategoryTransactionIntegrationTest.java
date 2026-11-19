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

    private static final Long CATEGORY_ID = 1L;
    private static final BigDecimal CATEGORY_BUDGET = BigDecimal.valueOf(200.0);
    private static final BigDecimal MILK_AMOUNT = BigDecimal.valueOf(4.50);
    private static final BigDecimal BREAD_AMOUNT = BigDecimal.valueOf(3.20);
    private static final BigDecimal EXPECTED_TOTAL_AMOUNT = BigDecimal.valueOf(7.70);
    private static final int EXPECTED_TRANSACTION_COUNT = 2;

    @Test
    public void categoryWithTransactions_addsTransactions_calculatesTotalAmountCorrectly() {
        Category category = new Category("Groceries", "Food expenses", CATEGORY_BUDGET, TrackingType.EXPENSE);
        category.setCategoryId(CATEGORY_ID);

        Transaction t1 = new Transaction.Builder("Milk", MILK_AMOUNT).build();
        Transaction t2 = new Transaction.Builder("Bread", BREAD_AMOUNT).build();

        CategoryWithTransactions cwt = new CategoryWithTransactions();
        cwt.category = category;
        cwt.transactions.add(t1);
        cwt.transactions.add(t2);

        assertAll(
            () -> assertNotNull(cwt.category),
            () -> assertEquals(EXPECTED_TRANSACTION_COUNT, cwt.transactions.size()),
            () -> assertEquals(0, EXPECTED_TOTAL_AMOUNT.compareTo(cwt.findTotal()))
        );
    }
}
