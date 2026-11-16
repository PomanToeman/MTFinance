package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.trackingengine.TransactionViolationException;

import java.util.List;

import javax.inject.Inject;

/**
 * Concrete Tracking repository class for managing categories and transactions together
 * across Room database DAOs and enforcing business policies.
 */
public class TrackingRepository {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryTransactionDao categoryWithTransactionsDao;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    /**
     * Constructs a TrackingRepository with required repositories, DAOs, and services.
     * @param categoryRepository the category write repository
     * @param transactionRepository the transaction write repository
     * @param categoryWithTransactionsDao the cross-ref DAO
     * @param categoryService the category query service
     * @param transactionService the transaction query service
     */
    @Inject
    public TrackingRepository(CategoryRepository categoryRepository,
                              TransactionRepository transactionRepository,
                              CategoryTransactionDao categoryWithTransactionsDao,
                              CategoryService categoryService,
                              TransactionService transactionService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.categoryWithTransactionsDao = categoryWithTransactionsDao;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    /**
     * Insert a category into the database.
     * Throws CategoryViolationException if insertion violates category policies.
     * @param category - the category to be inserted
     */
    public void insertCategory(@NonNull Category category) {
        categoryRepository.insert(category);
    }

    /**
     * Inserts a transaction with a specific category ID.
     * Throws CategoryViolationException or TransactionViolationException if inputs are invalid.
     * @param transaction - The transaction to be inserted.
     * @param categoryId - The ID of a category already within the database.
     */
    public void insertTransaction(@NonNull Transaction transaction, @NonNull Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        if (!category.isSameType(transaction.getType())) {
            insertTransaction(transaction, categoryService.getRootCategoryByType(transaction.getType()).getCategoryId());
            return;
        }

        transactionRepository.insert(transaction);

        CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(categoryId, transaction.getTransactionId());
        categoryWithTransactionsDao.insertCrossRef(crossRef);
    }

    /**
     * Inserts a transaction under the default root category for its type.
     * @param transaction - The transaction to be inserted.
     */
    public void insertTransactionDefault(@NonNull Transaction transaction) {
        Category generalCategory = categoryService.getRootCategoryByType(transaction.getType());
        insertTransaction(transaction, generalCategory.getCategoryId());
    }

    /**
     * Establishes a relationship between a transaction and a category.
     * Throws TransactionViolationException or CategoryViolationException if types mismatch or entities do not exist.
     * @param transactionId - The transaction ID
     * @param categoryId - The category ID
     */
    public void insertRelationship(@NonNull Long transactionId, @NonNull Long categoryId) {
        if (!transactionService.transactionExists(transactionId)) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
        }
        if (!categoryService.categoryExists(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }

        if (!categoryService.getCategoryById(categoryId).isSameType(transactionService.getById(transactionId).getType())) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_TYPE_MISMATCH.getMessage());
        }

        CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(categoryId, transactionId);
        categoryWithTransactionsDao.insertCrossRef(crossRef);
    }

    /**
     * Deletes the relationship between a transaction and a category.
     * Throws TransactionViolationException if a transaction would be left with zero categories.
     * @param transactionId - The transaction ID
     * @param categoryId - The category ID
     */
    public void deleteRelationship(@NonNull Long transactionId, @NonNull Long categoryId) {
        if (categoryWithTransactionsDao.getCategoryIdsForTransaction(transactionId).size() <= 1) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_MIN_CATEGORY.getMessage());
        }
        categoryWithTransactionsDao.deleteCrossRef(categoryId, transactionId);
    }

    /**
     * Deletes a transaction and all its category relationships.
     * @param transactionId - The transaction ID
     */
    public void deleteTransaction(@NonNull Long transactionId) {
        categoryWithTransactionsDao.deleteCrossRefsForTransaction(transactionId);
        transactionRepository.delete(transactionId);
    }

    /**
     * Deletes a category and handles its transactions and sub-categories.
     * Throws CategoryViolationException if trying to delete root categories.
     * @param categoryId - The category ID
     * @param deleteTransactions - If true, delete all associated transactions
     */
    public void deleteCategory(@NonNull Long categoryId, boolean deleteTransactions) {
        if (!categoryService.categoryExists(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        if (categoryService.isRoot(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }

        List<Long> transactionIds = categoryWithTransactionsDao.getTransactionIdsForCategory(categoryId);
        if (deleteTransactions) {
            for (Long transactionId : transactionIds) {
                deleteTransaction(transactionId);
            }
        } else {
            Long rootCategoryId = categoryService.getRootCategoryByType(categoryService.getCategoryById(categoryId).getType()).getCategoryId();
            for (Long transactionId : transactionIds) {
                insertRelationship(transactionId, rootCategoryId);
            }
        }

        categoryWithTransactionsDao.deleteCrossRefsForCategory(categoryId);
        categoryRepository.deleteCategory(categoryService.getCategoryByIdRestored(categoryId));
    }

    /**
     * Updates a category.
     * @param category - the category to update
     */
    public void updateCategory(@NonNull Category category) {
        categoryRepository.updateCategory(category);
    }

    /**
     * Updates a transaction.
     * @param transaction - the transaction to update
     */
    public void updateTransaction(@NonNull Transaction transaction) {
        transactionRepository.update(transaction);
    }

    /**
     * Updates a category tree.
     * @param category - the category tree root
     */
    public void updateCategoryTree(@NonNull Category category) {
        categoryRepository.updateCategoryTree(category);
    }
}
