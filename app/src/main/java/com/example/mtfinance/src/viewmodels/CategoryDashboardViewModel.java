package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CategoryDashboardViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final LiveData<List<CategoryWithTransactions>> categoryWithTransactionsLiveData;
    @Inject
    public CategoryDashboardViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        this.categoryWithTransactionsLiveData = trackingRepository.getAllCategoriesWithTransactions();
    }

    public LiveData<List<CategoryWithTransactions>> getCategoryWithTransactionsLiveData() {
        return categoryWithTransactionsLiveData;
    }


}
