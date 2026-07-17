package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class CategoryRepository {

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




    public Long insert(@NonNull Category category) {
        if (category.equals(getGeneralCategory()) || !TrackingType.EXPENSE.equals(category.getType())) {
            return -1L;
        }

        if (category.getParentId() == null) {
            category.setParent(generalCategory); // ensures the greatest parent is the general category

        }
        else if (!exists(category.getParentId())) {
            insert(category.getParent()); // automatically inserts parent if not already in database.
        }

        return categoryDao.insert(category);
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
        if (ids == null)  {
            return false;
        }
        List<Long> existingIds = categoryDao.veifyExitsingIds(ids);
        return existingIds.size() == ids.size();


    }

    public boolean nameExists(String name) {
        if (name == null || name.isEmpty()) return false;
        return categoryDao.nameExists(name.trim());
    }

    public void deleteCategory(@NonNull Category categoryToDelete) {
        if (categoryToDelete.equals(getGeneralCategory())) {
            return; // cannot delete general/root category
        }
        if (categoryToDelete.getChildren(false).isEmpty()) {
            categoryDao.delete(categoryToDelete);
            return;
        }


        // Get the actual entity from DB
        Category categoryInDb = getCategoryById(categoryToDelete.getCategoryId());
        if (categoryInDb == null) {
            return; // not found
        }

        // Re-parent children to grandparent (or root)
        Long parentId = categoryInDb.getParentId();
        Set<Category> children = categoryToDelete.getChildren(false);

        categoryToDelete.makeChildrenCongruent(); // make children at same level as parent
        updateAllCategories(children); // save new children to Db

        categoryDao.delete(categoryToDelete);
    }


    /**
     * Meant to add all necessary categories to the database that are needed for the app to work.
     * This includes the root categories for each type, and the default expense categories.
     */

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


    public void updateCategory(@NonNull Category category) {
        if (category.equals(getGeneralCategory()) || !TrackingType.EXPENSE.equals(category.getType())) {
            return;
        }
        categoryDao.update(category);
    }

    public void updateAllCategories(@NonNull Collection<Category> categories) {
        categoryDao.updateAll(categories);
    }

    /**
     * Returns the Category with restored cache (parent and children) for the given ID.
     * Category must already be in the database.
     * @param id - the ID of the category to restore.
     * @return - the category with restored cache.
     */
    public Category getCategoryByIdRestored(@NonNull Long id) {
        return getCategoryByIdRestoredInternal(id, new HashSet<>());
    }

    /**
     *
     * @param id - the ID of the category to restore (recusrive)
     * @param visited - a set of IDs that have already been restored.
     * @return - the category with restored cache.
     */
    private Category getCategoryByIdRestoredInternal(Long id, Set<Long> visited) {
        if (id == null || visited.contains(id)) return null;
        visited.add(id); // to track if categories has already been restored.


        Category category = categoryDao.getById(id);
        // restore all parents
        if (category != null) {
            Category parent = getCategoryByIdRestoredInternal(category.getParentId(), visited);
            if (parent != null) {
                category.setParent(parent);
            }

            // restore all children.
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

    /**
     * Returns the Category with restored cache (parent and children) for the given instance.
     * Category must already be in the database, and will use that version for restoration.
     * If it is a modified instance, please update the category first before using this.
     * @param category - the category to restore.
     * @return - the category with restored cache.
     */
    public Category getCategoryRestored(@NonNull Category category) {
        return getCategoryByIdRestored(category.getCategoryId()); // this will create a copy
    }



    public List<Category> getCategoriesByIds(Collection<Long> ids) {
        return categoryDao.getByIds(ids);
    }

    /**
     * This assumes the category Tree has been modified.
     * Only works when Category has its cache restored.
     * @param category - the category inside the tree (must have cache restored).
     */
    public void updateCategoryTree(Category category) {
        Set<Category> treeContents = new HashSet<>();
        treeContents.add(category);
        treeContents.addAll(category.getChildren(true));
        treeContents.addAll(category.getAncestors());
        treeContents.remove(null); // for safety
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

    /**
     * meant for auto sorting the best categories for a given transaction (or query)
     */
    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryDao.autoSearchBestFittingCategories(query, TrackingUtlis.EMPTY_DESCRIPTION, type.toString());
    }














}
