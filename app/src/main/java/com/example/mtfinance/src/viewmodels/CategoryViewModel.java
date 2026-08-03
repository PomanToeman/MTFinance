package com.example.mtfinance.src.viewmodels;


import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;


/**
 * Meant to view all and filter categories, as well as view specific details of a category.
 */
@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final Executor executor;
    private final LiveData<List<CategoryWithTransactions>> allCategories;
    @NonNull
    private final MutableLiveData<SearchCriteria> searchCriteria = new MutableLiveData<>(new SearchCriteria("", null));
    private final LiveData<List<CategoryWithTransactions>> filteredCategories;
    private final MutableLiveData<CategoryWithTransactions> selectedCategory = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> totalIncludingSub = new MutableLiveData<>(BigDecimal.ZERO);
    private final MutableLiveData<BigDecimal> totalExcludingSub = new MutableLiveData<>(BigDecimal.ZERO);
    private final MutableLiveData<BigDecimal> remaining = new MutableLiveData<>(BigDecimal.ZERO);
    private final MutableLiveData<Map<CategoryWithTransactions, BigDecimal>> childrenTotals = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> startDate = new MutableLiveData<>(LocalDate.now().withDayOfMonth(1));
    private final MutableLiveData<LocalDate> endDate = new MutableLiveData<>(LocalDate.now());
    private final LiveData<Long> updateTrigger;



    @Inject
    public CategoryViewModel(TrackingRepository trackingRepository, Executor executor) {
        this.trackingRepository = trackingRepository;
        this.executor = executor;
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
        executor.execute(() -> {
            CategoryWithTransactions selected = trackingRepository.getCategoryWithTransactionsByCategoryId(categoryId);
            if (selected != null) {
                selected.category = trackingRepository.getCategoryByIdRestored(categoryId);
                updateLiveData(selectedCategory, selected);
                loadTotals(selected);
            }
        });
    }

    /**
     * Resets the selected category back to no selection.
     * resets totals is well.
     */
    public void resetSelectedCategory() {
        selectedCategory.setValue(null);
        totalIncludingSub.setValue(BigDecimal.ZERO);
        totalExcludingSub.setValue(BigDecimal.ZERO);
        remaining.setValue(BigDecimal.ZERO);
        childrenTotals.setValue(null);


    }


    /**
     * Loads the totals for the selected category. The category must be passed through for background thread reasons.
     *
     * @param selectedCategory - the selected category to load totals for.
     */
    private void loadTotals(CategoryWithTransactions selectedCategory) {
        if (selectedCategory != null) {
            BigDecimal selectedTotal = trackingRepository.getTotalInCategory(selectedCategory.category, true, startDate.getValue(), endDate.getValue());
            updateLiveData(totalIncludingSub, selectedTotal);
            updateLiveData(remaining, selectedCategory.category.getMonthlyBudget().subtract(selectedTotal));
            updateLiveData(totalExcludingSub, trackingRepository.getTotalInCategory(selectedCategory.category, false, startDate.getValue(), endDate.getValue()));
            updateLiveData(childrenTotals, trackingRepository.getChildrenTotals(selectedCategory, startDate.getValue(), endDate.getValue()));
        }

    }

    private <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                liveData.setValue(value);
            } else {
                liveData.postValue(value);
            }
        } catch (Exception e) {
            liveData.postValue(value);
        }
    }

    // GETTERS

    public LiveData<CategoryWithTransactions> getSelectedCategory() {
        return selectedCategory;
    }

    public LiveData<Long> getUpdateTrigger() {
        return updateTrigger;
    }

    public LiveData<BigDecimal> getTotalIncludingSub() {
        return totalIncludingSub;
    }
    public LiveData<BigDecimal> getRemaining() {
        return remaining;
    }
    public LiveData<Map<CategoryWithTransactions, BigDecimal>> getChildrenTotals() {
        return childrenTotals;
    }

    public LiveData<BigDecimal> getTotalExcludingSub() {
        return totalExcludingSub;
    }
    public LiveData<LocalDate> getStartDate() {
        return startDate;
    }
    public LiveData<LocalDate> getEndDate() {
        return endDate;
    }



}
