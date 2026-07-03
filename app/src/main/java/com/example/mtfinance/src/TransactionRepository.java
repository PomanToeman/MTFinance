package com.example.mtfinance.src;

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
}
