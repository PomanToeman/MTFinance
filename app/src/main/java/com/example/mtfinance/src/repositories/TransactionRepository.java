package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.Transaction;

import javax.inject.Inject;

/**
 * Package-private repository for transaction write and mutation operations.
 */
class TransactionRepository implements TransactionWriteRepository {
    private final TransactionDao transactionDao;

    @Inject
    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /**
     * Inserts a new transaction into the database.
     * @param transaction - the transaction to be inserted
     * @return the row ID of the inserted transaction
     */
    @Override
    public Long insert(Transaction transaction) {
        return transactionDao.insert(transaction);
    }

    /**
     * Updates an existing transaction in the database.
     * @param transaction - the transaction to be updated
     */
    @Override
    public void update(Transaction transaction) {
        transactionDao.update(transaction);
    }

    /**
     * Deletes a transaction from the database by its ID.
     * @param id - the transaction ID
     */
    @Override
    public void delete(Long id) {
        transactionDao.delete(id);
    }
}
