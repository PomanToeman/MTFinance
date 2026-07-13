package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.List;

import javax.inject.Inject;

public class TransactionRepository {
    private final TransactionDao transactionDao;

    @Inject
    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public Long insert(Transaction transaction) {
        return transactionDao.insert(transaction);
    }

    public List<Transaction> getAll() {
        return transactionDao.getAll();
    }

    public LiveData<List<Transaction>> getAllLive() {
        return transactionDao.getAllLive();
    }

    public LiveData<List<Transaction>> searchTransactions(String query) {
        return transactionDao.searchTransactions(query);
    }


    public Transaction getById(Long id) {
        return transactionDao.getById(id);
    }

    public List<Transaction> getByIds(java.util.Collection<Long> ids) {
        return transactionDao.getByIds(ids);
    }

    public void update(Transaction transaction) {
        transactionDao.update(transaction);
    }
}
