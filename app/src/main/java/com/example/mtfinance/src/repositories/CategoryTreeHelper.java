package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Public helper class specifically dedicated to complex category tree and hierarchy operations:
 * default/root category seeding, root category getters via direct SQLite name lookups, recursive entity tree restoration,
 * and category tree cycle validation. Simple database existence queries interact with CategoryDao directly.
 */
@Singleton
public class CategoryTreeHelper {
    public final Category generalCategory = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
    public final Category incomeCategory = new Category("Income", "Tracks income transactions", BigDecimal.valueOf(1000), TrackingType.INCOME);
    public final Category accountTransferCategory = new Category("Account Transfer", "Account Transfers", BigDecimal.valueOf(1000), TrackingType.ACCOUNT_TRANSFERS);
    public final Category otherCategory = new Category("Other Category (only for errors)", "Use this as a log for erroneous transactions", BigDecimal.valueOf(1000), TrackingType.OTHER);

    public final List<Category> defaultExpenseCategories = List.of(
            new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100), TrackingType.EXPENSE),
            new Category("Utilities", "Utilities", BigDecimal.valueOf(100), TrackingType.EXPENSE));

    private final CategoryDao categoryDao;

    /**
     * Constructs a CategoryTreeHelper with the given CategoryDao.
     * @param categoryDao the Room DAO for categories
     */
    @Inject
    public CategoryTreeHelper(CategoryDao categoryDao) {
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
     * Queries a category by its exact name using direct SQLite lookup and restores its tree hierarchy.
     * @param name the category name to look up
     * @return the restored Category, or null if not found
     */
    public Category getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Category dbCategory = categoryDao.getByName(name.trim());
        return dbCategory != null ? getCategoryByIdRestored(dbCategory.getCategoryId()) : null;
    }

    public Category getGeneralCategory() {
        Category dbGeneral = getCategoryByName(generalCategory.getName());
        return dbGeneral != null ? dbGeneral : getCategoryRestored(generalCategory);
    }

    public Category getIncomeCategory() {
        Category dbIncome = getCategoryByName(incomeCategory.getName());
        return dbIncome != null ? dbIncome : incomeCategory;
    }

    public Category getAccountTransferCategory() {
        Category dbTransfer = getCategoryByName(accountTransferCategory.getName());
        return dbTransfer != null ? dbTransfer : accountTransferCategory;
    }

    public Category getOtherCategory() {
        Category dbOther = getCategoryByName(otherCategory.getName());
        return dbOther != null ? dbOther : otherCategory;
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
        if (id == null) return null;

        Map<Long, Category> cache = new HashMap<>();
        collectRelatedCategories(id, cache, new HashSet<>());

        if (!cache.containsKey(id)) return null;

        List<Category> nodes = new ArrayList<>(cache.values());
        nodes.sort(Comparator.comparingInt(c -> countAncestorIdsInCache(c, cache)));

        for (Category c : nodes) {
            if (c.getParentId() != null && cache.containsKey(c.getParentId())) {
                Category p = cache.get(c.getParentId());
                if (p != null) {
                    c.setParent(p);
                }
            }
        }

        return cache.get(id);
    }

    private void collectRelatedCategories(Long id, Map<Long, Category> cache, Set<Long> visited) {
        if (id == null || visited.contains(id)) return;
        visited.add(id);

        Category category = categoryDao.getById(id);
        if (category == null) return;

        cache.put(category.getCategoryId(), category);

        if (category.getParentId() != null) {
            collectRelatedCategories(category.getParentId(), cache, visited);
        }

        List<Category> children = categoryDao.getByParentId(category.getCategoryId());
        if (children != null) {
            for (Category child : children) {
                if (child != null) {
                    collectRelatedCategories(child.getCategoryId(), cache, visited);
                }
            }
        }
    }

    private int countAncestorIdsInCache(Category c, Map<Long, Category> cache) {
        int depth = 0;
        Long pId = c.getParentId();
        Set<Long> visited = new HashSet<>();
        while (pId != null && cache.containsKey(pId) && visited.add(pId)) {
            depth++;
            Category parent = cache.get(pId);
            pId = parent != null ? parent.getParentId() : null;
        }
        return depth;
    }

    public Category getCategoryRestored(@NonNull Category category) {
        return category.getCategoryId() != null ? getCategoryByIdRestored(category.getCategoryId()) : category;
    }

    /**
     * Validates category tree hierarchy and checks for circular dependencies.
     * @param category the category tree node
     * @throws CategoryViolationException if circular hierarchy is detected
     */
    public void validateCategoryTree(@NonNull Category category) throws CategoryViolationException {
        Long parentId = category.getParentId();
        while (parentId != null) {
            if (parentId.equals(category.getCategoryId())) {
                throw new CategoryViolationException("Cannot set a category as a child of its own descendant.");
            }
            Category p = categoryDao.getById(parentId);
            parentId = p != null ? p.getParentId() : null;
        }
    }
}
