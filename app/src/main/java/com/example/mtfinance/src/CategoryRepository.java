package com.example.mtfinance.src;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CategoryRepository {

    public final List<Category> defaultCategories = new ArrayList<>();
    public final Category root = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000));


    private final CategoryDao categoryDao;

    public CategoryRepository(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
        // default categories
        defaultCategories.add(new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100)));
        defaultCategories.add(new Category("Utilities", "Utilities", BigDecimal.valueOf(100)));

        populateDefaultCategories();
    }

    public Long insert(@NonNull Category category) {
        if (category.equals(getGeneralCategory())) {
            return -1L; // cannot insert general category
        }

        if (category.getParentId() == null) {
            category.setParent(getGeneralCategory()); // ensures the greatest parent is the general category
            category.setParentId(getGeneralCategory().getId());
        }
        else if (!getAllCategories().contains(category.getParent())) {
            insert(category.getParent()); // automatically inserts parent if not already in database.
        }

        return categoryDao.insert(category);
    }

    public Category getGeneralCategory() {
        return categoryDao.getById(1L);
    }

    public List<Category> getAllCategories() {
        return categoryDao.getAll();
    }

    public Category getCategoryById(long id) {
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
        Category categoryInDb = getCategoryById(categoryToDelete.getId());
        if (categoryInDb == null) {
            return; // not found
        }

        // Re-parent children to grandparent (or root)
        Long parentId = categoryInDb.getParentId();
        Set<Category> children = categoryToDelete.getChildren(false);


        for (Category child : children) {
            child.setParentId(1L);
            categoryDao.update(child);         // save to DB//
        }

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




}
