package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Package-private helper class responsible for centralized default/root category initialization,
 * root category verification, type resolution, and recursive tree validation across services and repositories.
 */
class CategoryRepositoryHelper {
    public final Category generalCategory = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
    public final Category incomeCategory = new Category("Income", "Tracks income transactions", BigDecimal.valueOf(1000), TrackingType.INCOME);
    public final Category accountTransferCategory = new Category("Account Transfer", "Account Transfers", BigDecimal.valueOf(1000), TrackingType.ACCOUNT_TRANSFERS);
    public final Category otherCategory = new Category("Other Category (only for errors)", "Use this as a log for erroneous transactions", BigDecimal.valueOf(1000), TrackingType.OTHER);

    public final List<Category> defaultExpenseCategories = List.of(
            new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100), TrackingType.EXPENSE),
            new Category("Utilities", "Utilities", BigDecimal.valueOf(100), TrackingType.EXPENSE));

    private final CategoryDao categoryDao;

    /**
     * Constructs a CategoryRepositoryHelper with the given CategoryDao.
     * @param categoryDao the Room DAO for categories
     */
    public CategoryRepositoryHelper(CategoryDao categoryDao) {
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

    /**
     * Resolves the root category for the given TrackingType from database or defaults.
     * @param type the tracking type
     * @return the root Category
     */
    public Category getRootCategoryByType(@NonNull TrackingType type) {
        String rootName;
        Category fallback;
        switch (type) {
            case EXPENSE:
                rootName = "General Category";
                fallback = generalCategory;
                break;
            case INCOME:
                rootName = "Income";
                fallback = incomeCategory;
                break;
            case ACCOUNT_TRANSFERS:
                rootName = "Account Transfer";
                fallback = accountTransferCategory;
                break;
            default:
                rootName = "Other Category (only for errors)";
                fallback = otherCategory;
                break;
        }

        Category dbRoot = categoryDao.getAll().stream()
                .filter(c -> rootName.equals(c.getName()))
                .findFirst()
                .orElse(null);
        return dbRoot != null ? dbRoot : fallback;
    }

    /**
     * Checks if the given category is a system root category.
     * @param category the category to check
     * @return true if it is a root category
     */
    public boolean isRoot(@NonNull Category category) {
        return isRoot(category.getCategoryId()) ||
                "General Category".equals(category.getName()) ||
                "Income".equals(category.getName()) ||
                "Account Transfer".equals(category.getName()) ||
                "Other Category (only for errors)".equals(category.getName());
    }

    /**
     * Checks if the category ID corresponds to a system root category.
     * @param categoryId the category ID
     * @return true if it is a root category
     */
    public boolean isRoot(Long categoryId) {
        if (categoryId == null) return false;
        Category c = categoryDao.getById(categoryId);
        if (c == null) return false;
        return "General Category".equals(c.getName()) ||
                "Income".equals(c.getName()) ||
                "Account Transfer".equals(c.getName()) ||
                "Other Category (only for errors)".equals(c.getName());
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
