package com.example.mtfinance.src.repositories;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;
import java.util.Collection;

import javax.inject.Inject;

/**
 * Concrete repository class managing category write operations and business policy validations.
 */
public class CategoryRepository {
    private final AppDatabase appDatabase;
    private final CategoryDao categoryDao;
    private final CategoryQueries helper;

    /**
     * Constructs a CategoryRepository with required AppDatabase, DAO, and helper.
     * @param appDatabase the Room AppDatabase instance for atomic transaction management
     * @param categoryDao the category Room DAO
     */
    @Inject
    public CategoryRepository(AppDatabase appDatabase, CategoryDao categoryDao) {
        this.appDatabase = appDatabase;
        this.categoryDao = categoryDao;
        this.helper = new CategoryQueries(categoryDao);
    }

    /**
     * Convenience constructor when AppDatabase is not directly provided.
     * @param categoryDao the category Room DAO
     */
    public CategoryRepository(CategoryDao categoryDao) {
        this(null, categoryDao);
    }

    /**
     * Inserts a category into the database atomically.
     * Enforces business rules: cannot insert root category duplicates or invalid tracking types.
     * @param category - the category to be inserted
     * @return the generated category ID
     * @throws CategoryViolationException if constraints are violated
     */
    public Long insert(@NonNull Category category) throws CategoryViolationException {
        if (appDatabase != null) {
            return appDatabase.runInTransaction(() -> insertInternal(category));
        } else {
            return insertInternal(category);
        }
    }

    private Long insertInternal(@NonNull Category category) {
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
            insertInternal(category.getParent());
        }

        Long result = categoryDao.insert(category);
        if (result != null && result > 0) {
            category.setCategoryId(result);
        }
        if (category.getParent() != null && category.getParent().getCategoryId() != null) {
            updateCategoryTreeInternal(category);
        }
        return result;
    }

    /**
     * Deletes a category from the database atomically.
     * Cannot delete root categories or non-existent categories. Re-parents children if necessary.
     * @param categoryToDelete - the category to delete
     * @throws CategoryViolationException if attempting to delete a root category or non-existent category
     */
    public void deleteCategory(@NonNull Category categoryToDelete) throws CategoryViolationException {
        if (appDatabase != null) {
            appDatabase.runInTransaction(() -> deleteCategoryInternal(categoryToDelete));
        } else {
            deleteCategoryInternal(categoryToDelete);
        }
    }

    private void deleteCategoryInternal(@NonNull Category categoryToDelete) {
        if (categoryToDelete.getCategoryId() == null || !helper.categoryExists(categoryToDelete.getCategoryId())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }

        if (helper.isRoot(categoryToDelete)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }

        Category categoryRestored = helper.getCategoryByIdRestored(categoryToDelete.getCategoryId());
        if (categoryRestored == null) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }

        if (categoryRestored.getChildren(false).isEmpty()) {
            categoryDao.delete(categoryRestored);
            return;
        }

        Category parentOfDeleted = categoryRestored.getParent();
        if (parentOfDeleted == null && categoryRestored.getParentId() != null) {
            parentOfDeleted = helper.getCategoryByIdRestored(categoryRestored.getParentId());
        }
        if (parentOfDeleted == null) {
            parentOfDeleted = helper.getGeneralCategory();
        }

        for (Category child : categoryRestored.getChildren(false)) {
            child.setParent(parentOfDeleted);
            updateCategoryInternal(child);
        }

        categoryDao.delete(categoryRestored);
    }

    /**
     * Updates an existing category in the database.
     * Prevents modifying system root categories or non-existent categories.
     * @param category - the category to update
     * @throws CategoryViolationException if trying to update a root category or non-existent category
     */
    public void updateCategory(@NonNull Category category) throws CategoryViolationException {
        updateCategoryInternal(category);
    }

    private void updateCategoryInternal(@NonNull Category category) {
        if (category.getCategoryId() == null || !helper.categoryExists(category.getCategoryId())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        if (helper.isRoot(category.getCategoryId())) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }
        categoryDao.update(category);
    }

    /**
     * Explicitly updates the monthly budget of a system root category identified by TrackingType.
     * @param type the tracking type of the root category
     * @param newBudget the new monthly budget amount
     * @throws CategoryViolationException if root category not found
     */
    public void updateRootCategoryBudget(@NonNull TrackingType type, @NonNull BigDecimal newBudget) throws CategoryViolationException {
        TrackingUtlis.checkAmount(newBudget);
        Category root = helper.getRootCategoryByType(type);
        if (root == null || !helper.isRoot(root)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_NOT_FOUND.getMessage());
        }
        root.setMonthlyBudget(newBudget);
        categoryDao.update(root);
    }

    /**
     * Explicitly updates the monthly budget of a system root category identified by ID.
     * @param categoryId the category ID
     * @param newBudget the new monthly budget amount
     * @throws CategoryViolationException if category not found or not a root category
     */
    public void updateRootCategoryBudget(@NonNull Long categoryId, @NonNull BigDecimal newBudget) throws CategoryViolationException {
        TrackingUtlis.checkAmount(newBudget);
        Category category = categoryDao.getById(categoryId);
        if (category == null || !helper.isRoot(category)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }
        category.setMonthlyBudget(newBudget);
        categoryDao.update(category);
    }

    /**
     * Updates multiple categories in bulk atomically.
     * @param categories - collection of categories to update
     * @throws CategoryViolationException if any category is a root category
     */
    public void updateAllCategories(Collection<Category> categories) throws CategoryViolationException {
        if (appDatabase != null) {
            appDatabase.runInTransaction(() -> updateAllCategoriesInternal(categories));
        } else {
            updateAllCategoriesInternal(categories);
        }
    }

    private void updateAllCategoriesInternal(Collection<Category> categories) {
        for (Category c : categories) {
            if (c.getCategoryId() != null && helper.isRoot(c.getCategoryId())) {
                throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
            }
        }
        categoryDao.updateAll(categories);
    }

    /**
     * Updates the category tree structure and validates parent hierarchy atomically.
     * @param category - the category inside the tree with cache restored
     * @throws CategoryViolationException if root category is modified or hierarchy rules fail
     */
    public void updateCategoryTree(@NonNull Category category) throws CategoryViolationException {
        if (appDatabase != null) {
            appDatabase.runInTransaction(() -> updateCategoryTreeInternal(category));
        } else {
            updateCategoryTreeInternal(category);
        }
    }

    private void updateCategoryTreeInternal(@NonNull Category category) {
        if (helper.isRoot(category)) {
            throw new CategoryViolationException(MessageCli.CATEGORY_ROOT_EDIT_DENIED.getMessage());
        }

        if (category.getParent() == null && category.getParentId() != null) {
            category.setParent(helper.getCategoryByIdRestored(category.getParentId()));
        }

        if (category.getParent() == null) {
            category.setParent(helper.getGeneralCategory());
        } else if (category.getParent().getCategoryId() == null || !helper.categoryExists(category.getParent().getCategoryId())) {
            insertInternal(category.getParent());
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
