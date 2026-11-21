package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel responsible for category creation, editing, parent hierarchy configuration, and deletion forms.
 */
@HiltViewModel
public class CategoryFormViewModel extends ViewModel {

    public enum CategoryErrorFields {
        NAME, DESCRIPTION, PARENT_ID, MONTHLY_BUDGET, MINIMUM_BUDGET, TYPE
    }

    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final TrackingRepository trackingRepository;
    private final SystemService systemService;
    private final Executor executor;

    private final MutableLiveData<CategoryFormFields> formFields = new MutableLiveData<>(new CategoryFormFields());
    private final MutableLiveData<Map<CategoryErrorFields, String>> fieldErrors = new MutableLiveData<>(new EnumMap<>(CategoryErrorFields.class));

    private final LiveData<String> name = Transformations.map(formFields, fields -> fields.name);
    private final LiveData<String> description = Transformations.map(formFields, fields -> fields.description);
    private final LiveData<Long> parentId = Transformations.map(formFields, fields -> fields.parentId);
    private final LiveData<BigDecimal> monthlyBudget = Transformations.map(formFields, fields -> fields.monthlyBudget);
    private final LiveData<BigDecimal> minimumBudget = Transformations.map(formFields, fields -> fields.minimumBudget);
    private final LiveData<TrackingType> type = Transformations.map(formFields, fields -> fields.type);

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");
    private final LiveData<Boolean> isEditMode;
    private final MutableLiveData<Boolean> isRoot = new MutableLiveData<>(false);
    private final LiveData<List<CategoryWithTransactions>> categorySelection;
    private final MutableLiveData<Category> cachedParentCategory = new MutableLiveData<>(null);
    private final LiveData<CurrencyFormat> currencyFormat;

    @Inject
    public CategoryFormViewModel(CategoryService categoryService, TrackingService trackingService, TrackingRepository trackingRepository, SystemService systemService, Executor executor) {
        this.categoryService = categoryService;
        this.trackingService = trackingService;
        this.trackingRepository = trackingRepository;
        this.systemService = systemService;
        this.executor = executor;
        isEditMode = Transformations.map(formFields, fields -> fields.editCategoryId != null);

        if (systemService != null) {
            this.currencyFormat = Transformations.map(systemService.getPreferencesLive(), prefs -> {
                if (prefs == null || prefs.getCurrencyFormat() == null) {
                    return CurrencyFormat.NZD;
                }
                return CurrencyFormat.fromString(prefs.getCurrencyFormat());
            });
        } else {
            this.currencyFormat = new MutableLiveData<>(CurrencyFormat.NZD);
        }

        categorySelection = Transformations.switchMap(type, t -> trackingService.searchCategoriesWithType("", t));

        clearSync();
    }

    /**
     * Overloaded constructor for callers and tests without SystemService.
     */
    public CategoryFormViewModel(CategoryService categoryService, TrackingService trackingService, TrackingRepository trackingRepository, Executor executor) {
        this(categoryService, trackingService, trackingRepository, null, executor);
    }

    /**
     * Resets all field-specific error messages in the map.
     */
    public void resetFieldErrors() {
        Map<CategoryErrorFields, String> errors = new EnumMap<>(CategoryErrorFields.class);
        for (CategoryErrorFields field : CategoryErrorFields.values()) {
            errors.put(field, "");
        }
        updateLiveData(this.fieldErrors, errors);
    }

    /**
     * Checks if any field error exists in the current LiveData error map.
     * @return true if at least one field has a non-empty error message
     */
    public boolean hasFieldErrors() {
        return hasFieldErrors(fieldErrors.getValue());
    }

