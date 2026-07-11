package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CategoryViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private LiveData<List<CategoryWithTransactions>> categories;



    @Inject
    public CategoryViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        loadAllCategories();
    }


    private void loadAllCategories() {
        categories = trackingRepository.getAllCategoriesWithTransactions();

    }

    public LiveData<List<CategoryWithTransactions>> getAllCategories() {
        return categories;
    }







}
