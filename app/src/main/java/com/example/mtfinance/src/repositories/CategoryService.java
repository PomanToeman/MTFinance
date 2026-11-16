package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

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
 * and leveraging CategoryRepositoryHelper for centralized initialization and validation.
 */
@Singleton
public class CategoryService {

    private final CategoryDao categoryDao;
    private final CategoryRepositoryHelper helper;

    /**
     * Constructs a CategoryService with the specified CategoryDao.
     * @param categoryDao the Room DAO for categories
     */
    @Inject
    public CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
        this.helper = new CategoryRepositoryHelper(categoryDao);
        helper.populateDefaultCategories();
    }

    /**
     * Test constructor supporting optional default category population.
     * @param categoryDao the Room DAO for categories
     * @param populateDefaultCategories whether to populate default categories
     */
    @VisibleForTesting
    public CategoryService(CategoryDao categoryDao, boolean populateDefaultCategories) {
        this.categoryDao = categoryDao;
        this.helper = new CategoryRepositoryHelper(categoryDao);
        if (populateDefaultCategories) helper.populateDefaultCategories();
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

    public Category getCategoryById(Long id) {
        return categoryDao.getById(id);
    }

    public Category getCategoryByIdRestored(@NonNull Long id) {
        return helper.getCategoryByIdRestored(id);
    }

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
        return helper.categoryExists(id);
    }

    public boolean categoryNameExists(String name) {
        return helper.categoryNameExists(name);
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
