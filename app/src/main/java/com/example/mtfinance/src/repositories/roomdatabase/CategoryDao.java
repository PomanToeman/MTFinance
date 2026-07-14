package com.example.mtfinance.src.repositories.roomdatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;


import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;

import java.util.Collection;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    Long insert(Category category);


    @Query("SELECT * FROM categories")
    List<Category> getAll();

    @Query("SELECT * FROM categories WHERE categoryId = :id")
    Category getById(Long id);

    @Delete
    void delete(Category category);

    @Update
    void update(Category category);

    @Update
    void updateAll(Collection<Category> categories);

    @Query("SELECT * FROM categories WHERE parent_id = :id")
    List<Category> getByParentId(Long id);

    @Query("SELECT * FROM categories WHERE categoryId IN (:ids)")
    List<Category> getByIds(java.util.Collection<Long> ids);

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE parent_id = :id)")
    Boolean hasChildren(Long id);

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE categoryId = :id)")
    Boolean exists(Long id);

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE LOWER(name) = LOWER(:name))")
    Boolean nameExists(String name);

    @Query("SELECT (SELECT COUNT(*) FROM categories) == 0")
    boolean isEmpty();










}
