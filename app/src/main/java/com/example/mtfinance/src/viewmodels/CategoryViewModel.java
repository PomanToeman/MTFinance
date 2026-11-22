package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Meant to view all and filter categories, as well as view specific details of a category.
 */
@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final SystemService systemService;
    private final Executor executor;
    private final LiveData<List<CategoryWithTransactions>> allCategories;
    @NonNull
    private final MutableLiveData<SearchCriteria> searchCriteria = new MutableLiveData<>(new SearchCriteria("", null));
    private final LiveData<List<CategoryWithTransactions>> filteredCategories;
    private final LiveData<Long> updateTrigger;

    @Inject
    public CategoryViewModel(CategoryService categoryService, TrackingService trackingService, SystemService systemService, Executor executor) {
        this.categoryService = categoryService;
        this.trackingService = trackingService;
        this.systemService = systemService;
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

    public static LocalDate[] calculatePayPeriodDates(int startDay, int monthOffset, boolean includeProvisional, LocalDate today) {
        int validatedStartDay = Math.max(1, Math.min(31, startDay));

        LocalDate anchorMonth = today.plusMonths(monthOffset);

        LocalDate periodStart;
        if (anchorMonth.getDayOfMonth() >= Math.min(validatedStartDay, anchorMonth.lengthOfMonth())) {
            periodStart = anchorMonth.withDayOfMonth(Math.min(validatedStartDay, anchorMonth.lengthOfMonth()));
        } else {
            LocalDate prevMonth = anchorMonth.minusMonths(1);
            periodStart = prevMonth.withDayOfMonth(Math.min(validatedStartDay, prevMonth.lengthOfMonth()));
        }

        LocalDate nextPeriodStart = periodStart.plusMonths(1);
        nextPeriodStart = nextPeriodStart.withDayOfMonth(Math.min(validatedStartDay, nextPeriodStart.lengthOfMonth()));

        LocalDate periodEndFull = nextPeriodStart.minusDays(1);

        LocalDate periodEnd;
        if (monthOffset == 0 && !includeProvisional && today.isBefore(periodEndFull)) {
            periodEnd = today;
        } else {
            periodEnd = periodEndFull;
        }

        return new LocalDate[]{periodStart, periodEnd};
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

    public LiveData<Long> getUpdateTrigger() {
        return updateTrigger;
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
}
