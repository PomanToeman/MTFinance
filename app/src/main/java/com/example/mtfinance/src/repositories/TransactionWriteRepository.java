package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.trackingengine.Transaction;

interface TransactionWriteRepository {
    Long insert(Transaction transaction);
    void update(Transaction transaction);
    void delete(Long id);
}
