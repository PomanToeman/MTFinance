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

import javax.inject.Inject;

public class TrackingServiceImpl implements TrackingService {
    private final TrackingRepository trackingRepository;

    @Inject
    public TrackingServiceImpl(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

    @Override
    public Category getCategoryByIdRestored(Long id) {
        return trackingRepository.getCategoryByIdRestored(id);
    }

    @Override
    public CategoryWithTransactions getCategoryWithTransactionsByCategoryId(Long id) {
        return trackingRepository.getCategoryWithTransactionsByCategoryId(id);
    }

    @Override
    public List<CategoryWithTransactions> getCategoryWithTransactionsByParentId(Long parentId) {
        return trackingRepository.getCategoryWithTransactionsByParentId(parentId);
    }

    @Override
    public LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions() {
        return trackingRepository.getAllCategoriesWithTransactions();
    }

    @Override
    public List<CategoryWithTransactions> getCategoriesWithTransactionsByIds(Collection<Long> categoryIds) {
        return trackingRepository.getCategoriesWithTransactionsByIds(categoryIds);
    }

    @Override
    public List<Category> getCategoriesByTransactionId(Long transactionId) {
        return trackingRepository.getCategoriesByTransactionId(transactionId);
    }

    @Override
    public List<Long> getCategoryIdsByTransactionId(Long transactionId) {
        return trackingRepository.getCategoryIdsByTransactionId(transactionId);
    }

    @Override
    public List<Long> getTransactionIdsForCategory(Long categoryId) {
        return trackingRepository.getTransactionIdsForCategory(categoryId);
    }

    @Override
    public Set<Transaction> getTransactionsForCategory(Category category, boolean includeSubs) {
        return trackingRepository.getTransactionsForCategory(category, includeSubs);
    }

    @Override
    public BigDecimal getTotalInCategory(Category category, boolean includeSubs) {
        return trackingRepository.getTotalInCategory(category, includeSubs);
    }

    @Override
    public BigDecimal getTotalInCategory(Category category, boolean includeSubs, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        return trackingRepository.getTotalInCategory(category, includeSubs, startDate, endDate);
    }

    @Override
    public Map<CategoryWithTransactions, BigDecimal> getChildrenTotals(CategoryWithTransactions parentCategory, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        return trackingRepository.getChildrenTotals(parentCategory, startDate, endDate);
    }

    @Override
    public LiveData<List<CategoryWithTransactions>> searchCategoriesWithType(String query, TrackingType type) {
        return trackingRepository.searchCategoriesWithType(query, type);
    }

    @Override
    public LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type) {
        return trackingRepository.searchTransactionsWithType(query, type);
    }

    @Override
    public LiveData<List<Transaction>> getAllTransactions() {
        return trackingRepository.getAllTransactions();
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return trackingRepository.getTransactionById(id);
    }

    @Override
    public boolean categoryExists(Long id) {
        return trackingRepository.categoryExists(id);
    }

    @Override
    public boolean categoryNameExists(String name) {
        return trackingRepository.categoryNameExists(name);
    }

    @Override
    public boolean transactionExists(Long id) {
        return trackingRepository.transactionExists(id);
    }

    @Override
    public boolean isRoot(Category category) {
        return trackingRepository.isRoot(category);
    }

    @Override
    public boolean isRoot(Long categoryId) {
        return trackingRepository.isRoot(categoryId);
    }
}
