package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Meant to view all and filter categories, as well as view specific details of a category.
 */
@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final TrackingService trackingService;
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
    public CategoryViewModel(TrackingService trackingService, Executor executor) {
        this.trackingService = trackingService;
        this.executor = executor;
        this.allCategories = trackingService.getAllCategoriesWithTransactions();

        this.filteredCategories = Transformations.switchMap(searchCriteria, criteria -> {
            if (criteria == null || (criteria.getQuery() == null || criteria.getQuery().trim().isEmpty()) && criteria.getTypeFilter() == null) {
                return allCategories;
            }
            return trackingService.searchCategoriesWithType(criteria.getTrimmedQuery(), criteria.getTypeFilter());
        });
        this.updateTrigger = Transformations.map(filteredCategories, categories -> System.currentTimeMillis());
    }

    public LiveData<List<CategoryWithTransactions>> getAllCategories() {
        return allCategories;
    }

    public void setSearchQuery(String query) {
        SearchCriteria criteria = this.searchCriteria.getValue();
        if (criteria == null) {
            criteria = new SearchCriteria("", null);
        }
        criteria.setQuery(query);
        this.searchCriteria.setValue(criteria);
    }

    public void setTypeFilter(TrackingType typeFilter) {
        SearchCriteria criteria = this.searchCriteria.getValue() != null ? this.searchCriteria.getValue() : new SearchCriteria("", null);
        criteria.setTypeFilter(typeFilter);
        this.searchCriteria.setValue(criteria);
    }

    public LiveData<TrackingType> getTypeFilter() {
        return Transformations.map(searchCriteria, criteria -> {
            if (criteria != null) {
                return criteria.getTypeFilter();
            }
            return null;
        });
    }

    public LiveData<SearchCriteria> getSearchCriteria() {
        return searchCriteria;
    }

    public LiveData<String> getSearchQuery() {
        return Transformations.map(searchCriteria, criteria -> {
            if (criteria != null) {
                return criteria.getQuery();
            }
            return "";
        });
    }

    public LiveData<List<CategoryWithTransactions>> getFilteredCategories() {
        return filteredCategories;
    }

    public void setSelectedCategory(Long categoryId) {
        executor.execute(() -> {
            CategoryWithTransactions selected = trackingService.getCategoryWithTransactionsByCategoryId(categoryId);
            if (selected != null) {
                selected.category = trackingService.getCategoryByIdRestored(categoryId);
                updateLiveData(selectedCategory, selected);
                loadTotals(selected);
            }
        });
    }

    public void resetSelectedCategory() {
        selectedCategory.setValue(null);
        totalIncludingSub.setValue(BigDecimal.ZERO);
        totalExcludingSub.setValue(BigDecimal.ZERO);
        remaining.setValue(BigDecimal.ZERO);
        childrenTotals.setValue(null);
    }

    private void loadTotals(CategoryWithTransactions selectedCategory) {
        if (selectedCategory != null) {
            BigDecimal selectedTotal = trackingService.getTotalInCategory(selectedCategory.category, true, startDate.getValue(), endDate.getValue());
            updateLiveData(totalIncludingSub, selectedTotal);
            updateLiveData(remaining, selectedCategory.category.getMonthlyBudget().subtract(selectedTotal));
            updateLiveData(totalExcludingSub, trackingService.getTotalInCategory(selectedCategory.category, false, startDate.getValue(), endDate.getValue()));
            updateLiveData(childrenTotals, trackingService.getChildrenTotals(selectedCategory, startDate.getValue(), endDate.getValue()));
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
