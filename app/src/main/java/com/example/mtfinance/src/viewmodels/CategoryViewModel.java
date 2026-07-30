package com.example.mtfinance.src.viewmodels;


import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final LiveData<List<CategoryWithTransactions>> allCategories;
    @NonNull
    private final MutableLiveData<SearchCriteria> searchCriteria = new MutableLiveData<>(new SearchCriteria("", null));
    private final LiveData<List<CategoryWithTransactions>> filteredCategories;
    private final MutableLiveData<CategoryWithTransactions> selectedCategory = new MutableLiveData<>();
    private final LiveData<Long> updateTrigger;



    @Inject
    public CategoryViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        this.allCategories = trackingRepository.getAllCategoriesWithTransactions();

        // to automatically switch sources for filteredCategories whenever searchQuery changes
        this.filteredCategories = Transformations.switchMap(searchCriteria, searchCriteria -> {

            if (searchCriteria == null || (searchCriteria.getQuery() == null || searchCriteria.getQuery().trim().isEmpty()) && searchCriteria.getTypeFilter() == null) {
                return allCategories;
            }

            return trackingRepository.searchCategoriesWithType(searchCriteria.getTrimmedQuery(), searchCriteria.getTypeFilter());
        });
        this.updateTrigger = Transformations.map(filteredCategories, categories -> System.currentTimeMillis());
    }

    public LiveData<List<CategoryWithTransactions>> getAllCategories() {
        return allCategories;
    }

    // SEARCH

    /**
     * This automatically changes the filteredCategories
     * null or empty query will return all categories
     * @param query - the query to search for.
     */
    public void setSearchQuery(String query) {
        SearchCriteria searchCriteria = this.searchCriteria.getValue();
        searchCriteria.setQuery(query);

        this.searchCriteria.setValue(searchCriteria);

    }

    public void setTypeFilter(TrackingType typeFilter) {
        SearchCriteria searchCriteria = this.searchCriteria.getValue() != null ? this.searchCriteria.getValue() : new SearchCriteria("", null);
        searchCriteria.setTypeFilter(typeFilter);
        this.searchCriteria.setValue(searchCriteria);

    }

    public LiveData<TrackingType> getTypeFilter() {
        return Transformations.map(searchCriteria, searchCriteria ->{
            if (searchCriteria != null) {
                return searchCriteria.getTypeFilter();
            }
            return null;
        });
    }


    public LiveData<SearchCriteria> getSearchCriteria() {
        return searchCriteria;
    }

    public LiveData<String> getSearchQuery() {
        return Transformations.map(searchCriteria, searchCriteria ->{
            if (searchCriteria != null) {
                return searchCriteria.getQuery();
            }
                return "";
        });
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

    public LiveData<Long> getUpdateTrigger() {
        return updateTrigger;
    }
}
