package com.example.mtfinance.src;


import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is the main Tracking repository class.
 * To store and extract categories and transactions together from the room database.
 * Transactions and Categories follow a many-to-many relationship.
 * Each transaction has one or more categories that is in.
 */
public class TrackingRepository {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    // constructor
    public TrackingRepository(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Insert a category into the database.
     * Warining: this method assumes any chanced transactions are already in the database (as it only brings the IDs).
     *  this does not insert/update the cached transactions into the database. Must be done separately.
     * @param category - the category to be inserted
     */
    public void insertCategory(Category category) {
        categoryRepository.insert(category);
    }

    /**
     * If you just want to insert a transaction with a specific category, use this method.
     * The Category must already be in the database.
     * It's best to insert the category first before inserting the transaction.
     *
     * @param transaction - The transaction to be inserted.
     * @param CategoryId - The ID of a category already within a database.
     */
    public void insertTransaction(@NonNull Transaction transaction, @NonNull Long CategoryId) {
        Category category = categoryRepository.getCategoryById(CategoryId);
        if (category == null) {
            return;
        }
        category.addTransaction(transaction);
        categoryRepository.updateCategory(category);


        transactionRepository.insert(transaction);
    }

    /**
     * If you just want to insert a transaction with no specific category, use this method.
     * This method will put it under the general category.
     * @param transaction - The transaction to be inserted.
     */
    public void insertTransactionDefault(@NonNull Transaction transaction) {
        Category generalCategory = categoryRepository.getGeneralCategory();
        generalCategory.addTransaction(transaction);
        categoryRepository.updateCategory(generalCategory);
        transactionRepository.insert(transaction);

    }

    /**
     * finds all categories that has a given transaction with the same ID.
     *
     * @param id - The given ID of the transaction
     * @return - returns a set of categories that has the given transaction ID.
     */
     public List<Category> findCategoriesByTransactionId(Long id) {

        return categoryRepository.getCategoriesByTransactionId(id);
    }

}
