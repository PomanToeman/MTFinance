package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.Collection;
import java.util.List;

/**
 * Service interface for transaction read operations.
 */
public interface TransactionService {
    /**
     * Gets all transactions in the database.
     * @return list of all transactions
     */
    List<Transaction> getAll();

    /**
     * LiveData stream of all transactions in the database.
     * @return LiveData list of all transactions
     */
    LiveData<List<Transaction>> getAllLive();

    /**
     * Searches transactions matching the given query string.
     * @param query - search text
     * @return LiveData list of matching transactions
     */
    LiveData<List<Transaction>> searchTransactions(String query);

    /**
     * Searches transactions matching the given query string and type filter.
     * @param query - search text
     * @param type - tracking type filter
     * @return LiveData list of matching transactions
     */
    LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type);

    /**
     * Gets a transaction by its ID.
     * @param id - transaction ID
     * @return transaction or null if not found
     */
    Transaction getById(Long id);

    /**
     * Gets transactions matching a collection of IDs.
     * @param ids - collection of transaction IDs
     * @return list of matching transactions
     */
    List<Transaction> getByIds(Collection<Long> ids);

    /**
     * Checks if a transaction exists with the given ID.
     * @param id - transaction ID
     * @return true if transaction exists
     */
    Boolean transactionExists(Long id);

    /**
     * Checks if a transaction exists with the given generated hash.
     * @param hash - generated transaction hash string
     * @return true if hash exists
     */
    boolean hashExists(String hash);
}
