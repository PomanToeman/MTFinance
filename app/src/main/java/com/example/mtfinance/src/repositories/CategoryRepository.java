package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.util.Collection;

import javax.inject.Inject;

/**
 * Concrete repository class managing category write operations and business policy validations.
 */
public class CategoryRepository {
    private final CategoryDao categoryDao;
    private final CategoryRepositoryHelper helper;

    /**
     * Constructs a CategoryRepository with required DAO and helper.
     * @param categoryDao the category Room DAO
     */
    @Inject
    public CategoryRepository(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
        this.helper = new CategoryRepositoryHelper(categoryDao);
    }

    /**
     * Inserts a category into the database.
     * Enforces business rules: cannot insert root category duplicates or invalid tracking types.
     * Throws CategoryViolationException if constraints are violated.
     * @param category - the category to be inserted
     * @return the generated category ID
     */
    public Long insert(@NonNull Category category) {
        if (category.equals(helper.getGeneralCategory()) || !TrackingType.EXPENSE.equals(category.getType())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_SAVE_FAILED.getMessage("Invalid category type or root duplication"));
        }

        if (helper.categoryNameExists(category.getName())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NAME_EXISTS.getMessage());
        }

        if (category.getParent() == null && category.getParentId() != null) {
            category.setParent(helper.getCategoryByIdRestored(category.getParentId()));
        }

        if (category.getParent() == null) {
            category.setParent(helper.getGeneralCategory());
        } else if (category.getParent().getCategoryId() == null || !helper.categoryExists(category.getParent().getCategoryId())) {
            insert(category.getParent());
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

    /**
     * Deletes a category from the database.
     * Cannot delete root categories or non-existent categories. Re-parents children if necessary.
     * Throws CategoryViolationException if attempting to delete a root category or non-existent category.
     * @param categoryToDelete - the category to delete
     */
    public void deleteCategory(@NonNull Category categoryToDelete) {
        if (categoryToDelete.getCategoryId() == null || !helper.categoryExists(categoryToDelete.getCategoryId())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }

        if (helper.isRoot(categoryToDelete)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }

        if (categoryToDelete.getChildren(false).isEmpty()) {
            categoryDao.delete(categoryToDelete);
            return;
        }

        Category categoryInDb = categoryDao.getById(categoryToDelete.getCategoryId());
        if (categoryInDb == null) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }

        Category parentOfDeleted = categoryToDelete.getParent();
        if (parentOfDeleted == null && categoryToDelete.getParentId() != null) {
            parentOfDeleted = helper.getCategoryByIdRestored(categoryToDelete.getParentId());
        }
        if (parentOfDeleted == null) {
            parentOfDeleted = helper.getGeneralCategory();
        }

        for (Category child : categoryInDb.getChildren(false)) {
            child.setParent(parentOfDeleted);
            updateCategory(child);
        }

        categoryDao.delete(categoryToDelete);
    }

    /**
     * Updates an existing category in the database.
     * Prevents modifying system root categories via standard update.
     * Throws CategoryViolationException if trying to update a root category.
     * @param category - the category to update
     */
    public void updateCategory(@NonNull Category category) {
        if (category.getCategoryId() != null && helper.isRoot(category.getCategoryId())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }
        categoryDao.update(category);
    }

    /**
     * Updates multiple categories in bulk.
     * Throws CategoryViolationException if any category is a root category.
     * @param categories - collection of categories to update
     */
    public void updateAllCategories(Collection<Category> categories) {
        for (Category c : categories) {
            if (c.getCategoryId() != null && helper.isRoot(c.getCategoryId())) {
                throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
            }
        }
        categoryDao.updateAll(categories);
    }

    /**
     * Updates the category tree structure and validates parent hierarchy.
     * Throws CategoryViolationException if root category is modified or hierarchy rules fail.
     * @param category - the category inside the tree with cache restored
     */
    public void updateCategoryTree(@NonNull Category category) {
        if (helper.isRoot(category)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }

        if (category.getParent() == null && category.getParentId() != null) {
            category.setParent(helper.getCategoryByIdRestored(category.getParentId()));
        }

        if (category.getParent() == null) {
            category.setParent(helper.getGeneralCategory());
        } else if (category.getParent().getCategoryId() == null || !helper.categoryExists(category.getParent().getCategoryId())) {
            insert(category.getParent());
        }

        helper.validateCategoryTree(category);

        categoryDao.update(category);
        if (category.getParent() != null) {
            categoryDao.update(category.getParent());
        }
        if (!category.getChildren(false).isEmpty()) {
            categoryDao.updateAll(category.getChildren(false));
        }
    }
}
