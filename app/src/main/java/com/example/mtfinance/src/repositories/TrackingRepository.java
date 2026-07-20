package com.example.mtfinance.src.repositories;


import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * This is the main Tracking repository class.
 * To store and extract categories and transactions together from the room database.
 * Transactions and Categories follow a many-to-many relationship.
 * Each transaction has one or more categories that is in.
 */
public class TrackingRepository {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryTransactionDao categoryWithTransactionsDao;

    // constructor
    @Inject
    public TrackingRepository(CategoryRepository categoryRepository, TransactionRepository transactionRepository, CategoryTransactionDao categoryWithTransactionsDao) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.categoryWithTransactionsDao = categoryWithTransactionsDao;
    }

    /**
     * Insert a category into the database.
     * No two categories can have the same name.
     * You cannot insert income and account Transfer Category (will not be inserted).
     * You need to insert a category first before inserting any transactions under it.
     * @param category - the category to be inserted
     */
    public void insertCategory(Category category) {
        categoryRepository.insert(category);
    }

    /**
     * If you just want to insert a transaction with a specific category, use this method.
     * The Category must already be in the database.
     * will automatically insert under income or accountTransfer categories if not expense (regardless of category input).
     * It's best to insert the category first before inserting the transaction.
     *
     *
     * @param transaction - The transaction to be inserted.
     * @param CategoryId - The ID of a category already within a database.
     */
    public void insertTransaction(@NonNull Transaction transaction, @NonNull Long CategoryId) {

        Category category = categoryRepository.getCategoryById(CategoryId);
        if (category == null) {
            return;
        }
        if (!category.isSameType(transaction.getType())) {
            insertTransaction(transaction, categoryRepository.getRootCategoryByType(transaction.getType()).getCategoryId());
            return;
        }

        transactionRepository.insert(transaction);

        // to define relationship.
        CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(CategoryId, transaction.getTransactionId());
        categoryWithTransactionsDao.insertCrossRef(crossRef);
    }

    /**
     * To regonise a relationship between a category and relationship.
     * Both transaction and category must already be in the database.
     * @param transactionId - The ID of a transaction already within a database.
     * @param categoryId - The ID of a category already within a database.
     */
    public void insertRelationship(Long transactionId, Long categoryId) {
        if (transactionExists(transactionId) && categoryExists(categoryId) ) {
            CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(categoryId, transactionId);
            categoryWithTransactionsDao.insertCrossRef(crossRef);
        }
    }
    public void deleteRelationship(Long transactionId, Long categoryId) throws IllegalStateException {
        if (getCategoryIdsByTransactionId(transactionId).size() <= 1) {
            throw new IllegalStateException(MessageCli.TRANSACTION_MIN_CATEGORY.getMessage());
        }
        categoryWithTransactionsDao.deleteCrossRef(categoryId, transactionId);
    }

    public void deleteTransaction(Long transactionId) {
        categoryWithTransactionsDao.deleteCrossRefsForTransaction(transactionId);
        transactionRepository.delete(transactionId);
    }

    /**
     * Note when deleting a category, all transactions are automatically moved to their root category if you do
     * not delete transactions.
     * Cannot delete a root is well.
     * @param categoryId - The ID of a category that will be deleted.
     * @param deleteTransactions - If true, all transactions under the category will be deleted.
     */
    public void deleteCategory(Long categoryId, boolean deleteTransactions) {
        if (categoryExists(categoryId) && !isRoot(categoryId)) {

            List<Long> transactionIds = getTransactionIdsForCategory(categoryId);
            if (deleteTransactions) {
                for (Long transactionId : transactionIds) {
                    deleteTransaction(transactionId);
                }
            }
            else {
                Long rootCategoryId = categoryRepository.getRootCategoryByType(categoryRepository.getCategoryById(categoryId).getType()).getCategoryId();
                for (Long transactionId : transactionIds) {
                    insertRelationship(transactionId, rootCategoryId);
                }

            }


            categoryWithTransactionsDao.deleteCrossRefsForCategory(categoryId);
            categoryRepository.deleteCategory(categoryRepository.getCategoryById(categoryId));
        }
    }

    /**
     * If you just want to insert a transaction with no specific category, use this method.
     * This method will put it under the general category (or other roots if not expense type).
     * @param transaction - The transaction to be inserted.
     */
    public void insertTransactionDefault(@NonNull Transaction transaction) {
        Category generalCategory = categoryRepository.getRootCategoryByType(transaction.getType());
        insertTransaction(transaction, generalCategory.getCategoryId());

    }

    /**
     * finds all categories that has a given transaction with the same ID.
     *
     * @param id - The given ID of the transaction
     * @return - returns a set of categories that has the given transaction ID.
     */
     public List<Category> getCategoriesByTransactionId(Long id) {
         List<Long> categoryIds = categoryWithTransactionsDao.getCategoryIdsForTransaction(id);
         return categoryRepository.getCategoriesByIds(categoryIds);
    }

    public List<Long> getCategoryIdsByTransactionId(Long id) {
         return categoryWithTransactionsDao.getCategoryIdsForTransaction(id);
    }

    /**
     * Live Data is to ensure these instances stay in sync with Room when one updates.
     * @return
     */

    public LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions() {
         return categoryWithTransactionsDao.getAllCategoriesWithTransactions();
    }

    public CategoryWithTransactions getCategoryWithTransactionsByCategoryId(Long id) {
         return categoryWithTransactionsDao.getCategoryById(id);

    }


    public List<CategoryWithTransactions> getCategoryWithTransactionsByParentId(Long parentId) {
         return categoryWithTransactionsDao.getCategoriesWithTransactionsByParentId(parentId);
    }

    public List<Long> getTransactionIdsForCategory(Long categoryId) {
         return categoryWithTransactionsDao.getTransactionIdsForCategory(categoryId);
    }



    public List<CategoryWithTransactions> getCategoriesWithTransactionsByIds(java.util.Collection<Long> ids) {
         return categoryWithTransactionsDao.getCategoriesByIds(ids);
    }

    public LiveData<List<CategoryWithTransactions>> searchCategories(String query) {
         return categoryWithTransactionsDao.searchCategories(query, TrackingUtlis.EMPTY_DESCRIPTION);
    }

    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryRepository.autoSearchCategoryIds(query, type);
    }



    public void updateCategory(Category category) {
        categoryRepository.updateCategory(category);
    }

    public void updateTransaction(Transaction transaction) {
        transactionRepository.update(transaction);
    }

    public void updateCategoryTree(Category category) {
        categoryRepository.updateCategoryTree(category);
    }

    /**
     * Returns the Category with restored cache (parent and children) for the given ID.
     * @param id
     * @return
     */
    public Category getCategoryByIdRestored(Long id) {
        return categoryRepository.getCategoryByIdRestored(id);
    }

    /**
     * Allows for easy checks if a category is a root.
     * @param category
     * @return
     */
    public boolean isRoot(Category category) {
        if (category == null) return false;
        return categoryRepository.isRoot(category);
    }

    public boolean isRoot(Long id) {
        return isRoot(getCategoryByIdRestored(id));
    }









    /**
     *Includes all given transactions
     * @param category - the category to find the total.
     * @param includeSub adds the transactions' amounts of all sub-categories if true. Ignores duplicates.
     * @return returns the total amount of the category.
     */
    public BigDecimal getTotalInCategory(Category category, boolean includeSub) {
         return getTotalInCategory(category, includeSub, LocalDate.MIN, LocalDate.MAX);
    }

    /**
     *Includes all given transactions within the given date range.
     * @param category - the category to find the total.
     * @param includeSub adds the transactions' amounts of all sub-categories if true. Ignores duplicates.
     * @param startDate - the start date to filter by.
     * @param endDate - the end date to filter by.
     * @return returns the total amount of the category.
     */
    public BigDecimal getTotalInCategory(Category category, boolean includeSub, LocalDate startDate, LocalDate endDate) {
        CategoryWithTransactions parentCategorywithTransactions =categoryWithTransactionsDao.getCategoryById(category.getCategoryId());
        if (parentCategorywithTransactions == null) {
            return BigDecimal.ZERO;
        }


        Set<Transaction> transactions = new HashSet<>(parentCategorywithTransactions.transactions);

        if (includeSub) {
            // to cache the children of the category.
            parentCategorywithTransactions.category = categoryRepository.getCategoryRestored(parentCategorywithTransactions.category);

            for (Category child : parentCategorywithTransactions.category.getChildren(true)) {
                CategoryWithTransactions childCategory = categoryWithTransactionsDao.getCategoryById(child.getCategoryId());
                transactions.addAll(childCategory.transactions);
            }
        }

        transactions.removeIf(t -> t.getDate().isBefore(startDate.atStartOfDay()) || t.getDate().isAfter(endDate.atStartOfDay()));


        BigDecimal total = new BigDecimal("0");
        for (Transaction transaction : transactions) {
            total = total.add(transaction.getAmount());
        }
        return total;


    }

    public boolean categoryExists(Long id) {
        return categoryRepository.exists(id);
    }

    public boolean verifyExistingIdsCategories(Collection<Long> ids) {
        return categoryRepository.verifyExistingIds(ids);
    }

    public boolean categoryNameExists(String name) {
        return categoryRepository.nameExists(name);
    }

    public Category getRootCategoryByType(TrackingType type) {
        return categoryRepository.getRootCategoryByType(type);
    }



    // TRANSACTION METHODS

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionRepository.getAllLive();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.getById(id);
    }

    public LiveData<List<Transaction>> searchTransactions(String query) {
        return transactionRepository.searchTransactions(query);
    }

    public boolean transactionExists(Long id) {
        return transactionRepository.exists(id);
    }

    public boolean transactionHashExists(String hash) {
        return transactionRepository.hashExists(hash);
    }
}
