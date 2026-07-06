package com.example.mtfinance.src.roomdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    Long insert(Transaction transaction);

    @Query("SELECT * FROM transactions")
    List<Transaction> getAll();

    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    Transaction getById(Long id);

    @Query("SELECT * FROM transactions WHERE transactionId IN (:ids)")
    List<Transaction> getByIds(java.util.Collection<Long> ids);

    @Update
    void update(Transaction transaction);
}
