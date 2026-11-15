package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.trackingengine.Category;

import java.util.Collection;

interface CategoryWriteRepository {
    Long insert(@NonNull Category category);
    void deleteCategory(@NonNull Category categoryToDelete);
    void updateCategory(@NonNull Category category);
    void updateAllCategories(Collection<Category> categories);
    void updateCategoryTree(Category root);
}
