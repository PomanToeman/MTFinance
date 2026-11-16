package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.util.Collection;

import javax.inject.Inject;

class CategoryRepository implements CategoryWriteRepository {
    private final CategoryDao categoryDao;
    private final CategoryService categoryService;

    @Inject
    public CategoryRepository(CategoryDao categoryDao, CategoryService categoryService) {
        this.categoryDao = categoryDao;
        this.categoryService = categoryService;
    }

    @Override
    public Long insert(@NonNull Category category) {
        if (category.equals(categoryService.getGeneralCategory()) || !TrackingType.EXPENSE.equals(category.getType())) {
            return -1L;
        }

        if (category.getParent() == null && category.getParentId() != null) {
            category.setParent(categoryService.getCategoryByIdRestored(category.getParentId()));
        }

        if (category.getParent() == null) {
            category.setParent(categoryService.getGeneralCategory()); // ensures the greatest parent is the general category
        } else if (category.getParent().getCategoryId() == null || !categoryService.categoryExists(category.getParent().getCategoryId())) {
            insert(category.getParent()); // automatically inserts parent if not already in database.
        }

        Long result = categoryDao.insert(category);
        if (result != null && result > 0) {
            category.setCategoryId(result);
        }
        if (category.getParent() != null && category.getParent().getCategoryId() != null) {
            updateCategoryTree(category);
        }
        return result;
    }

    @Override
    public void deleteCategory(@NonNull Category categoryToDelete) {
        if (categoryToDelete.equals(categoryService.getGeneralCategory())) {
            return; // cannot delete general/root category
        }
        if (categoryToDelete.getChildren(false).isEmpty()) {
            categoryDao.delete(categoryToDelete);
            return;
        }

        Category categoryInDb = categoryService.getCategoryById(categoryToDelete.getCategoryId());
        if (categoryInDb == null) {
            return; // not found
        }

        Category parentOfDeleted = categoryToDelete.getParent();
        if (parentOfDeleted == null && categoryToDelete.getParentId() != null) {
            parentOfDeleted = categoryService.getCategoryByIdRestored(categoryToDelete.getParentId());
        }
        if (parentOfDeleted == null) {
            parentOfDeleted = categoryService.getGeneralCategory();
        }

        for (Category child : categoryInDb.getChildren(false)) {
            child.setParent(parentOfDeleted);
            updateCategory(child);
        }

        categoryDao.delete(categoryToDelete);
    }

    /**
     * Updates the category in database, will automatically pass root categories to updateRootCategory.
     *
     * @param category - the category to update.
     */
    @Override
    public void updateCategory(@NonNull Category category) {
        categoryDao.update(category);
    }

    @Override
    public void updateAllCategories(Collection<Category> categories) {
        categoryDao.updateAll(categories);
    }

    /**
     * This assumes the category Tree has been modified.
     * Only works when Category has its cache restored.
     * @param category - the category inside the tree (must have cache restored).
     */
    @Override
    public void updateCategoryTree(@NonNull Category category) {
        if (category.equals(categoryService.getGeneralCategory())) {
            return;
        }

        if (category.getParent() == null && category.getParentId() != null) {
            category.setParent(categoryService.getCategoryByIdRestored(category.getParentId()));
        }

        if (category.getParent() == null) {
            category.setParent(categoryService.getGeneralCategory());
        } else if (category.getParent().getCategoryId() == null || !categoryService.categoryExists(category.getParent().getCategoryId())) {
            insert(category.getParent());
        }

        Long parentId = category.getParentId();
        while (parentId != null) {
            if (parentId.equals(category.getCategoryId())) {
                throw new IllegalArgumentException("Cannot set a category as a child of its own descendant.");
            }
            Category p = categoryService.getCategoryById(parentId);
            parentId = p != null ? p.getParentId() : null;
        }

        categoryDao.update(category);
        if (category.getParent() != null) {
            categoryDao.update(category.getParent());
        }
        if (!category.getChildren(false).isEmpty()) {
            categoryDao.updateAll(category.getChildren(false));
        }
    }
}
