package com.example.mtfinance.src.repositories;


import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
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
     * If you just want to insert a transaction with no specific category, use this method.
     * This method will put it under the general category (or other roots if not expense type).
     * @param transaction - The transaction to be inserted.
     */
    public void insertTransactionDefault(@NonNull Transaction transaction) {
        Category generalCategory = categoryRepository.getGeneralCategory();
        insertTransaction(transaction, generalCategory.getCategoryId());

    }

    /**
     * finds all categories that has a given transaction with the same ID.
     *
     * @param id - The given ID of the transaction
     * @return - returns a set of categories that has the given transaction ID.
     */
     public List<Category> findCategoriesByTransactionId(Long id) {
         List<Long> categoryIds = categoryWithTransactionsDao.getCategoryIdsForTransaction(id);
         return categoryRepository.getCategoriesByIds(categoryIds);
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



    public LiveData<List<CategoryWithTransactions>> getCategoriesWithTransactionsByIds(java.util.Collection<Long> ids) {
         return categoryWithTransactionsDao.getCategoriesByIds(ids);
    }

    public LiveData<List<CategoryWithTransactions>> searchCategories(String query) {
         return categoryWithTransactionsDao.searchCategories(query);
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
     *
     * @param category - the category to find the total.
     * @param includeSub adds the transactions' amounts of all sub-categories if true. Ignores duplicates.
     * @return returns the total amount of the category.
     */
    public BigDecimal findTotalInCategory(Category category, boolean includeSub) {
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

         BigDecimal total = new BigDecimal("0");
         for (Transaction transaction : transactions) {
             total = total.add(transaction.getAmount());
         }
         return total;


    }




}
