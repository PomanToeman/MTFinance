package com.example.mtfinance.src.repositories.roomdatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    Long insert(Transaction transaction);

    @Query("SELECT * FROM transactions")
    List<Transaction> getAll();

    @Query("SELECT * FROM transactions")
    LiveData<List<Transaction>> getAllLive();


    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    Transaction getById(Long id);

    @Query("SELECT * FROM transactions WHERE transactionId IN (:ids)")
    List<Transaction> getByIds(java.util.Collection<Long> ids);

    @Update
    void update(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR (LOWER(description) LIKE '%' || LOWER(:query) || '%' AND description != :defaultDescription)")
    LiveData<List<Transaction>> searchTransactions(String query, String defaultDescription);

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE transactionId = :id)")
    Boolean exists(Long id);

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE hash = :hash)")
    Boolean hashExists(String hash);

    @Query("DELETE FROM transactions WHERE transactionId = :id")
    void delete(Long id);


}
