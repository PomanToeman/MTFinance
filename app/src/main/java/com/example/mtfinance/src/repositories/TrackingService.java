package com.example.mtfinance.src.repositories;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Concrete service class for tracking read operations.
 */
@Singleton
public class TrackingService {
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final CategoryTransactionDao categoryWithTransactionsDao;
    private final CategoryService categoryService;

    @Inject
    public TrackingService(CategoryDao categoryDao,
                           TransactionDao transactionDao,
                           CategoryTransactionDao categoryWithTransactionsDao,
                           CategoryService categoryService) {
        this.categoryDao = categoryDao;
        this.transactionDao = transactionDao;
        this.categoryWithTransactionsDao = categoryWithTransactionsDao;
        this.categoryService = categoryService;
    }

    public CategoryWithTransactions getCategoryWithTransactionsByCategoryId(Long id) {
        return categoryWithTransactionsDao.getCategoryById(id);
    }

    public List<CategoryWithTransactions> getCategoryWithTransactionsByParentId(Long parentId) {
        return categoryWithTransactionsDao.getCategoriesWithTransactionsByParentId(parentId);
    }

    public LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions() {
        return categoryWithTransactionsDao.getAllCategoriesWithTransactions();
    }

    public List<CategoryWithTransactions> getCategoriesWithTransactionsByIds(Collection<Long> categoryIds) {
        return categoryWithTransactionsDao.getCategoriesByIds(categoryIds);
    }

    public List<Category> getCategoriesByTransactionId(Long transactionId) {
        List<Long> categoryIds = categoryWithTransactionsDao.getCategoryIdsForTransaction(transactionId);
        return categoryDao.getByIds(categoryIds);
    }

    public List<Long> getCategoryIdsByTransactionId(Long transactionId) {
        return categoryWithTransactionsDao.getCategoryIdsForTransaction(transactionId);
    }

    public List<Long> getTransactionIdsForCategory(Long categoryId) {
        return categoryWithTransactionsDao.getTransactionIdsForCategory(categoryId);
    }

    public Set<Transaction> getTransactionsForCategory(Category category, boolean includeSub) {
        CategoryWithTransactions categoryWithTransactions = categoryWithTransactionsDao.getCategoryById(category.getCategoryId());
        if (categoryWithTransactions == null) {
            return new HashSet<>();
        }
        Set<Transaction> transactions = new HashSet<>(categoryWithTransactions.transactions);
        if (includeSub) {
            for (Category child : category.getChildren(true)) {
                CategoryWithTransactions childCwt = categoryWithTransactionsDao.getCategoryById(child.getCategoryId());
                if (childCwt != null) {
                    transactions.addAll(childCwt.transactions);
                }
            }
        }
        return transactions;
    }

    public BigDecimal getTotalInCategory(Category category, boolean includeSub) {
        return getTotalInCategory(category, includeSub, LocalDate.MIN, LocalDate.MAX, new HashSet<>());
    }

    public BigDecimal getTotalInCategory(Category category, boolean includeSub, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        return getTotalInCategory(category, includeSub, startDate, endDate, new HashSet<>());
    }

    public BigDecimal getTotalInCategory(Category category, boolean includeSub, LocalDate startDate, LocalDate endDate, Set<Transaction> excludedTransactions) {
        CategoryWithTransactions parentCategoryWithTransactions = categoryWithTransactionsDao.getCategoryById(category.getCategoryId());
        if (parentCategoryWithTransactions == null) {
            return BigDecimal.ZERO;
        }

        Set<Transaction> transactions = getTransactionsForCategory(category, includeSub);
        transactions.removeAll(excludedTransactions);
        if (startDate != null && endDate != null) {
            transactions.removeIf(t -> t.getDate().isBefore(startDate.atStartOfDay()) || t.getDate().isAfter(endDate.atStartOfDay()));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            total = total.add(transaction.getAmount());
        }
        return total;
    }

    public Map<CategoryWithTransactions, BigDecimal> getChildrenTotals(CategoryWithTransactions parentCategory, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        Set<Category> children = parentCategory.category.getChildren(false);
        if (children.isEmpty()) {
            return new HashMap<>();
        }
        Set<Transaction> excluded = new HashSet<>(parentCategory.transactions);

        Map<CategoryWithTransactions, BigDecimal> totals = new HashMap<>();
        for (Category child : children) {
            if (categoryService.categoryExists(child.getCategoryId())) {
                totals.put(categoryWithTransactionsDao.getCategoryById(child.getCategoryId()), getTotalInCategory(child, true, startDate, endDate, excluded));
                excluded.addAll(getTransactionsForCategory(child, true));
            }
        }
        return totals;
    }

    public LiveData<List<CategoryWithTransactions>> searchCategoriesWithType(String query, TrackingType type) {
        if (type == null) {
            return categoryWithTransactionsDao.searchCategories(query, TrackingUtlis.EMPTY_DESCRIPTION);
        }
        return categoryWithTransactionsDao.searchCategoriesByType(query, TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }
}
