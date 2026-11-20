package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Concrete service class managing read-only category query responsibilities via Room DAOs
 * and leveraging CategoryTreeHelper for centralized initialization and validation.
 */
@Singleton
public class CategoryService {

    private final CategoryDao categoryDao;
    private final CategoryTreeHelper helper;

    /**
     * Constructs a CategoryService with required CategoryDao and CategoryTreeHelper.
     * @param categoryDao the Room DAO for categories
     * @param helper the category tree helper
     */
    @Inject
    public CategoryService(CategoryDao categoryDao, CategoryTreeHelper helper) {
        this.categoryDao = categoryDao;
        this.helper = helper;
    }

    public Category getGeneralCategory() {
        return helper.getGeneralCategory();
    }

    public Category getIncomeCategory() {
        return helper.getIncomeCategory();
    }

    public Category getAccountTransferCategory() {
        return helper.getAccountTransferCategory();
    }

    public Category getOtherCategory() {
        return helper.getOtherCategory();
    }

    public Category getRootCategoryByType(TrackingType type) {
        return helper.getRootCategoryByType(type);
    }

    /**
     * Note that this category DOESN'T come with category cache fields (i.e. parents and children)
     * @param id - The ID of category within the database.
     * @return The skimmed category without cache.
     */
    public Category getCategoryByIdSkim(Long id) {
        return categoryDao.getById(id);
    }

    /**
     * Queries a category by exact name using direct SQLite lookup and restores its tree hierarchy.
     * @param name the category name
     * @return the restored Category, or null if not found
     */
    public Category getCategoryByName(String name) {
        return helper.getCategoryByName(name);
    }

    /**
     * This fully restores the category cache.
     * @param id - The ID of category within the database.
     * @return Category with cache.
     */
    public Category getCategoryByIdRestored(@NonNull Long id) {
        return helper.getCategoryByIdRestored(id);
    }

    /**
     * fully restores/updates the cache of a given category.
     * @param category - A given category (must already be in database)
     * @return category with cache fully restored.
     */
    public Category getCategoryRestored(@NonNull Category category) {
        return helper.getCategoryRestored(category);
    }

    public List<Category> getAllCategories() {
        return categoryDao.getAll();
    }

    public List<Category> getCategoriesByIds(Collection<Long> ids) {
        return categoryDao.getByIds(ids);
    }

    public boolean categoryExists(Long id) {
        if (id == null) return false;
        return categoryDao.exists(id);
    }

    public boolean categoryNameExists(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return categoryDao.nameExists(name.trim());
    }

    public boolean verifyExistingIds(Collection<Long> ids) {
        if (ids == null) {
            return false;
        }
        List<Long> existingIds = categoryDao.veifyExitsingIds(ids);
        return existingIds.size() == ids.size();
    }

    public boolean isRoot(Category category) {
        if (category == null) {
            return false;
        }
        return helper.isRoot(category);
    }

    public boolean isRoot(Long categoryId) {
        if (categoryId == null) return false;
        return helper.isRoot(categoryId);
    }

    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryDao.autoSearchBestFittingCategories(query, TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }
}
