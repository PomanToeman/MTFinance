package com.example.mtfinance.src;


/**
 * This is the main Tracking repository class.
 * To store and extract categories and transactions together from the room database.
 * Transactions and Categories follow a many-to-many relationship.
 * Each transaction has one or more categories that is in.
 */
public class TrackingRepository {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public TrackingRepository(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

}