    /**
     * Checks if any field error exists in the given error map.
     * @param errors the error map to check
     * @return true if at least one field has a non-empty error message
     */
    public boolean hasFieldErrors(Map<CategoryErrorFields, String> errors) {
        if (errors == null || errors.isEmpty()) return false;
        for (String error : errors.values()) {
            if (error != null && !error.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setFieldError(CategoryErrorFields field, String message) {
        Map<CategoryErrorFields, String> currentMap = fieldErrors.getValue();
        Map<CategoryErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(CategoryErrorFields.class);
        newMap.put(field, message != null ? message : "");
        updateLiveData(this.fieldErrors, newMap);
    }

    /**
     * Sets the category ID for editing and loads its attributes.
     * @param categoryId - the category ID to edit, or null for creation
     */
    public void setEditCategory(Long categoryId) {
        CategoryFormFields fields = getFields();
        fields.editCategoryId = categoryId;
        updateFields(fields);
        if (categoryId != null) {
            loadCategoryForEditing();
        }
    }

    /**
     * Asynchronously loads category details for editing.
     */
    public void loadCategoryForEditing() {
        executor.execute(this::loadCategoryForEditingSync);
    }

    /**
     * Synchronously loads category details for editing.
     */
    public void loadCategoryForEditingSync() {
        CategoryFormFields fields = getFields();
        if (fields.editCategoryId == null) return;

        if (!categoryService.categoryExists(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_NOT_FOUND.getMessage());
            return;
        }
        Category category = categoryService.getCategoryByIdRestored(fields.editCategoryId);

        fields.name = category.getName();
        fields.description = category.getDescription();
        fields.parentId = category.getParentId();
        fields.monthlyBudget = category.getMonthlyBudget();
        fields.minimumBudget = category.determineMinimumBudget();
        fields.type = category.getType();
        updateFields(fields);
    }

    /**
     * Sets the category name field.
     * @param name - category name
     */
    public void setName(String name) {
        CategoryFormFields fields = getFields();
        fields.name = name != null ? name : "";
        updateFields(fields);
        if (fields.name.trim().isEmpty()) {
            setFieldError(CategoryErrorFields.NAME, MessageCli.CATEGORY_NAME_EMPTY.getMessage());
        } else {
            setFieldError(CategoryErrorFields.NAME, "");
        }
    }

    /**
     * Sets the category description field.
     * @param description - category description
     */
    public void setDescription(String description) {
        CategoryFormFields fields = getFields();
        fields.description = description;
        updateFields(fields);
        setFieldError(CategoryErrorFields.DESCRIPTION, "");
    }

    /**
     * Sets the tracking type for the category.
     * @param type - tracking type
     */
    public void setType(TrackingType type) {
        CategoryFormFields fields = getFields();
        fields.type = type;
        updateFields(fields);
        if (type == null) {
            setFieldError(CategoryErrorFields.TYPE, "Type cannot be empty.");
        } else {
            setFieldError(CategoryErrorFields.TYPE, "");
        }
    }

    /**
     * Sets the parent category ID asynchronously.
     * @param parentId - parent category ID
     */
    public void setParentId(Long parentId) {
        executor.execute(() -> setParentIdSync(parentId));
    }

    /**
     * Sets the parent category ID synchronously with hierarchy depth validation.
     * @param parentId - parent category ID
     */
    public void setParentIdSync(Long parentId) {
        CategoryFormFields fields = getFields();
        fields.parentId = parentId;
        updateFields(fields);

        if (parentId == null) {
            setFieldError(CategoryErrorFields.PARENT_ID, "");
            return;
        }
        if (parentId.equals(fields.editCategoryId)) {
            setFieldError(CategoryErrorFields.PARENT_ID, MessageCli.CATEGORY_PARENT_SELF.getMessage());
            setErrorMessage(MessageCli.CATEGORY_PARENT_SELF.getMessage());
            return;
        }

        if (!categoryService.categoryExists(parentId)) {
            setFieldError(CategoryErrorFields.PARENT_ID, MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
            setErrorMessage(MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
            return;
        }
        Category parentCat = categoryService.getCategoryByIdRestored(parentId);
        if (parentCat != null && parentCat.getAncestors().size() >= Category.MAX_DEPTH) {
            setFieldError(CategoryErrorFields.PARENT_ID, MessageCli.CATEGORY_MAX_DEPTH_REACHED.getMessage(Category.MAX_DEPTH));
            setErrorMessage(MessageCli.CATEGORY_MAX_DEPTH_REACHED.getMessage(Category.MAX_DEPTH));
            return;
        }
        setFieldError(CategoryErrorFields.PARENT_ID, "");
    }

    /**
     * Sets the monthly budget field with minimum budget validation.
     * @param monthlyBudget - monthly budget
     */
    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        CategoryFormFields fields = getFields();
        if (monthlyBudget == null) {
            fields.monthlyBudget = null;
            setFieldError(CategoryErrorFields.MONTHLY_BUDGET, MessageCli.INVALID_AMOUNT.getMessage("null"));
        } else if (monthlyBudget.compareTo(fields.minimumBudget) < 0) {
            fields.monthlyBudget = monthlyBudget;
            setFieldError(CategoryErrorFields.MONTHLY_BUDGET, MessageCli.BELOW_MINIMUM_BUDGET.getMessage(monthlyBudget.toString(), fields.minimumBudget.toString()));
            setErrorMessage(MessageCli.BELOW_MINIMUM_BUDGET.getMessage(monthlyBudget.toString(), fields.minimumBudget.toString()));
        } else {
            fields.monthlyBudget = monthlyBudget;
            setFieldError(CategoryErrorFields.MONTHLY_BUDGET, "");
            setErrorMessage("");
        }
        updateFields(fields);
    }

    private void setIsLoading(boolean isLoading) {
        updateLiveData(this.isLoading, isLoading);
    }

    private void setErrorMessage(String errorMessage) {
        updateLiveData(this.errorMessage, errorMessage);
    }

    private void setSuccessMessage(String successMessage) {
        updateLiveData(this.successMessage, successMessage);
    }

    /**
     * Deletes the currently edited category asynchronously.
     * @param deleteTransactions - whether to delete transactions associated with this category
     */
    public void deleteCategory(boolean deleteTransactions) {
        executor.execute(() -> deleteCategorySync(deleteTransactions));
    }

    /**
     * Deletes the currently edited category synchronously.
     * @param deleteTransactions - whether to delete transactions associated with this category
     */
    public void deleteCategorySync(boolean deleteTransactions) {
        CategoryFormFields fields = getFields();
        if (fields.editCategoryId == null || !categoryService.categoryExists(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingRepository.deleteCategory(fields.editCategoryId, deleteTransactions);
            clearSync();
            setSuccessMessage(MessageCli.CATEGORY_DELETED.getMessage());
            setErrorMessage("");
        } catch (Exception e) {
            setErrorMessage(MessageCli.CATEGORY_DELETE_FAILED.getMessage(e.getMessage()));
            setSuccessMessage("");
        } finally {
            setIsLoading(false);
        }
    }

    /**
     * Saves the category form asynchronously.
     */
    public void saveCategory() {
        executor.execute(this::saveCategorySync);
    }

    /**
     * Saves the category form synchronously.
     */
    public void saveCategorySync() {
        CategoryFormFields fields = getFields();

        try {
            Map<CategoryErrorFields, String> errors = validateFormSync(fields);

            if (hasFieldErrors(errors)) {
                setErrorMessage(MessageCli.FORM_INCOMPLETE.getMessage("Please fix form field errors"));
                setSuccessMessage("");
                return;
            }

            setErrorMessage("");
            setSuccessMessage("");
            setIsLoading(true);

            if (fields.editCategoryId == null) {
                Category newCategory = new Category(fields.name, fields.description, fields.monthlyBudget, fields.type);
                if (fields.parentId != null) {
                    newCategory.setParentId(fields.parentId);
                }
                trackingRepository.insertCategory(newCategory);
            } else {
                Category category = categoryService.getCategoryByIdRestored(fields.editCategoryId);
                if (category == null) {
                    return;
                }

                if (!categoryService.isRoot(category)) {
                    category.setName(fields.name);
                    category.setDescription(fields.description);
                    category.setMonthlyBudget(fields.monthlyBudget);
                    if (fields.parentId != null) {
                        category.setParent(categoryService.getCategoryByIdRestored(fields.parentId));
                    }
                    trackingRepository.updateCategoryTree(category);
                } else {
                    trackingRepository.updateRootCategoryBudget(category.getCategoryId(), fields.monthlyBudget);
                }
            }

            setSuccessMessage(MessageCli.CATEGORY_SAVED.getMessage());
            setErrorMessage("");
            if (fields.editCategoryId != null) {
                loadCategoryForEditingSync();
            }
        } catch (Exception e) {
            setErrorMessage(MessageCli.CATEGORY_SAVE_FAILED.getMessage(e.getMessage()));
            setSuccessMessage("");
        } finally {
            setIsLoading(false);

        }
    }

    private Map<CategoryErrorFields, String> validateFormSync(CategoryFormFields fields) {
        Map<CategoryErrorFields, String> errors = new EnumMap<>(CategoryErrorFields.class);
        for (CategoryErrorFields field : CategoryErrorFields.values()) {
            errors.put(field, "");
        }

        try {
            TrackingUtlis.checkAmount(fields.monthlyBudget);
        } catch (Exception e) {
            errors.put(CategoryErrorFields.MONTHLY_BUDGET, e.getMessage());
        }
        if (fields.minimumBudget != null && fields.minimumBudget.compareTo(fields.monthlyBudget) > 0) {
            errors.put(CategoryErrorFields.MONTHLY_BUDGET, MessageCli.BELOW_MINIMUM_BUDGET.getMessage(fields.monthlyBudget.toString(), fields.minimumBudget.toString()));
        }

        if (fields.editCategoryId != null && categoryService.isRoot(fields.editCategoryId)) {
            updateLiveData(this.fieldErrors, errors);
            return errors;
        }

        if (fields.name == null || fields.name.trim().isEmpty()) {
            errors.put(CategoryErrorFields.NAME, MessageCli.CATEGORY_NAME_EMPTY.getMessage());
        } else if (categoryService.categoryNameExists(fields.name)) {
            if (fields.editCategoryId == null) {
                errors.put(CategoryErrorFields.NAME, MessageCli.CATEGORY_NAME_EXISTS.getMessage());
            } else {
                Category current = categoryService.getCategoryByIdRestored(fields.editCategoryId);
                if (current != null && !current.getName().equalsIgnoreCase(fields.name.trim())) {
                    errors.put(CategoryErrorFields.NAME, MessageCli.CATEGORY_NAME_EXISTS.getMessage());
                }
            }
        }

        if (fields.parentId != null) {
            if (fields.parentId.equals(fields.editCategoryId)) {
                errors.put(CategoryErrorFields.PARENT_ID, MessageCli.CATEGORY_PARENT_SELF.getMessage());
            } else if (!categoryService.categoryExists(fields.parentId)) {
                errors.put(CategoryErrorFields.PARENT_ID, MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
            }
        }

        if (fields.type == null) {
            errors.put(CategoryErrorFields.TYPE, "Type cannot be empty.");
        }

        updateLiveData(this.fieldErrors, errors);
        return errors;
    }

    /**
     * Clears form fields asynchronously.
     */
    public void clear() {
        executor.execute(this::clearSync);
    }

    /**
     * Clears form fields synchronously.
     */
    public void clearSync() {
        CategoryFormFields fields = new CategoryFormFields();
        updateFields(fields);
        resetFieldErrors();
        setErrorMessage("");
        setSuccessMessage("");
        setIsLoading(false);
    }

    private CategoryFormFields getFields() {
        CategoryFormFields fields = formFields.getValue();
        return fields != null ? fields.copy() : new CategoryFormFields();
    }

    private void updateFields(CategoryFormFields fields) {
        updateLiveData(formFields, fields);
        if (executor != null) {
            executor.execute(() -> {
                boolean root = fields.editCategoryId != null && categoryService.isRoot(fields.editCategoryId);
                updateLiveData(isRoot, root);

                Category parent = fields.parentId != null ? categoryService.getCategoryByIdRestored(fields.parentId) : null;
                updateLiveData(cachedParentCategory, parent);
            });
        } else {
            boolean root = fields.editCategoryId != null && categoryService.isRoot(fields.editCategoryId);
            updateLiveData(isRoot, root);

            Category parent = fields.parentId != null ? categoryService.getCategoryByIdRestored(fields.parentId) : null;
            updateLiveData(cachedParentCategory, parent);
        }
    }

    private <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            liveData.setValue(value);
        } catch (Exception e) {
            liveData.postValue(value);
        }
    }

    private static class CategoryFormFields {
        public Long editCategoryId = null;
        String name = "Name";
        String description = TrackingUtlis.EMPTY_DESCRIPTION;
        Long parentId = null;
        BigDecimal monthlyBudget = BigDecimal.ONE;
        BigDecimal minimumBudget = BigDecimal.ZERO;
        TrackingType type = TrackingType.EXPENSE;

        CategoryFormFields copy() {
            CategoryFormFields copy = new CategoryFormFields();
            copy.editCategoryId = this.editCategoryId;
            copy.name = this.name;
            copy.description = this.description;
            copy.parentId = this.parentId;
            copy.monthlyBudget = this.monthlyBudget;
            copy.minimumBudget = this.minimumBudget;
            copy.type = this.type;
            return copy;
        }
    }

    public Long getEditCategoryId() {
        CategoryFormFields fields = formFields.getValue();
        return fields != null ? fields.editCategoryId : null;
    }
    public LiveData<String> getName() {
        return name;
    }
    public LiveData<String> getDescription() {
        return description;
    }
    public LiveData<Long> getParentId() {
        return parentId;
    }
    public LiveData<BigDecimal> getMonthlyBudget() {
        return monthlyBudget;
    }
    public LiveData<BigDecimal> getMinimumBudget() {
        return minimumBudget;
    }
    public LiveData<TrackingType> getType() {
        return type;
    }
    public LiveData<CurrencyFormat> getCurrencyFormat() {
        return currencyFormat;
    }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    public LiveData<Map<CategoryErrorFields, String>> getFieldErrors() {
        return fieldErrors;
    }

    public LiveData<Boolean> IsEditMode() {
        return isEditMode;
    }
    public LiveData<List<CategoryWithTransactions>> getCategorySelection() {
        return categorySelection;
    }
    public LiveData<Category> getCachedParentCategory() {
        return cachedParentCategory;
    }
    public LiveData<Boolean> getIsRoot() {
        return isRoot;
    }
}
