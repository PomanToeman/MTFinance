package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;

    @Inject
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<Transaction> getAll() {
        return transactionRepository.getAll();
    }

    @Override
    public LiveData<List<Transaction>> getAllLive() {
        return transactionRepository.getAllLive();
    }

    @Override
    public LiveData<List<Transaction>> searchTransactions(String query) {
        return transactionRepository.searchTransactions(query);
    }

    @Override
    public LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type) {
        return transactionRepository.searchTransactionsWithType(query, type);
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.getById(id);
    }

    @Override
    public List<Transaction> getByIds(Collection<Long> ids) {
        return transactionRepository.getByIds(ids);
    }

    @Override
    public Boolean exists(Long id) {
        return transactionRepository.exists(id);
    }

    @Override
    public boolean hashExists(String hash) {
        return transactionRepository.hashExists(hash);
    }
}
