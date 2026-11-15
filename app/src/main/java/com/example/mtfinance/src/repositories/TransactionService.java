package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;

import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.Collection;
import java.util.List;

public interface TransactionService {
    List<Transaction> getAll();
    LiveData<List<Transaction>> getAllLive();
    LiveData<List<Transaction>> searchTransactions(String query);
    LiveData<List<Transaction>> searchTransactionsWithType(String query, TrackingType type);
    Transaction getById(Long id);
    List<Transaction> getByIds(Collection<Long> ids);
    Boolean exists(Long id);
    boolean hashExists(String hash);
}
