package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class TransactionServiceImpl implements TransactionService {
    private final TransactionDao transactionDao;

    @Inject
    public TransactionServiceImpl(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Override
    public List<Transaction> getAll() {
        return transactionDao.getAll();
    }

    @Override
    public LiveData<List<Transaction>> getAllLive() {
        return transactionDao.getAllLive();
    }

    @Override
    public LiveData<List<Transaction>> searchTransactions(String query) {
        return transactionDao.searchTransactions(query, com.example.mtfinance.src.trackingengine.TrackingUtlis.EMPTY_DESCRIPTION);
    }

    @Override
    public LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type) {
        return transactionDao.searchTransactionsWithType(query, com.example.mtfinance.src.trackingengine.TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }

    @Override
    public Transaction getById(Long id) {
        return transactionDao.getById(id);
    }

    @Override
    public List<Transaction> getByIds(Collection<Long> ids) {
        return transactionDao.getByIds(ids);
    }

    @Override
    public Boolean transactionExists(Long id) {
        return transactionDao.exists(id);
    }

    @Override
    public boolean hashExists(String hash) {
        if (hash == null || hash.isEmpty()) return false;
        return transactionDao.hashExists(hash);
    }
}
