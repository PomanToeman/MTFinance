package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Concrete service class for transaction read operations.
 */
@Singleton
public class TransactionService {
    private final TransactionDao transactionDao;

    @Inject
    public TransactionService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public List<Transaction> getAll() {
        return transactionDao.getAll();
    }

    public LiveData<List<Transaction>> getAllLive() {
        return transactionDao.getAllLive();
    }

    public LiveData<List<Transaction>> searchTransactions(String query) {
        return transactionDao.searchTransactions(query, com.example.mtfinance.src.trackingengine.TrackingUtlis.EMPTY_DESCRIPTION);
    }

    public LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type) {
        return transactionDao.searchTransactionsWithType(query, com.example.mtfinance.src.trackingengine.TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }

    public Transaction getById(Long id) {
        return transactionDao.getById(id);
    }

    public List<Transaction> getByIds(Collection<Long> ids) {
        return transactionDao.getByIds(ids);
    }

    public Boolean transactionExists(Long id) {
        return transactionDao.exists(id);
    }

    public boolean hashExists(String hash) {
        if (hash == null || hash.isEmpty()) return false;
        return transactionDao.hashExists(hash);
    }
}
