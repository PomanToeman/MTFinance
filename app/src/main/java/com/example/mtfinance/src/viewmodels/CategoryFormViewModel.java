package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingWriteRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CategoryFormViewModel extends ViewModel {
    private final TrackingService trackingService;
    private final TrackingWriteRepository trackingWriteRepository;
    private final Executor executor;

    private final MutableLiveData<CategoryFormFields> formFields = new MutableLiveData<>(new CategoryFormFields());

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
    private final LiveData<Boolean> isRoot;
    private final LiveData<List<CategoryWithTransactions>> categorySelection;
    private final LiveData<Category> cachedParentCategory;

    @Inject
    public CategoryFormViewModel(TrackingService trackingService, TrackingWriteRepository trackingWriteRepository, Executor executor) {
        this.trackingService = trackingService;
        this.trackingWriteRepository = trackingWriteRepository;
        this.executor = executor;
        isEditMode = Transformations.map(formFields, fields -> fields.editCategoryId != null);
        isRoot = Transformations.map(formFields, fields -> {
            if (fields.editCategoryId == null) return false;
            return trackingService.isRoot(fields.editCategoryId);
        });

        categorySelection = Transformations.switchMap(type, t -> trackingService.searchCategoriesWithType("", t));
        this.cachedParentCategory = Transformations.switchMap(formFields, fields -> {
            if (fields.parentId == null) return null;
            return new MutableLiveData<>(trackingService.getCategoryByIdRestored(fields.parentId));
        });

        clearSync();
    }

    public void setEditCategory(Long categoryId) {
        CategoryFormFields fields = getFields();
        fields.editCategoryId = categoryId;
        updateFields(fields);
        if (categoryId != null) {
            loadCategoryForEditing();
        }
    }

    public void loadCategoryForEditing() {
        executor.execute(this::loadCategoryForEditingSync);
    }

    public void loadCategoryForEditingSync() {
        CategoryFormFields fields = getFields();
        if (fields.editCategoryId == null) return;

        if (!trackingService.categoryExists(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_NOT_FOUND.getMessage());
            return;
        }
        Category category = trackingService.getCategoryByIdRestored(fields.editCategoryId);

        fields.name = category.getName();
        fields.description = category.getDescription();
        fields.parentId = category.getParentId();
        fields.monthlyBudget = category.getMonthlyBudget();
        fields.minimumBudget = category.determineMinimumBudget();
        updateFields(fields);
    }

    public void setName(String name) {
        if (name == null) return;
        CategoryFormFields fields = getFields();
        fields.name = name;
        updateFields(fields);
    }

    public void setDescription(String description) {
        CategoryFormFields fields = getFields();
        fields.description = description;
        updateFields(fields);
    }

    public void setParentId(Long parentId) {
        executor.execute(() -> setParentIdSync(parentId));
    }

    public void setParentIdSync(Long parentId) {
        CategoryFormFields fields = getFields();
        if (parentId == null) {
            fields.parentId = null;
            updateFields(fields);
            return;
        }
        if (parentId.equals(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_PARENT_SELF.getMessage());
            return;
        }

        if (!trackingService.categoryExists(parentId)) {
            setErrorMessage(MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
            return;
        }
        if (trackingService.getCategoryByIdRestored(parentId).getAncestors().size() >= Category.MAX_DEPTH) {
            setErrorMessage(MessageCli.CATEGORY_MAX_DEPTH_REACHED.getMessage(Category.MAX_DEPTH));
            return;
        }
        fields.parentId = parentId;
        updateFields(fields);
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        CategoryFormFields fields = getFields();
        if (monthlyBudget == null) {
            fields.monthlyBudget = null;
        } else if (monthlyBudget.compareTo(fields.minimumBudget) < 0) {
            fields.monthlyBudget = monthlyBudget;
            setErrorMessage(MessageCli.BELOW_MINIMUM_BUDGET.getMessage(monthlyBudget.toString(), fields.minimumBudget.toString()));
        } else {
            fields.monthlyBudget = monthlyBudget;
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

    public void deleteCategory(boolean deleteTransactions) {
        executor.execute(() -> deleteCategorySync(deleteTransactions));
    }

    public void deleteCategorySync(boolean deleteTransactions) {
        CategoryFormFields fields = getFields();
        if (fields.editCategoryId == null || !trackingService.categoryExists(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingWriteRepository.deleteCategory(fields.editCategoryId, deleteTransactions);
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

    public void saveCategory() {
        executor.execute(this::saveCategorySync);
    }

    public void saveCategorySync() {
        CategoryFormFields fields = getFields();

        try {
            validateFormSync(fields);

            setErrorMessage("");
            setSuccessMessage("");
            setIsLoading(true);

            if (fields.editCategoryId == null) {
                Category newCategory = new Category(fields.name, fields.description, fields.monthlyBudget, fields.type);
                if (fields.parentId != null) {
                    newCategory.setParentId(fields.parentId);
                }
                trackingWriteRepository.insertCategory(newCategory);
            } else {
                Category category = trackingService.getCategoryByIdRestored(fields.editCategoryId);
                if (category == null) {
                    return;
                }

                if (!trackingService.isRoot(category)) {
                    category.setName(fields.name);
                    category.setDescription(fields.description);
                    category.setMonthlyBudget(fields.monthlyBudget);
                    if (fields.parentId != null) {
                        category.setParent(trackingService.getCategoryByIdRestored(fields.parentId));
                    }
                    trackingWriteRepository.updateCategoryTree(category);
                } else {
                    category.setMonthlyBudget(fields.monthlyBudget);
                    trackingWriteRepository.updateCategory(category);
                }
            }

            setSuccessMessage(MessageCli.CATEGORY_SAVED.getMessage());
        } catch (Exception e) {
            setErrorMessage(MessageCli.CATEGORY_SAVE_FAILED.getMessage(e.getMessage()));
        } finally {
            setIsLoading(false);
            loadCategoryForEditingSync();
        }
    }

    private void validateFormSync(CategoryFormFields fields) throws IllegalArgumentException {
        if (fields.name == null || fields.name.isEmpty()) {
            throw new IllegalArgumentException(MessageCli.CATEGORY_NAME_EMPTY.getMessage());
        }

        if (trackingService.categoryNameExists(fields.name)) {
            if (fields.editCategoryId == null) {
                throw new IllegalArgumentException(MessageCli.CATEGORY_NAME_EXISTS.getMessage());
            } else {
                Category current = trackingService.getCategoryByIdRestored(fields.editCategoryId);
                if (current != null && !current.getName().equalsIgnoreCase(fields.name.trim())) {
                    throw new IllegalArgumentException(MessageCli.CATEGORY_NAME_EXISTS.getMessage());
                }
            }
        }

        TrackingUtlis.checkAmount(fields.monthlyBudget);
        if (fields.minimumBudget.compareTo(fields.monthlyBudget) > 0) {
            throw new IllegalArgumentException(MessageCli.BELOW_MINIMUM_BUDGET.getMessage(fields.monthlyBudget.toString(), fields.minimumBudget.toString()));
        }

        if (fields.parentId != null && !trackingService.categoryExists(fields.parentId)) {
            throw new IllegalArgumentException(MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
        }
    }

    public void clear() {
        executor.execute(this::clearSync);
    }

    public void clearSync() {
        CategoryFormFields fields = new CategoryFormFields();
        updateFields(fields);
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
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public LiveData<String> getSuccessMessage() {
        return successMessage;
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
