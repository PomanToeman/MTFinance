package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.Transaction;

public interface TrackingWriteRepository {
    void insertCategory(Category category);
    void insertTransaction(@NonNull Transaction transaction, @NonNull Long categoryId);
    void insertTransactionDefault(@NonNull Transaction transaction);
    void insertRelationship(Long transactionId, Long categoryId) throws IllegalStateException;
    void deleteRelationship(Long transactionId, Long categoryId) throws IllegalStateException;
    void deleteTransaction(Long transactionId);
    void deleteCategory(Long categoryId, boolean deleteTransactions);
    void updateCategory(Category category);
    void updateTransaction(Transaction transaction);
    void updateCategoryTree(Category category);
}
