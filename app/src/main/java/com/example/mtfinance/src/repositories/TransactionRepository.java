package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.trackingengine.TransactionViolationException;

import javax.inject.Inject;

/**
 * Concrete repository class for transaction write and mutation operations.
 */
public class TransactionRepository {
    private final TransactionDao transactionDao;

    /**
     * Constructs a TransactionRepository with the specified TransactionDao.
     * @param transactionDao the Room DAO for transactions
     */
    @Inject
    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /**
     * Inserts a new transaction into the database.
     * Throws TransactionViolationException if insertion fails.
     * @param transaction - the transaction to be inserted
     * @return the row ID of the inserted transaction
     */
    public Long insert(@NonNull Transaction transaction) {
        Long result = transactionDao.insert(transaction);
        if (result == null || result <= 0) {
            throw new TransactionViolationException(MessageCli.TRANSACTION_SAVE_FAILED.getMessage("Database insert failed"));
        }
        return result;
    }

    /**
     * Updates an existing transaction in the database.
     * @param transaction - the transaction to be updated
     */
    public void update(@NonNull Transaction transaction) {
        transactionDao.update(transaction);
    }

    /**
     * Deletes a transaction from the database by its ID.
     * @param id - the transaction ID
     */
    public void delete(@NonNull Long id) {
        transactionDao.delete(id);
    }
}
