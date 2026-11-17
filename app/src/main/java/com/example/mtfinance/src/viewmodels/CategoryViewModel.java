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
import com.example.mtfinance.src.trackingengine.SystemPreferences;
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
    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final SystemService systemService;
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
    private final MutableLiveData<Integer> monthOffset = new MutableLiveData<>(0);
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

        if (systemService != null && systemService.getPreferencesLive() != null) {
            systemService.getPreferencesLive().observeForever(prefs -> updatePayPeriodDates(prefs, getMonthOffsetValue()));
        }
    }

    public CategoryViewModel(CategoryService categoryService, TrackingService trackingService, Executor executor) {
        this(categoryService, trackingService, null, executor);
    }

    public void updatePayPeriodDates(SystemPreferences prefs, int offset) {
        int startDay = prefs != null ? prefs.getStartDayOfMonth() : 1;
        boolean includeProvisional = prefs == null || prefs.isIncludeProvisional();
        LocalDate today = LocalDate.now();

        LocalDate[] dates = calculatePayPeriodDates(startDay, offset, includeProvisional, today);
        updateLiveData(startDate, dates[0]);
        updateLiveData(endDate, dates[1]);

        CategoryWithTransactions selected = selectedCategory.getValue();
        if (selected != null) {
            executor.execute(() -> loadTotals(selected));
        }
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

    public int getMonthOffsetValue() {
        Integer offset = monthOffset.getValue();
        return offset != null ? offset : 0;
    }

    public LiveData<Integer> getMonthOffset() {
        return monthOffset;
    }

    public void navigatePreviousMonth() {
        int current = getMonthOffsetValue();
        int newOffset = current - 1;
        monthOffset.setValue(newOffset);
        SystemPreferences prefs = systemService != null ? systemService.getPreferencesSync() : null;
        updatePayPeriodDates(prefs, newOffset);
    }

    public void navigateNextMonth() {
        int current = getMonthOffsetValue();
        int newOffset = current + 1;
        monthOffset.setValue(newOffset);
        SystemPreferences prefs = systemService != null ? systemService.getPreferencesSync() : null;
        updatePayPeriodDates(prefs, newOffset);
    }

    public void resetToCurrentMonth() {
        monthOffset.setValue(0);
        SystemPreferences prefs = systemService != null ? systemService.getPreferencesSync() : null;
        updatePayPeriodDates(prefs, 0);
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
                selected.category = categoryService.getCategoryByIdRestored(categoryId);
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
