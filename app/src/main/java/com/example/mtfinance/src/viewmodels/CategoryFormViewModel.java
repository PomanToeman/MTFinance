package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CategoryFormViewModel extends ViewModel {
    // instance fields.
    private final TrackingRepository trackingRepository;
    private final Executor executor;

    private final MutableLiveData<CategoryFormFields> formFields = new MutableLiveData<>(new CategoryFormFields());

    private final LiveData<String> name = Transformations.map(formFields, fields -> fields.name);
    private final LiveData<String> description = Transformations.map(formFields, fields -> fields.description);
    private final LiveData<Long> parentId = Transformations.map(formFields, fields -> fields.parentId);
    private final LiveData<BigDecimal> monthlyBudget = Transformations.map(formFields, fields -> fields.monthlyBudget);
    private final LiveData<BigDecimal> minimumBudget = Transformations.map(formFields, fields -> fields.minimumBudget);
    private final LiveData<TrackingType> type = Transformations.map(formFields, fields -> fields.type);

    // logistical fields
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");
    private final LiveData<Boolean> isEditMode;
    private final LiveData<Boolean> isRoot;
    private final LiveData<List<CategoryWithTransactions>> categorySelection;
    private final LiveData<Category> cachedParentCategory;


    @Inject
    public CategoryFormViewModel(TrackingRepository trackingRepository, Executor executor) {
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        isEditMode = Transformations.map(formFields, fields -> fields.editCategoryId != null);
        isRoot = Transformations.map(formFields, fields -> {
            if (fields.editCategoryId == null) return false;
            return trackingRepository.isRoot(fields.editCategoryId);
        });

        categorySelection = Transformations.switchMap(type, type -> trackingRepository.searchCategoriesWithType("", type));
        this.cachedParentCategory = Transformations.switchMap(formFields, fields -> {
            if (fields.parentId == null) return null;
            return new MutableLiveData<>(trackingRepository.getCategoryByIdRestored(fields.parentId));
        });

        clearSync(); // set default values
    }

    /**
     * Sets the category to edit.
     * The category must already be in the database.
     * @param categoryId - The category in database to edit.
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
     * Gets category and sets all fields to its values.
     * Will ignore if category cannot be found.
     */
    public void loadCategoryForEditing() {
        executor.execute(this::loadCategoryForEditingSync);
    }

    public void loadCategoryForEditingSync() {
        CategoryFormFields fields = getFields();
        if (fields.editCategoryId == null) return;

        if (!trackingRepository.categoryExists(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_NOT_FOUND.getMessage());
            return;
        }
        Category category = trackingRepository.getCategoryByIdRestored(fields.editCategoryId);
        
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

    /**
     * Note: a default description is set if empty.
     * @param description - the description to be set (if not empty)
     */
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


        if (!trackingRepository.categoryExists(parentId)) {
            setErrorMessage(MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
            return;
        }
        if (trackingRepository.getCategoryByIdRestored(parentId).getAncestors().size() >= Category.MAX_DEPTH) {
            setErrorMessage(MessageCli.CATEGORY_MAX_DEPTH_REACHED.getMessage(Category.MAX_DEPTH));
            return;
        }
        fields.parentId = parentId;
        updateFields(fields);
    }

    /**
     * The given budget must be greater than or equal to the minimum budget to be set.
     * @param monthlyBudget - the monthly budget to set.
     */
    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        CategoryFormFields fields = getFields();
        if (monthlyBudget == null) {
            fields.monthlyBudget = null;
        }
        else if (monthlyBudget.compareTo(fields.minimumBudget) < 0) {
            fields.monthlyBudget = monthlyBudget;
            setErrorMessage(MessageCli.BELOW_MINIMUM_BUDGET.getMessage(monthlyBudget.toString(), fields.minimumBudget.toString()));
        } else {
            fields.monthlyBudget = monthlyBudget;
            setErrorMessage("");
        }
        updateFields(fields);
    }
    
    // private since only Expense categories allowed.
    private void setType(TrackingType type) {
        CategoryFormFields fields = getFields();
        fields.type = type;
        updateFields(fields);
    }


    // private setters
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
     * WIll delete the category that is currently being edited.
     * This CANNOT be undone!!
     * deletes all of its transactions if permitted (will move to root category if permitted).
     */
    public void deleteCategory(boolean deleteTransactions) {
        executor.execute(() -> deleteCategorySync(deleteTransactions));
    }

    public void deleteCategorySync(boolean deleteTransactions) {
        CategoryFormFields fields = getFields();
        if (fields.editCategoryId == null || !trackingRepository.categoryExists(fields.editCategoryId)) {
            setErrorMessage(MessageCli.CATEGORY_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingRepository.deleteCategory(fields.editCategoryId, deleteTransactions);
            clearSync();
            setSuccessMessage(MessageCli.CATEGORY_DELETED.getMessage());
            setErrorMessage("");
        }
        catch (Exception e) {
            setErrorMessage(MessageCli.CATEGORY_DELETE_FAILED.getMessage(e.getMessage()));
            setSuccessMessage("");
        }
        finally {
            setIsLoading(false);
        }
    }

    /**
     * This will create or update existing category in edit mode in the database.
     * All fields must be valid before saving.
     *
     */

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

           // Create Category
           if (fields.editCategoryId == null) {
               Category newCategory = new Category(fields.name, fields.description, fields.monthlyBudget, fields.type);
               if (fields.parentId != null) {
                   newCategory.setParentId(fields.parentId);
               }
               trackingRepository.insertCategory(newCategory);

           }
           // Edit Category
           else {
               Category category = trackingRepository.getCategoryByIdRestored(fields.editCategoryId);
               if (category == null) {
                   return;
               }

               if (!trackingRepository.isRoot(category)) {
                   category.setName(fields.name);
                   category.setDescription(fields.description);
                   category.setMonthlyBudget(fields.monthlyBudget);
                   if (fields.parentId != null) {
                       category.setParent(trackingRepository.getCategoryByIdRestored(fields.parentId));
                   }
                   trackingRepository.updateCategoryTree(category);
               } else {
                   category.setMonthlyBudget(fields.monthlyBudget);
                   trackingRepository.updateCategory(category);
               }
           }

           setSuccessMessage(MessageCli.CATEGORY_SAVED.getMessage());

       }

       catch (Exception e) {
           setErrorMessage(MessageCli.CATEGORY_SAVE_FAILED.getMessage(e.getMessage()));

       }
       finally {
           setIsLoading(false);
           
       }

    }

    /**
     * Checks if the fields are filled out correctly.
     * @throws IllegalArgumentException
     */
    private void validateFormSync(CategoryFormFields fields) throws IllegalArgumentException{
        if (fields.name == null || fields.name.isEmpty()) {
            throw new IllegalArgumentException(MessageCli.CATEGORY_NAME_EMPTY.getMessage());
        }

        // Check if name already exists (excluding the current category if editing)
        if (trackingRepository.categoryNameExists(fields.name)) {
            if (fields.editCategoryId == null) {
                throw new IllegalArgumentException(MessageCli.CATEGORY_NAME_EXISTS.getMessage());
            } else {
                Category current = trackingRepository.getCategoryByIdRestored(fields.editCategoryId);
                if (current != null && !current.getName().equalsIgnoreCase(fields.name.trim())) {
                    throw new IllegalArgumentException(MessageCli.CATEGORY_NAME_EXISTS.getMessage());
                }
            }
        }

        TrackingUtlis.checkAmount(fields.monthlyBudget);
        if (fields.minimumBudget.compareTo(fields.monthlyBudget) > 0) {
            throw new IllegalArgumentException(MessageCli.BELOW_MINIMUM_BUDGET.getMessage(fields.monthlyBudget.toString(), fields.minimumBudget.toString()));
        }
        
        // if parent category is set, check it exists.
        if (fields.parentId != null && !trackingRepository.categoryExists(fields.parentId)) {
            throw new IllegalArgumentException(MessageCli.CATEGORY_PARENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Clears all fields to their default values
     */
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


    // PUBLIC GETTERS
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
