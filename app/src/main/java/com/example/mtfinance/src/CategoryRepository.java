package com.example.mtfinance.src;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    public static final List<Category> defaultCategories = new ArrayList<>();

    private final CategoryDao categoryDao;

    public CategoryRepository(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
        // default categories
        defaultCategories.add(new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100)));
        defaultCategories.add(new Category("Utilities", "Utilities", BigDecimal.valueOf(100)));

        populateDefaultCategories();
    }

    public long insert(@NonNull Category category) {
        if (category.equals(getGeneralCategory())) {
            return -1; // cannot insert general category
        }

        if (category.getParent() == null) {
            category.setParent(getGeneralCategory()); // ensures the greatest parent is the general category
        }
        else if (getAllCategories().contains(category.getParent())) {
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


    private void populateDefaultCategories() {
        if (categoryDao.getAll().isEmpty()) {
            Category root = new Category("General Category", "General tracking for all categories", BigDecimal.valueOf(1000));
            categoryDao.insert(root);

            for (Category category : defaultCategories) {
                category.setParent(root);
                categoryDao.insert(category);
            }
        }
    }




}
