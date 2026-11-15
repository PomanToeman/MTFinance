package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

class CategoryRepository implements CategoryWriteRepository {

    // default categories
    public final Category generalCategory = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
    public final Category IncomeCategory = new Category("Income", "Tracks income transactions", BigDecimal.valueOf(1000), TrackingType.INCOME);
    public final Category accountTransferCategory = new Category("Account Transfer", "Account Transfers", BigDecimal.valueOf(1000), TrackingType.ACCOUNT_TRANSFERS);
    public final Category otherCategory = new Category("Other Category (only for errors)", "Use this as a log for erroneous transactions", BigDecimal.valueOf(1000), TrackingType.OTHER);

    public final List<Category> defaultExpenseCategories = List.of(
            new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100), TrackingType.EXPENSE),
            new Category("Utilities", "Utilities", BigDecimal.valueOf(100), TrackingType.EXPENSE));

    private final CategoryDao categoryDao;

    @Inject
    public CategoryRepository(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;

        new Thread(this::populateDefaultCategories).start();
    }

    @VisibleForTesting
    public CategoryRepository(CategoryDao categoryDao, boolean populateDefaultCategories) {
        this.categoryDao = categoryDao;

        if (populateDefaultCategories) populateDefaultCategories();
    }

    @Override
    public Long insert(@NonNull Category category) {
        if (category.equals(getGeneralCategory()) || !TrackingType.EXPENSE.equals(category.getType())) {
            return -1L;
        }

        if (category.getParentId() == null) {
            category.setParent(generalCategory); // ensures the greatest parent is the general category
        } else if (!exists(category.getParentId())) {
            insert(category.getParent()); // automatically inserts parent if not already in database.
        }

        Long result = categoryDao.insert(category);
        if (category.getParentId() != null) {
            updateCategoryTree(category);
        }
        return result;
    }

    public Category getGeneralCategory() {
        return getCategoryRestored(generalCategory); // general category.
    }

    public Category getIncomeCategory() {
        return IncomeCategory;
    }

    public Category getAccountTransferCategory() {
        return accountTransferCategory;
    }

    public Category getOtherCategory() {
        return otherCategory;
    }

    public List<Category> getAllCategories() {
        return categoryDao.getAll();
    }

    public Category getCategoryById(Long id) {
        return categoryDao.getById(id);
    }

    public boolean exists(Long id) {
        if (id == null) return false;
        return categoryDao.exists(id);
    }

    public boolean verifyExistingIds(Collection<Long> ids) {
        if (ids == null) {
            return false;
        }
        List<Long> existingIds = categoryDao.veifyExitsingIds(ids);
        return existingIds.size() == ids.size();
    }

    public boolean nameExists(String name) {
        if (name == null || name.isEmpty()) return false;
        return categoryDao.nameExists(name.trim());
    }

    @Override
    public void deleteCategory(@NonNull Category categoryToDelete) {
        if (categoryToDelete.equals(getGeneralCategory())) {
            return; // cannot delete general/root category
        }
        if (categoryToDelete.getChildren(false).isEmpty()) {
            categoryDao.delete(categoryToDelete);
            return;
        }

        Category categoryInDb = getCategoryById(categoryToDelete.getCategoryId());
        if (categoryInDb == null) {
            return; // not found
        }

        categoryToDelete.makeChildrenCongruent(); // make children at same level as parent
        updateAllCategories(categoryToDelete.getParent().getChildren(false)); // save new children to Db

        categoryDao.delete(categoryToDelete);
    }

    private void populateDefaultCategories() {
        if (categoryDao.getAll().isEmpty()) {
            categoryDao.insert(generalCategory);
            categoryDao.insert(IncomeCategory);
            categoryDao.insert(accountTransferCategory);
            categoryDao.insert(otherCategory);

            for (Category category : defaultExpenseCategories) {
                category.setParent(generalCategory);
                categoryDao.insert(category);
            }
        }
    }

    @Override
    public void updateCategory(@NonNull Category category) {
        if (isRoot(category)) {
            updateRootCategory(category);
        }
        categoryDao.update(category);
    }

    public void updateRootCategory(@NonNull Category rootCategory) {
        Category originalRootCategory = getRootCategoryByType(rootCategory.getType());
        originalRootCategory.setMonthlyBudget(rootCategory.getMonthlyBudget());
        categoryDao.update(originalRootCategory);
    }

    @Override
    public void updateAllCategories(@NonNull Collection<Category> categories) {
        categoryDao.updateAll(categories);
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
        return getCategoryByIdRestored(category.getCategoryId());
    }

    public List<Category> getCategoriesByIds(Collection<Long> ids) {
        return categoryDao.getByIds(ids);
    }

    @Override
    public void updateCategoryTree(Category category) {
        Set<Category> treeContents = new HashSet<>();
        treeContents.add(category);
        treeContents.addAll(category.getChildren(true));
        treeContents.addAll(category.getAncestors());

        Category general = getGeneralCategory();
        if (category.getType() == TrackingType.EXPENSE && !treeContents.contains(general)) {
            for (Category c : treeContents) {
                if (c.isDescendantOf(general)) {
                    treeContents.add(general);
                    break;
                }
            }
        }

        treeContents.remove(null);
        updateAllCategories(treeContents);
    }

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

    public boolean isRoot(Category category) {
        if (category == null) {
            return false;
        }
        return category.equals(getRootCategoryByType(category.getType()));
    }

    public boolean isRoot(Long categoryId) {
        if (exists(categoryId)) {
            return isRoot(getCategoryById(categoryId));
        }
        return false;
    }

    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryDao.autoSearchBestFittingCategories(query, TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }
}
