package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.messages.TransactionMessages;
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
     * @param transaction - the transaction to be inserted
     * @return the row ID of the inserted transaction
     * @throws TransactionViolationException if insertion fails
     */
    public Long insert(@NonNull Transaction transaction) {
        Long result = transactionDao.insert(transaction);
        if (result == null || result <= 0) {
            throw new TransactionViolationException(TransactionMessages.TRANSACTION_SAVE_FAILED.getMessage("Database insert failed"));
        }
        return result;
    }

    /**
     * Updates an existing transaction in the database.
     * @param transaction - the transaction to be updated
     * @throws TransactionViolationException if transaction does not exist in database
     */
    public void update(@NonNull Transaction transaction) {
        if (transaction.getTransactionId() == null || !transactionDao.exists(transaction.getTransactionId())) {
            throw new TransactionViolationException(TransactionMessages.TRANSACTION_NOT_FOUND.getMessage());
        }
        transactionDao.update(transaction);
    }

    /**
     * Deletes a transaction from the database by its ID.
     * @param id - the transaction ID
     * @throws TransactionViolationException if transaction does not exist in database
     */
    public void delete(@NonNull Long id) {
        if (id == null || !transactionDao.exists(id)) {
            throw new TransactionViolationException(TransactionMessages.TRANSACTION_NOT_FOUND.getMessage());
        }
        transactionDao.delete(id);
    }
}
