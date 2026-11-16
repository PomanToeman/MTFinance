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

/**
 * Concrete service class managing read-only category query responsibilities via Room DAOs
 * and leveraging CategoryRepositoryHelper for centralized initialization and validation.
 */
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDao categoryDao;
    private final CategoryRepositoryHelper helper;

    /**
     * Constructs a CategoryServiceImpl with the specified CategoryDao.
     * @param categoryDao the Room DAO for categories
     */
    @Inject
    public CategoryServiceImpl(CategoryDao categoryDao) {
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
    public CategoryServiceImpl(CategoryDao categoryDao, boolean populateDefaultCategories) {
        this.categoryDao = categoryDao;
        this.helper = new CategoryRepositoryHelper(categoryDao);
        if (populateDefaultCategories) helper.populateDefaultCategories();
    }

    @Override
    public Category getGeneralCategory() {
        Category dbGeneral = categoryDao.getAll().stream()
                .filter(c -> "General Category".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbGeneral != null ? getCategoryByIdRestored(dbGeneral.getCategoryId()) : getCategoryRestored(helper.generalCategory);
    }

    @Override
    public Category getIncomeCategory() {
        Category dbIncome = categoryDao.getAll().stream()
                .filter(c -> "Income".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbIncome != null ? getCategoryByIdRestored(dbIncome.getCategoryId()) : helper.incomeCategory;
    }

    @Override
    public Category getAccountTransferCategory() {
        Category dbTransfer = categoryDao.getAll().stream()
                .filter(c -> "Account Transfer".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbTransfer != null ? getCategoryByIdRestored(dbTransfer.getCategoryId()) : helper.accountTransferCategory;
    }

    @Override
    public Category getOtherCategory() {
        Category dbOther = categoryDao.getAll().stream()
                .filter(c -> "Other Category (only for errors)".equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbOther != null ? getCategoryByIdRestored(dbOther.getCategoryId()) : helper.otherCategory;
    }

    @Override
    public Category getRootCategoryByType(TrackingType type) {
        switch (type) {
            case EXPENSE:
                return getGeneralCategory();
            case INCOME:
                return getIncomeCategory();
            case ACCOUNT_TRANSFERS:
                return getAccountTransferCategory();
            default:
                return getOtherCategory();
        }
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryDao.getById(id);
    }

    @Override
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

    @Override
    public Category getCategoryRestored(@NonNull Category category) {
        return category.getCategoryId() != null ? getCategoryByIdRestored(category.getCategoryId()) : category;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryDao.getAll();
    }

    @Override
    public List<Category> getCategoriesByIds(Collection<Long> ids) {
        return categoryDao.getByIds(ids);
    }

    @Override
    public boolean categoryExists(Long id) {
        if (id == null) return false;
        return categoryDao.exists(id);
    }

    @Override
    public boolean categoryNameExists(String name) {
        if (name == null || name.isEmpty()) return false;
        return categoryDao.nameExists(name.trim());
    }

    @Override
    public boolean verifyExistingIds(Collection<Long> ids) {
        if (ids == null) {
            return false;
        }
        List<Long> existingIds = categoryDao.veifyExitsingIds(ids);
        return existingIds.size() == ids.size();
    }

    @Override
    public boolean isRoot(Category category) {
        if (category == null) {
            return false;
        }
        return helper.isRoot(category);
    }

    @Override
    public boolean isRoot(Long categoryId) {
        return helper.isRoot(categoryId);
    }

    @Override
    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryDao.autoSearchBestFittingCategories(query, TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }
}
