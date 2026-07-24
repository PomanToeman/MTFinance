package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final LiveData<List<CategoryWithTransactions>> allCategories;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<CategoryWithTransactions>> filteredCategories;
    private final MutableLiveData<CategoryWithTransactions> selectedCategory = new MutableLiveData<>();

    @Inject
    public CategoryViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        this.allCategories = trackingRepository.getAllCategoriesWithTransactions();

        // to automatically switch sources for filteredCategories whenever searchQuery changes
        this.filteredCategories = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return allCategories;
            }
            return trackingRepository.searchCategories(query);
        });
    }

    public LiveData<List<CategoryWithTransactions>> getAllCategories() {
        return allCategories;
    }

    // SEARCH

    /**
     * This automatically changes the filteredCategories
     * @param query - the query to search for.
     */
    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query.trim() : "");
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<List<CategoryWithTransactions>> getFilteredCategories() {
        return filteredCategories;
    }

    // CATEGORY DASHBOARD (for singular Category)
    public void setSelectedCategory(Long categoryId) {
        new Thread(() -> {
            CategoryWithTransactions selected = trackingRepository.getCategoryWithTransactionsByCategoryId(categoryId);
            if (selected != null) {
                selected.category = trackingRepository.getCategoryByIdRestored(categoryId);
                selectedCategory.postValue(selected);
            }
        }).start();
    }

    public void resetSelectedCategory() {
        selectedCategory.setValue(null);
    }

    public LiveData<CategoryWithTransactions> getSelectedCategory() {
        return selectedCategory;
    }
}
