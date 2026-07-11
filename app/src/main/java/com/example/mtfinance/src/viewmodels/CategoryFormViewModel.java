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

public class CategoryFormViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;

    private final MutableLiveData<String> name = new MutableLiveData<>("name");
    private final MutableLiveData<String> description = new MutableLiveData<>("description");
    private final MutableLiveData<Long> parentId = new MutableLiveData<>();

    private final MutableLiveData<BigDecimal> monthlyBudget = new MutableLiveData<>(BigDecimal.ONE);
    private final MutableLiveData<BigDecimal> minimumBudget = new MutableLiveData<>(BigDecimal.ONE);
    private final MutableLiveData<TrackingType> type = new MutableLiveData<>(TrackingType.EXPENSE);

    // logistical fields
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");


    // creating mode if null
    private Long editCategoryId = null;



    public CategoryFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

    public void setEditCategory(Long categoryId) {
        this.editCategoryId = categoryId;
        loadCategoryForEditing();
    }

    private void loadCategoryForEditing() {
        if (editCategoryId == null) {
            return;
        }
        Category category = trackingRepository.getCategoryByIdRestored(editCategoryId);
        if (category == null) {
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
        if (name == null || name.isEmpty()) {
            return;
        }
        this.name.setValue(name);
    }

    public void setDescription(String description) {
        this.description.setValue(TrackingUtlis.determineDescription(description));
    }

    public void setParentId(Long parentId) {
        if (trackingRepository.getCategoryByIdRestored(parentId) == null) {
            return;
        }
        this.parentId.setValue(parentId);
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        if (monthlyBudget.compareTo(minimumBudget.getValue()) < 0) {
            this.monthlyBudget.setValue(minimumBudget.getValue());
            return;
        }
        this.monthlyBudget.setValue(monthlyBudget);
    }


    public void saveCategory() {
       try {
           validateForm();
           isLoading.setValue(true);
           if (editCategoryId == null) {
               Category newCategory = new Category(name.getValue(), description.getValue(), monthlyBudget.getValue(), type.getValue());
               if (parentId.getValue() != null) {
                   newCategory.setParentId(parentId.getValue());
               }
               trackingRepository.insertCategory(newCategory);
           }
           else {
               Category category = trackingRepository.getCategoryByIdRestored(editCategoryId);
               if (category == null) {
                   return;
               }
               category.setName(name.getValue());
               category.setDescription(description.getValue());
               category.setMonthlyBudget(monthlyBudget.getValue());
               category.setParent(trackingRepository.getCategoryByIdRestored(parentId.getValue()));
               trackingRepository.updateCategoryTree(category);
           }

       }
       catch (Exception e) {
           errorMessage.setValue("Cannot save category: " + e.getMessage());
           successMessage.setValue("");
       }
       finally {
           isLoading.setValue(false);
           successMessage.setValue("Category saved successfully");
       }

    }

    public void validateForm() throws IllegalArgumentException{
        if (name.getValue() == null || name.getValue().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        TrackingUtlis.checkAmount(monthlyBudget.getValue());
        if (trackingRepository.getCategoryByIdRestored(parentId.getValue()) == null) {
            throw new IllegalArgumentException("Parent category does not exist");
        }

    }


    // public getters
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
