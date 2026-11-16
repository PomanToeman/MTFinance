package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Category dbGeneral = categoryDao.getAll().stream()
                .filter(c -> "General Category".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbGeneral != null ? getCategoryByIdRestored(dbGeneral.getCategoryId()) : getCategoryRestored(helper.generalCategory);
    }

    public Category getIncomeCategory() {
        Category dbIncome = categoryDao.getAll().stream()
                .filter(c -> "Income".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbIncome != null ? getCategoryByIdRestored(dbIncome.getCategoryId()) : helper.incomeCategory;
    }

    public Category getAccountTransferCategory() {
        Category dbTransfer = categoryDao.getAll().stream()
                .filter(c -> "Account Transfer".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbTransfer != null ? getCategoryByIdRestored(dbTransfer.getCategoryId()) : helper.accountTransferCategory;
    }

    public Category getOtherCategory() {
        Category dbOther = categoryDao.getAll().stream()
                .filter(c -> "Other Category (only for errors)".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbOther != null ? getCategoryByIdRestored(dbOther.getCategoryId()) : helper.otherCategory;
    }

    public Category getRootCategoryByType(TrackingType type) {
        return helper.getRootCategoryByType(type);
    }

    public Category getCategoryById(Long id) {
        return categoryDao.getById(id);
    }

    public Category getCategoryByIdRestored(@NonNull Long id) {
        return getCategoryByIdRestoredInternal(id, new HashSet<>());
    }

    private Category getCategoryByIdRestoredInternal(Long id, Set<Long> visited) {
        if (id == null || visited.contains(id)) return null;
        visited.add(id);

        Category category = categoryDao.getById(id);
        if (category != null) {
            Category parent = getCategoryByIdRestoredInternal(category.getParentId(), visited);
            if (parent != null) {
                category.setParent(parent);
            }

            for (Category child : categoryDao.getByParentId(category.getCategoryId())) {
                if (child != null) {
                    Category childRestored = getCategoryByIdRestoredInternal(child.getCategoryId(), visited);
                    if (childRestored != null) {
                        childRestored.setParent(category);
                    }
                }
            }
        }
        return category;
    }

    public Category getCategoryRestored(@NonNull Category category) {
        return category.getCategoryId() != null ? getCategoryByIdRestored(category.getCategoryId()) : category;
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
        if (name == null || name.isEmpty()) return false;
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
        return helper.isRoot(categoryId);
    }

    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryDao.autoSearchBestFittingCategories(query, TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }
}
