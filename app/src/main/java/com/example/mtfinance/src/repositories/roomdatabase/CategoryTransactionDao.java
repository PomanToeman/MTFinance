package com.example.mtfinance.src.repositories.roomdatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;

import java.util.Collection;
import java.util.List;

@Dao
public interface CategoryTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCrossRef(CategoryTransactionCrossRef crossRef);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllCrossRefs(List<CategoryTransactionCrossRef> crossRefs);

    @Transaction
    @Query("SELECT * FROM categories")
    LiveData<List<CategoryWithTransactions>> getAllCategoriesWithTransactions();

    @Query("DELETE FROM categoryTransactionCrossRef WHERE transactionId = :transactionId")
    void deleteCrossRefsForTransaction(long transactionId);

    @Query("DELETE FROM categoryTransactionCrossRef WHERE categoryId = :categoryId")
    void deleteCrossRefsForCategory(long categoryId);

    @Query("DELETE FROM categoryTransactionCrossRef WHERE categoryId = :categoryId AND transactionId = :transactionId")
    void deleteCrossRef(long categoryId, long transactionId);


    @Transaction
    @Query("SELECT * FROM categories WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(description) LIKE '%' || LOWER(:query) || '%'")
    LiveData<List<CategoryWithTransactions>> searchCategories(String query);

    @Query("SELECT categoryId FROM categoryTransactionCrossRef WHERE transactionId = :transactionId")
    List<Long> getCategoryIdsForTransaction(long transactionId);

    @Query("SELECT transactionId FROM categoryTransactionCrossRef WHERE categoryId = :categoryId")
    List<Long> getTransactionIdsForCategory(long categoryId);

    @Transaction
    @Query("SELECT * FROM categories WHERE categoryId = :id")
    CategoryWithTransactions getCategoryById(Long id);

    @Transaction
    @Query("SELECT * FROM categories WHERE categoryId IN (:categoryIds)")
    LiveData<List<CategoryWithTransactions>> getCategoriesByIds(Collection<Long> categoryIds);




    @Transaction
    @Query("SELECT * FROM categories WHERE parent_id = :parentId")
    List<CategoryWithTransactions> getCategoriesWithTransactionsByParentId(Long parentId);





}
