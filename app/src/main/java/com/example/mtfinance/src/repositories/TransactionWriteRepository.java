package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.trackingengine.Transaction;

/**
 * Write-only repository contract for transaction mutations.
 */
interface TransactionWriteRepository {
    /**
     * Inserts a transaction into the database.
     * @param transaction - the transaction to insert
     * @return the generated transaction ID
     */
    Long insert(Transaction transaction);

    /**
     * Updates an existing transaction in the database.
     * @param transaction - the transaction to update
     */
    void update(Transaction transaction);

    /**
     * Deletes a transaction from the database by ID.
     * @param id - the ID of the transaction to delete
     */
    void delete(Long id);
}
