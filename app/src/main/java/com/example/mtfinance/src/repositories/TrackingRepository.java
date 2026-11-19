package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.trackingengine.TransactionViolationException;

import java.util.List;

import javax.inject.Inject;

/**
 * Concrete Tracking repository class for managing categories and transactions together
 * across Room database DAOs and enforcing business policies without service dependencies.
 */
public class TrackingRepository {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryTransactionDao categoryWithTransactionsDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final CategoryTreeHelper helper;

    /**
     * Constructs a TrackingRepository with required repositories, DAOs, and helper.
     * @param categoryRepository the category write repository
     * @param transactionRepository the transaction write repository
     * @param categoryWithTransactionsDao the cross-ref DAO
     * @param categoryDao the category DAO
     * @param transactionDao the transaction DAO
     * @param helper the category tree helper
     */
    @Inject
    public TrackingRepository(CategoryRepository categoryRepository,
                              TransactionRepository transactionRepository,
                              CategoryTransactionDao categoryWithTransactionsDao,
                              CategoryDao categoryDao,
                              TransactionDao transactionDao,
                              CategoryTreeHelper helper) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.categoryWithTransactionsDao = categoryWithTransactionsDao;
        this.categoryDao = categoryDao;
        this.transactionDao = transactionDao;
        this.helper = helper;
    }

    /**
     * Insert a category into the database.
     * @param category - the category to be inserted
     * @throws CategoryViolationException if insertion violates category policies
     */
    public void insertCategory(@NonNull Category category) throws CategoryViolationException {
        categoryRepository.insert(category);
    }

    /**
     * Inserts a transaction with a specific category ID.
     * @param transaction - The transaction to be inserted.
     * @param categoryId - The ID of a category already within the database.
     * @throws CategoryViolationException if category does not exist
     * @throws TransactionViolationException if transaction insertion fails or type mismatches category type
     */
    public void insertTransaction(@NonNull Transaction transaction, @NonNull Long categoryId) throws CategoryViolationException, TransactionViolationException {
        Category category = categoryDao.getById(categoryId);
        if (category == null) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        if (!category.isSameType(transaction.getType())) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_TYPE_MISMATCH.getMessage());
        }

        transactionRepository.insert(transaction);

        CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(categoryId, transaction.getTransactionId());
        categoryWithTransactionsDao.insertCrossRef(crossRef);
    }

    /**
     * Inserts a transaction under the default root category for its type.
     * @param transaction - The transaction to be inserted.
     * @throws TransactionViolationException if transaction insertion fails
     */
    public void insertTransactionDefault(@NonNull Transaction transaction) throws TransactionViolationException {
        Category generalCategory = helper.getRootCategoryByType(transaction.getType());
        insertTransaction(transaction, generalCategory.getCategoryId());
    }

    /**
     * Establishes a relationship between a transaction and a category.
     * @param transactionId - The transaction ID
     * @param categoryId - The category ID
     * @throws TransactionViolationException if transaction does not exist or types mismatch
     * @throws CategoryViolationException if category does not exist
     */
    public void insertRelationship(@NonNull Long transactionId, @NonNull Long categoryId) throws TransactionViolationException, CategoryViolationException {
        if (!transactionDao.exists(transactionId)) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
        }
        if (categoryId == null || !categoryDao.exists(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }

        Category category = categoryDao.getById(categoryId);
        Transaction transaction = transactionDao.getById(transactionId);
        if (category == null || transaction == null || !category.isSameType(transaction.getType())) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_TYPE_MISMATCH.getMessage());
        }

        CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(categoryId, transactionId);
        categoryWithTransactionsDao.insertCrossRef(crossRef);
    }

    /**
     * Deletes the relationship between a transaction and a category.
     * @param transactionId - The transaction ID
     * @param categoryId - The category ID
     * @throws TransactionViolationException if transaction does not exist or transaction would be left with zero categories
     * @throws CategoryViolationException if category does not exist
     */
    public void deleteRelationship(@NonNull Long transactionId, @NonNull Long categoryId) throws TransactionViolationException, CategoryViolationException {
        if (!transactionDao.exists(transactionId)) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
        }
        if (categoryId == null || !categoryDao.exists(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        if (categoryWithTransactionsDao.getCategoryIdsForTransaction(transactionId).size() <= 1) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_MIN_CATEGORY.getMessage());
        }
        categoryWithTransactionsDao.deleteCrossRef(categoryId, transactionId);
    }

    /**
     * Deletes a transaction and all its category relationships.
     * @param transactionId - The transaction ID
     * @throws TransactionViolationException if transaction does not exist in database
     */
    public void deleteTransaction(@NonNull Long transactionId) throws TransactionViolationException {
        if (transactionId == null || !transactionDao.exists(transactionId)) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
        }
        categoryWithTransactionsDao.deleteCrossRefsForTransaction(transactionId);
        transactionRepository.delete(transactionId);
    }

    /**
     * Deletes a category and handles its transactions and sub-categories.
     * @param categoryId - The category ID
     * @param deleteTransactions - If true, delete all associated transactions
     * @throws CategoryViolationException if category does not exist or is a system root category
     */
    public void deleteCategory(@NonNull Long categoryId, boolean deleteTransactions) throws CategoryViolationException {
        if (categoryId == null || !categoryDao.exists(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        if (helper.isRoot(categoryId)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }

        List<Long> transactionIds = categoryWithTransactionsDao.getTransactionIdsForCategory(categoryId);
        if (deleteTransactions) {
            for (Long transactionId : transactionIds) {
                deleteTransaction(transactionId);
            }
        } else {
            Category category = categoryDao.getById(categoryId);
            if (category != null) {
                Long rootCategoryId = helper.getRootCategoryByType(category.getType()).getCategoryId();
                for (Long transactionId : transactionIds) {
                    insertRelationship(transactionId, rootCategoryId);
                }
            }
        }

        categoryWithTransactionsDao.deleteCrossRefsForCategory(categoryId);
        Category category = helper.getCategoryByIdRestored(categoryId);
        if (category != null) {
            categoryRepository.deleteCategory(category);
        }
    }

    /**
     * Updates a category.
     * @param category - the category to update
     * @throws CategoryViolationException if category is a system root category or does not exist
     */
    public void updateCategory(@NonNull Category category) throws CategoryViolationException {
        categoryRepository.updateCategory(category);
    }

    /**
     * Updates a transaction.
     * @param transaction - the transaction to update
     * @throws TransactionViolationException if transaction does not exist
     */
    public void updateTransaction(@NonNull Transaction transaction) throws TransactionViolationException {
        transactionRepository.update(transaction);
    }

    /**
     * Updates a category tree.
     * @param category - the category tree root
     * @throws CategoryViolationException if root category is modified or circular reference detected
     */
    public void updateCategoryTree(@NonNull Category category) throws CategoryViolationException {
        categoryRepository.updateCategoryTree(category);
    }

    /**
     * Explicitly updates the monthly budget of a system root category identified by TrackingType.
     * @param type the tracking type of the root category
     * @param newBudget the new monthly budget amount
     * @throws CategoryViolationException if category is not found or not a root category
     */
    public void updateRootCategoryBudget(@NonNull com.example.mtfinance.src.trackingengine.TrackingType type, @NonNull java.math.BigDecimal newBudget) throws CategoryViolationException {
        categoryRepository.updateRootCategoryBudget(type, newBudget);
    }

    /**
     * Explicitly updates the monthly budget of a system root category identified by ID.
     * @param categoryId the category ID
     * @param newBudget the new monthly budget amount
     * @throws CategoryViolationException if category is not found or not a root category
     */
    public void updateRootCategoryBudget(@NonNull Long categoryId, @NonNull java.math.BigDecimal newBudget) throws CategoryViolationException {
        categoryRepository.updateRootCategoryBudget(categoryId, newBudget);
    }
}
