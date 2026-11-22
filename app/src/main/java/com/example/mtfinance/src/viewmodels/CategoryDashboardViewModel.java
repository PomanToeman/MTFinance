package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.SystemPreferences;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Dedicated ViewModel for the Category Dashboard, encapsulating selected category analytics,
 * month navigation, subcategory filtering, transaction filtering, and chart data producers.
 */
@HiltViewModel
public class CategoryDashboardViewModel extends ViewModel {
    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final SystemService systemService;
    private final Executor executor;

    private final MutableLiveData<CategoryWithTransactions> selectedCategory = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> totalIncludingSub = new MutableLiveData<>(BigDecimal.ZERO);
    private final MutableLiveData<BigDecimal> totalExcludingSub = new MutableLiveData<>(BigDecimal.ZERO);
    private final MutableLiveData<BigDecimal> remaining = new MutableLiveData<>(BigDecimal.ZERO);
    private final MutableLiveData<Map<CategoryWithTransactions, BigDecimal>> childrenTotals = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> startDate = new MutableLiveData<>(LocalDate.now().withDayOfMonth(1));
    private final MutableLiveData<LocalDate> endDate = new MutableLiveData<>(LocalDate.now());
    private final MutableLiveData<Integer> monthOffset = new MutableLiveData<>(0);

    private final MutableLiveData<String> subcategorySearchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> transactionSearchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> filterTransactionsByPeriod = new MutableLiveData<>(true);

    @Inject
    public CategoryDashboardViewModel(CategoryService categoryService, TrackingService trackingService, SystemService systemService, Executor executor) {
        this.categoryService = categoryService;
        this.trackingService = trackingService;
        this.systemService = systemService;
        this.executor = executor;

        if (systemService != null && systemService.getPreferencesLive() != null) {
            systemService.getPreferencesLive().observeForever(prefs -> updatePayPeriodDates(prefs, getMonthOffsetValue()));
        }
    }

    public void updatePayPeriodDates(SystemPreferences prefs, int offset) {
        int startDay = prefs != null ? prefs.getStartDayOfMonth() : 1;
        boolean includeProvisional = prefs == null || prefs.isIncludeProvisional();
        LocalDate today = LocalDate.now();

        LocalDate[] dates = CategoryViewModel.calculatePayPeriodDates(startDay, offset, includeProvisional, today);
        updateLiveData(startDate, dates[0]);
        updateLiveData(endDate, dates[1]);

        CategoryWithTransactions selected = selectedCategory.getValue();
        if (selected != null) {
            executor.execute(() -> loadTotals(selected, dates[0], dates[1]));
        }
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
        executeMonthOffsetUpdate(newOffset);
    }

    public void navigateNextMonth() {
        int current = getMonthOffsetValue();
        int newOffset = current + 1;
        monthOffset.setValue(newOffset);
        executeMonthOffsetUpdate(newOffset);
    }

    public void resetToCurrentMonth() {
        monthOffset.setValue(0);
        executeMonthOffsetUpdate(0);
    }

    public void jumpToMonthOffset(int targetOffset) {
        monthOffset.setValue(targetOffset);
        executeMonthOffsetUpdate(targetOffset);
    }

    private void executeMonthOffsetUpdate(int offset) {
        if (executor != null) {
            executor.execute(() -> {
                SystemPreferences prefs = systemService != null ? systemService.getPreferencesSync() : null;
                updatePayPeriodDates(prefs, offset);
            });
        } else {
            SystemPreferences prefs = systemService != null ? systemService.getPreferencesSync() : null;
            updatePayPeriodDates(prefs, offset);
        }
    }

    public void setSelectedCategory(Long categoryId) {
        if (categoryId == null) {
            resetSelectedCategory();
            return;
        }
        executor.execute(() -> {
            CategoryWithTransactions selected = trackingService.getCategoryWithTransactionsByCategoryId(categoryId);
            if (selected != null) {
                selected.category = categoryService.getCategoryByIdRestored(categoryId);
                updateLiveData(selectedCategory, selected);
                loadTotals(selected, startDate.getValue(), endDate.getValue());
            }
        });
    }

    public void resetSelectedCategory() {
        updateLiveData(selectedCategory, null);
        updateLiveData(totalIncludingSub, BigDecimal.ZERO);
        updateLiveData(totalExcludingSub, BigDecimal.ZERO);
        updateLiveData(remaining, BigDecimal.ZERO);
        updateLiveData(childrenTotals, null);
    }

    private void loadTotals(CategoryWithTransactions selectedCategory, LocalDate start, LocalDate end) {
        if (selectedCategory != null) {
            LocalDate effectiveStart = start != null ? start : LocalDate.now().withDayOfMonth(1);
            LocalDate effectiveEnd = end != null ? end : LocalDate.now();
            BigDecimal selectedTotal = trackingService.getTotalInCategory(selectedCategory.category, true, effectiveStart, effectiveEnd);
            updateLiveData(totalIncludingSub, selectedTotal);
            updateLiveData(remaining, selectedCategory.category.getMonthlyBudget().subtract(selectedTotal));
            updateLiveData(totalExcludingSub, trackingService.getTotalInCategory(selectedCategory.category, false, effectiveStart, effectiveEnd));
            updateLiveData(childrenTotals, trackingService.getChildrenTotals(selectedCategory, effectiveStart, effectiveEnd));
        }
    }

    public void setSubcategorySearchQuery(String query) {
        updateLiveData(subcategorySearchQuery, query != null ? query : "");
    }

    public void setTransactionSearchQuery(String query) {
        updateLiveData(transactionSearchQuery, query != null ? query : "");
    }

    public void setFilterTransactionsByPeriod(boolean filter) {
        updateLiveData(filterTransactionsByPeriod, filter);
    }

    public LiveData<String> getSubcategorySearchQuery() { return subcategorySearchQuery; }
    public LiveData<String> getTransactionSearchQuery() { return transactionSearchQuery; }
    public LiveData<Boolean> getFilterTransactionsByPeriod() { return filterTransactionsByPeriod; }

    public LiveData<CategoryWithTransactions> getSelectedCategory() { return selectedCategory; }
    public LiveData<BigDecimal> getTotalIncludingSub() { return totalIncludingSub; }
    public LiveData<BigDecimal> getRemaining() { return remaining; }
    public LiveData<Map<CategoryWithTransactions, BigDecimal>> getChildrenTotals() { return childrenTotals; }
    public LiveData<BigDecimal> getTotalExcludingSub() { return totalExcludingSub; }
    public LiveData<LocalDate> getStartDate() { return startDate; }
    public LiveData<LocalDate> getEndDate() { return endDate; }

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
