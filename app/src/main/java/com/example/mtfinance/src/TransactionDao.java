package com.example.mtfinance.src;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    Long insert(Transaction transaction);

    @Query("SELECT * FROM transactions")
    List<Transaction> getAll();

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getById(Long id);

    @Query("SELECT * FROM transactions WHERE id IN (:ids)")
    List<Transaction> getByIds(java.util.Collection<Long> ids);
}
