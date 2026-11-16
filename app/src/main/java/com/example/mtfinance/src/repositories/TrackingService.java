package com.example.mtfinance.src.repositories;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TrackingService {
    CategoryWithTransactions getCategoryWithTransactionsByCategoryId(Long id);
    List<CategoryWithTransactions> getCategoryWithTransactionsByParentId(Long parentId);

    /**
     * Live Data is to ensure these instances stay in sync with Room when one updates.
     * @return live data list of CategoryWithTransactions
     */
    LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions();

    List<CategoryWithTransactions> getCategoriesWithTransactionsByIds(Collection<Long> categoryIds);

    /**
     * Finds all categories that has a given transaction with the same ID.
     * @param transactionId - The given ID of the transaction
     * @return - returns a list of categories that has the given transaction ID.
     */
    List<Category> getCategoriesByTransactionId(Long transactionId);

    List<Long> getCategoryIdsByTransactionId(Long transactionId);
    List<Long> getTransactionIdsForCategory(Long categoryId);

    /**
     * Returns all transactions for the given category, including sub-categories if includeSub is true.
     * Duplicates are ignored.
     * Note that category must already be in the database.
     * @param category - the category to find the transactions.
     * @param includeSubs - if true, includes the transactions of all sub-categories.
     * @return - returns a set of transactions.
     */
    Set<Transaction> getTransactionsForCategory(Category category, boolean includeSubs);

    /**
     * Includes all given transactions
     * @param category - the category to find the total.
     * @param includeSubs adds the transactions' amounts of all sub-categories if true. Ignores duplicates.
     * @return returns the total amount of the category.
     */
    BigDecimal getTotalInCategory(Category category, boolean includeSubs);

    /**
     * Includes all given transactions within the given date range.
     * @param category - the category to find the total.
     * @param includeSubs adds the transactions' amounts of all sub-categories if true. Ignores duplicates.
     * @param startDate - the start date to filter by.
     * @param endDate - the end date to filter by.
     * @return returns the total amount of the category.
     */
    BigDecimal getTotalInCategory(Category category, boolean includeSubs, @Nullable LocalDate startDate, @Nullable LocalDate endDate);

    /**
     * This finds all children of the given category.
     * Note that the children totals can be different if other children have the same transactions.
     * For example, if children 1 has a transaction and children 2 has the same transaction, the transaction will only count to category's 1 total.
     * @param parentCategory - the category to find the children of.
     * @param startDate - the start date to filter by.
     * @param endDate - the end date to filter by.
     * @return - returns a map of the children and their totals.
     */
    Map<CategoryWithTransactions, BigDecimal> getChildrenTotals(CategoryWithTransactions parentCategory, @Nullable LocalDate startDate, @Nullable LocalDate endDate);

    LiveData<List<CategoryWithTransactions>> searchCategoriesWithType(String query, TrackingType type);
}
