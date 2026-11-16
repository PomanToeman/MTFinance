package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Package-private helper class responsible for centralized default/root category initialization,
 * root category verification, type resolution, category restoration, existence checks,
 * and recursive tree validation across services and repositories.
 */
class CategoryQueries {
    public final Category generalCategory = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
    public final Category incomeCategory = new Category("Income", "Tracks income transactions", BigDecimal.valueOf(1000), TrackingType.INCOME);
    public final Category accountTransferCategory = new Category("Account Transfer", "Account Transfers", BigDecimal.valueOf(1000), TrackingType.ACCOUNT_TRANSFERS);
    public final Category otherCategory = new Category("Other Category (only for errors)", "Use this as a log for erroneous transactions", BigDecimal.valueOf(1000), TrackingType.OTHER);

    public final List<Category> defaultExpenseCategories = List.of(
            new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100), TrackingType.EXPENSE),
            new Category("Utilities", "Utilities", BigDecimal.valueOf(100), TrackingType.EXPENSE));

    private final CategoryDao categoryDao;

    /**
     * Constructs a CategoryQueries with the given CategoryDao.
     * @param categoryDao the Room DAO for categories
     */
    public CategoryQueries(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    /**
     * Populates default and root categories in the database if none exist.
     */
    public synchronized void populateDefaultCategories() {
        if (categoryDao.getAll().isEmpty()) {
            categoryDao.insert(generalCategory);
            categoryDao.insert(incomeCategory);
            categoryDao.insert(accountTransferCategory);
            categoryDao.insert(otherCategory);

            for (Category category : defaultExpenseCategories) {
                category.setParent(generalCategory);
                categoryDao.insert(category);
            }
        }
    }

    public Category getGeneralCategory() {
        Category dbGeneral = categoryDao.getAll().stream()
                .filter(c -> generalCategory.getName().equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbGeneral != null ? getCategoryByIdRestored(dbGeneral.getCategoryId()) : getCategoryRestored(generalCategory);
    }

    public Category getIncomeCategory() {
        Category dbIncome = categoryDao.getAll().stream()
                .filter(c -> incomeCategory.getName().equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbIncome != null ? getCategoryByIdRestored(dbIncome.getCategoryId()) : incomeCategory;
    }

    public Category getAccountTransferCategory() {
        Category dbTransfer = categoryDao.getAll().stream()
                .filter(c -> accountTransferCategory.getName().equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbTransfer != null ? getCategoryByIdRestored(dbTransfer.getCategoryId()) : accountTransferCategory;
    }

    public Category getOtherCategory() {
        Category dbOther = categoryDao.getAll().stream()
                .filter(c -> otherCategory.getName().equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbOther != null ? getCategoryByIdRestored(dbOther.getCategoryId()) : otherCategory;
    }

    /**
     * Resolves the root category for the given TrackingType from database or defaults.
     * @param type the tracking type
     * @return the root Category
     */
    public Category getRootCategoryByType(@NonNull TrackingType type) {
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

    /**
     * Checks if the given category is a system root category.
     * Uses dynamic comparison against root category name for its type without hardcoded string literals.
     * @param category the category to check
     * @return true if it is a root category
     */
    public boolean isRoot(@NonNull Category category) {
        Category root = getRootCategoryByType(category.getType());
        return category.getName().equals(root.getName());
    }

    /**
     * Checks if the category ID corresponds to a system root category.
     * @param categoryId the non-null category ID
     * @return true if it is a root category
     */
    public boolean isRoot(@NonNull Long categoryId) {
        Category c = categoryDao.getById(categoryId);
        if (c == null) return false;
        return isRoot(c);
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

    public boolean categoryExists(Long id) {
        if (id == null) return false;
        return categoryDao.exists(id);
    }

    public boolean categoryNameExists(String name) {
        if (name == null || name.isEmpty()) return false;
        return categoryDao.nameExists(name.trim());
    }

    /**
     * Validates category tree hierarchy and checks for circular dependencies.
     * @param category the category tree node
     */
    public void validateCategoryTree(@NonNull Category category) {
        Long parentId = category.getParentId();
        while (parentId != null) {
            if (parentId.equals(category.getCategoryId())) {
                throw new IllegalArgumentException("Cannot set a category as a child of its own descendant.");
            }
            Category p = categoryDao.getById(parentId);
            parentId = p != null ? p.getParentId() : null;
        }
    }
}
