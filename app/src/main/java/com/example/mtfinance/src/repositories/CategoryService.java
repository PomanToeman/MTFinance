package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.util.Collection;
import java.util.List;

public interface CategoryService {
    Category getGeneralCategory();
    Category getIncomeCategory();
    Category getAccountTransferCategory();
    Category getOtherCategory();
    Category getRootCategoryByType(TrackingType type);
    Category getCategoryById(Long id);

    /**
     * Returns the Category with restored cache (parent and children) for the given ID.
     * Category must already be in the database.
     * @param id - the ID of the category to restore.
     * @return - the category with restored cache.
     */
    Category getCategoryByIdRestored(@NonNull Long id);

    /**
     * Returns the Category with restored cache (parent and children) for the given instance.
     * Category must already be in the database, and will use that version for restoration.
     * @param category - the category to restore.
     * @return - the category with restored cache.
     */
    Category getCategoryRestored(@NonNull Category category);

    List<Category> getAllCategories();
    List<Category> getCategoriesByIds(Collection<Long> ids);
    boolean categoryExists(Long id);
    boolean categoryNameExists(String name);
    boolean verifyExistingIds(Collection<Long> ids);

    /**
     * Allows for easy checks if a category is a root.
     * @param category - the category to check.
     * @return - true if the category is a root.
     */
    boolean isRoot(Category category);
    boolean isRoot(Long categoryId);

    /**
     * Meant for auto sorting the best categories for a given transaction (or query).
     */
    List<Long> autoSearchCategoryIds(String query, TrackingType type);
}
