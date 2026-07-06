package com.example.mtfinance.src;

import com.example.mtfinance.src.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.List;

public class TransactionRepository {
    private final TransactionDao transactionDao;

    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public Long insert(Transaction transaction) {
        return transactionDao.insert(transaction);
    }

    public List<Transaction> getAll() {
        return transactionDao.getAll();
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
