package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.List;

import javax.inject.Inject;

/**
 * This is the main Tracking repository class.
 * To store and extract categories and transactions together from the room database.
 * Transactions and Categories follow a many-to-many relationship.
 * Each transaction has one or more categories that is in.
 */
public class TrackingRepository implements TrackingWriteRepository {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryTransactionDao categoryWithTransactionsDao;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

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
     * No two categories can have the same name.
     * You cannot insert income and account Transfer Category (will not be inserted).
     * You need to insert a category first before inserting any transactions under it.
     * @param category - the category to be inserted
     */
    @Override
    public void insertCategory(Category category) {
        categoryRepository.insert(category);
    }

    /**
     * If you just want to insert a transaction with a specific category, use this method.
     * The Category must already be in the database.
     * will automatically insert under income or accountTransfer categories if not expense (regardless of category input).
     * It's best to insert the category first before inserting the transaction.
     *
     * @param transaction - The transaction to be inserted.
     * @param categoryId - The ID of a category already within a database.
     */
    @Override
    public void insertTransaction(@NonNull Transaction transaction, @NonNull Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            return;
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
     * If you just want to insert a transaction with no specific category, use this method.
     * This method will put it under the general category (or other roots if not expense type).
     * @param transaction - The transaction to be inserted.
     */
    @Override
    public void insertTransactionDefault(@NonNull Transaction transaction) {
        Category generalCategory = categoryService.getRootCategoryByType(transaction.getType());
        insertTransaction(transaction, generalCategory.getCategoryId());
    }

    /**
     * To recolonise a relationship between a category and transaction.
     * Both transaction and category must already be in the database.
     * @param transactionId - The ID of a transaction already within a database.
     * @param categoryId - The ID of a category already within a database.
     */
    @Override
    public void insertRelationship(Long transactionId, Long categoryId) throws IllegalStateException {
        if (transactionService.transactionExists(transactionId) && categoryService.categoryExists(categoryId)) {
            if (!categoryService.getCategoryById(categoryId).isSameType(transactionService.getById(transactionId).getType())) {
                throw new IllegalStateException(MessageCli.TRANSACTION_TYPE_MISMATCH.getMessage());
            }

            CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(categoryId, transactionId);
            categoryWithTransactionsDao.insertCrossRef(crossRef);
        }
    }

    @Override
    public void deleteRelationship(Long transactionId, Long categoryId) throws IllegalStateException {
        if (categoryWithTransactionsDao.getCategoryIdsForTransaction(transactionId).size() <= 1) {
            throw new IllegalStateException(MessageCli.TRANSACTION_MIN_CATEGORY.getMessage());
        }
        categoryWithTransactionsDao.deleteCrossRef(categoryId, transactionId);
    }

    @Override
    public void deleteTransaction(Long transactionId) {
        categoryWithTransactionsDao.deleteCrossRefsForTransaction(transactionId);
        transactionRepository.delete(transactionId);
    }

    /**
     * Note when deleting a category, all transactions are automatically moved to their root category if you do
     * not delete transactions.
     * Cannot delete a root as well.
     * @param categoryId - The ID of a category that will be deleted.
     * @param deleteTransactions - If true, all transactions under the category will be deleted.
     */
    @Override
    public void deleteCategory(Long categoryId, boolean deleteTransactions) {
        if (categoryService.categoryExists(categoryId) && !categoryService.isRoot(categoryId)) {
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
    }

    @Override
    public void updateCategory(Category category) {
        categoryRepository.updateCategory(category);
    }

    @Override
    public void updateTransaction(Transaction transaction) {
        transactionRepository.update(transaction);
    }

    @Override
    public void updateCategoryTree(Category category) {
        categoryRepository.updateCategoryTree(category);
    }
}
