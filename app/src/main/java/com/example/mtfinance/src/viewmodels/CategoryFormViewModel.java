package com.example.mtfinance.src.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import java.math.BigDecimal;

import dagger.hilt.android.lifecycle.HiltViewModel;
import jakarta.inject.Inject;

@HiltViewModel
public class CategoryFormViewModel extends ViewModel {
    // instance fields.
    private final TrackingRepository trackingRepository;

    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<Long> parentId = new MutableLiveData<>();

    private final MutableLiveData<BigDecimal> monthlyBudget = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> minimumBudget = new MutableLiveData<>();
    private final MutableLiveData<TrackingType> type = new MutableLiveData<>();

    // logistical fields
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();


    // creating mode if null
    private Long editCategoryId = null;


    @Inject
    public CategoryFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        clear(); // set default values
    }

    /**
     * Sets the category to edit.
     * The category must already be in the database, and cannot be root.
     * @param categoryId - The category in database to edit.
     */
    public void setEditCategory(Long categoryId) {
        this.editCategoryId = categoryId;
        loadCategoryForEditing();
    }

    /**
     * Gets category and sets all fields to its values.
     * Will ignore if category is root or cannot be found.
     */
    private void loadCategoryForEditing() {
        if (editCategoryId == null) {
            return;
        }
        Category category = trackingRepository.getCategoryByIdRestored(editCategoryId);
        if (category == null || trackingRepository.isRoot(category)) {
            errorMessage.setValue("Category not found or is root (cannot edit root)");
            setEditCategory(null);
            return;
        }
        setName(category.getName());
        setDescription(category.getDescription());
        setParentId(category.getParentId());
        setMonthlyBudget(category.getMonthlyBudget());
        setMinimumBudget(category.determineMinimumBudget());

    }

    private void setMinimumBudget(BigDecimal minimumBudget) {
        this.minimumBudget.setValue(minimumBudget);
    }

    public void setName( String name) {
        if (name == null) {
            return;
        }
        this.name.setValue(name);
    }

    /**
     * Note: a default description is set if empty.
     * @param description - the decription to set (if not empty)
     */
    public void setDescription(String description) {
        this.description.setValue(TrackingUtlis.determineDescription(description));
    }

    public void setParentId(Long parentId) {
        if (parentId != null && !trackingRepository.categoryExists(parentId)) {
            return;
        }
        this.parentId.setValue(parentId);
    }

    /**
     * The given budget must be greater than or equal to the minimum budget to be set.
     * @param monthlyBudget - the monthly budget to set.
     */
    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        if (monthlyBudget.compareTo(minimumBudget.getValue()) < 0) {
            this.monthlyBudget.setValue(minimumBudget.getValue());
            return;
        }
        this.monthlyBudget.setValue(monthlyBudget);
    }
    
    // private since only Expense categories allowed.
    private void setType(TrackingType type) {
        this.type.setValue(type);
    }



    public void saveCategory() {
       try {
           validateForm();

           errorMessage.setValue("");
           successMessage.setValue("");
           isLoading.setValue(true);

           // Create Category
           if (editCategoryId == null) {
               Category newCategory = new Category(name.getValue(), description.getValue(), monthlyBudget.getValue(), type.getValue());
               if (parentId.getValue() != null) {
                   newCategory.setParentId(parentId.getValue());
               }
               trackingRepository.insertCategory(newCategory);
               setEditCategory(newCategory.getCategoryId());
           }
           // Edit Category
           else {
               Category category = trackingRepository.getCategoryByIdRestored(editCategoryId);
               if (category == null) {
                   return;
               }
               category.setName(name.getValue());
               category.setDescription(description.getValue());
               category.setMonthlyBudget(monthlyBudget.getValue());
               if (parentId.getValue() != null) {
                   category.setParent(trackingRepository.getCategoryByIdRestored(parentId.getValue()));
               }
               trackingRepository.updateCategoryTree(category);
           }

           successMessage.setValue("Category saved successfully");

       }
       catch (Exception e) {
           errorMessage.setValue("Cannot save category: " + e.getMessage());

       }
       finally {
           isLoading.setValue(false);
           
       }

    }

    private void validateForm() throws IllegalArgumentException{
        if (name.getValue() == null || name.getValue().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        // Check if name already exists (excluding the current category if editing)
        if (trackingRepository.categoryNameExists(name.getValue())) {
            if (editCategoryId == null) {
                throw new IllegalArgumentException("Category with this name already exists");
            } else {
                Category current = trackingRepository.getCategoryByIdRestored(editCategoryId);
                if (current != null && !current.getName().equalsIgnoreCase(name.getValue().trim())) {
                    throw new IllegalArgumentException("Category with this name already exists");
                }
            }
        }

        TrackingUtlis.checkAmount(monthlyBudget.getValue());
        // if parent category is set, check it exists.
        if (parentId.getValue() != null && !trackingRepository.categoryExists(parentId.getValue())) {
            throw new IllegalArgumentException("Parent category does not exist");
        }

    }

    /**
     * Clears all fields to their default values
     */
    public void clear() {
        editCategoryId = null;
        name.setValue("Name");
        description.setValue(TrackingUtlis.EMPTY_DESCRIPTION);
        parentId.setValue(null);
        monthlyBudget.setValue(BigDecimal.ONE);
        minimumBudget.setValue(BigDecimal.ZERO);
        type.setValue(TrackingType.EXPENSE);
        errorMessage.setValue("");
        successMessage.setValue("");
    }


    // PUBLIC GETTERS
    public Long getEditCategoryId() {
        return editCategoryId;
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




}
