package com.example.mtfinance.src;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryRepository {

    public final List<Category> defaultCategories = new ArrayList<>();
    public final Category root = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000), TrackingType.EXPENSE);


    private final CategoryDao categoryDao;

    public CategoryRepository(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
        // default categories
        defaultCategories.add(new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100), TrackingType.EXPENSE));
        defaultCategories.add(new Category("Utilities", "Utilities", BigDecimal.valueOf(100), TrackingType.EXPENSE));

        populateDefaultCategories();
    }

    public Long insert(@NonNull Category category) {
        if (category.equals(getGeneralCategory())) {
            return -1L; // cannot insert general category
        }

        if (category.getParentId() == null) {
            category.setParent(getGeneralCategory()); // ensures the greatest parent is the general category
            category.setParentId(getGeneralCategory().getCategoryId());
        }
        else if (!getAllCategories().contains(category.getParent())) {
            insert(category.getParent()); // automatically inserts parent if not already in database.
        }

        return categoryDao.insert(category);
    }

    public Category getGeneralCategory() {
        return getCategoryRestored(root); // general category.
    }

    public List<Category> getAllCategories() {
        return categoryDao.getAll();
    }

    public Category getCategoryById(Long id) {
        return categoryDao.getById(id);
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


    private void populateDefaultCategories() {
        if (categoryDao.getAll().isEmpty()) {
            categoryDao.insert(root);

            for (Category category : defaultCategories) {
                category.setParent(root);
                categoryDao.insert(category);
            }
        }
    }


    public void updateCategory(@NonNull Category category) {
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


    /**
     * Meant for repositories
     * @return
     */
    protected CategoryDao getCategoryDao() {
        return categoryDao;
    }












}
