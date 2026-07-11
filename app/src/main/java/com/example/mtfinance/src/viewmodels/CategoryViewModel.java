package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final MutableLiveData<List<CategoryWithTransactions>> allCategories = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryWithTransactions>> filteredCategories = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>();
    private final MutableLiveData<CategoryWithTransactions> selectedCategory = new MutableLiveData<>();



    @Inject
    public CategoryViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        loadAllCategories();

        if (allCategories.getValue() != null && !allCategories.getValue().isEmpty()) {
            setSelectedCategory(1L);
        }

    }

    // LIST
    private void loadAllCategories() {
         allCategories.postValue(trackingRepository.getAllCategoriesWithTransactions().getValue());

    }

    public LiveData<List<CategoryWithTransactions>> getAllCategories() {
        return allCategories;
    }

    // SEARCH
    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query.trim() : "");
    }
    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }
    public LiveData<List<CategoryWithTransactions>> getFilteredCategories() {
        return filteredCategories;
    }
    public void filterCategories() {
        String query = searchQuery.getValue();
        if (query == null || query.isEmpty()) {
            filteredCategories.setValue(allCategories.getValue());
            return;
        }

        filteredCategories.setValue(trackingRepository.searchCategories(query).getValue());
    }



    // CATEGORY DASHBOARD (for singular Category)
    public void setSelectedCategory(Long categoryId) {
        CategoryWithTransactions selectedCategory = trackingRepository.getCategoryWithTransactionsByCategoryId(categoryId);
        if (selectedCategory == null) {
            return;
        }
        selectedCategory.category = trackingRepository.getCategoryByIdRestored(categoryId);
        this.selectedCategory.setValue(selectedCategory);
    }
    public LiveData<CategoryWithTransactions> getSelectedCategory() {
        return selectedCategory;
    }










}
