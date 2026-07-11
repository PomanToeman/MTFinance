package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import  com.example.mtfinance.src.trackingengine.*;

import java.math.BigDecimal;
import java.util.List;

public interface TrackingRepositoryInterface {

    public void insertCategory(Category category);
    public void insertTransaction(Transaction transaction, Long CategoryId);
   public void updateCategory(Category category);
   public void updateTransaction(Transaction transaction);
    public List<Long> getTransactionIdsForCategory(Long categoryId);
    public void updateCategoryTree(Category category);
    public Category getCategoryByIdRestored(Long id);
    public LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions();
    public CategoryWithTransactions getCategoryWithTransactionsByCategoryId(Long id);
    public BigDecimal findTotalInCategory(Category category, boolean includeSub);

    public List<CategoryWithTransactions> getCategoryWithTransactionsByParentId(Long parentId);
    public List<Category> findCategoriesByTransactionId(Long id);



}
