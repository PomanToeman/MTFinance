package com.example.mtfinance.src.roomdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCrossRef(CategoryTransactionCrossRef crossRef);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllCrossRefs(List<CategoryTransactionCrossRef> crossRefs);

    @Query("DELETE FROM categoryTransactionCrossRef WHERE transactionId = :transactionId")
    void deleteCrossRefsForTransaction(long transactionId);

    @Query("DELETE FROM categoryTransactionCrossRef WHERE categoryId = :categoryId")
    void deleteCrossRefsForCategory(long categoryId);


    @Query("SELECT categoryId FROM categoryTransactionCrossRef WHERE transactionId = :transactionId")
    List<Long> getCategoryIdsForTransaction(long transactionId);

    @Query("SELECT transactionId FROM categoryTransactionCrossRef WHERE categoryId = :categoryId")
    List<Long> getTransactionIdsForCategory(long categoryId);

}
