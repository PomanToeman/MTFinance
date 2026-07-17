package com.example.mtfinance.src.repositories.roomdatabase;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.example.mtfinance.src.trackingengine.Category;

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

    @Query("SELECT categoryId FROM categories WHERE categoryId IN (:ids)")
    List<Long> veifyExitsingIds(Collection<Long> ids);

    @Query(
            "SELECT categoryId FROM categories WHERE " +
            "(LOWER(name) LIKE '%' || LOWER(:query) || '%' " +
            "OR (LOWER(description) LIKE '%' || LOWER(:query) || '%' AND description != :defaultDescription)) " +
             "AND type == :type " +
            "ORDER BY CASE WHEN LOWER(name) == LOWER(:query) THEN 0 " +
            "WHEN LOWER(name) LIKE LOWER(:query) || '%' THEN 1 " +
            "WHEN LOWER(name) LIKE '%' || LOWER(:query) || '%' THEN 2 " +
            "WHEN description LIKE '%' || :query || '%' THEN 3 " +
            "ELSE 4 END"


    )
    List<Long> autoSearchBestFittingCategories(String query, String defaultDescription, String type);










}
