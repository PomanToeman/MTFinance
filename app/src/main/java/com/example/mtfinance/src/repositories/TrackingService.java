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
    Category getCategoryByIdRestored(Long id);
    CategoryWithTransactions getCategoryWithTransactionsByCategoryId(Long id);
    List<CategoryWithTransactions> getCategoryWithTransactionsByParentId(Long parentId);
    LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions();
    List<CategoryWithTransactions> getCategoriesWithTransactionsByIds(Collection<Long> categoryIds);
    List<Category> getCategoriesByTransactionId(Long transactionId);
    List<Long> getCategoryIdsByTransactionId(Long transactionId);
    List<Long> getTransactionIdsForCategory(Long categoryId);
    Set<Transaction> getTransactionsForCategory(Category category, boolean includeSubs);
    BigDecimal getTotalInCategory(Category category, boolean includeSubs);
    BigDecimal getTotalInCategory(Category category, boolean includeSubs, @Nullable LocalDate startDate, @Nullable LocalDate endDate);
    Map<CategoryWithTransactions, BigDecimal> getChildrenTotals(CategoryWithTransactions parentCategory, @Nullable LocalDate startDate, @Nullable LocalDate endDate);
    LiveData<List<CategoryWithTransactions>> searchCategoriesWithType(String query, TrackingType type);
    LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type);
    LiveData<List<Transaction>> getAllTransactions();
    Transaction getTransactionById(Long id);
    boolean categoryExists(Long id);
    boolean categoryNameExists(String name);
    boolean transactionExists(Long id);
    boolean isRoot(Category category);
    boolean isRoot(Long categoryId);
}
