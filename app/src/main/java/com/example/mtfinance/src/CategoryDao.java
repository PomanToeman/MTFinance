package com.example.mtfinance.src;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import java.util.Collection;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    Long insert(Category category);


    @Query("SELECT * FROM categories")
    List<Category> getAll();

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getById(Long id);

    @Delete
    void delete(Category category);

    @Update
    void update(Category category);

    @Update
    void updateAll(Collection<Category> categories);

    @Query("SELECT * FROM categories WHERE parent_id = :id")
    List<Category> getByParentId(Long id);
}
