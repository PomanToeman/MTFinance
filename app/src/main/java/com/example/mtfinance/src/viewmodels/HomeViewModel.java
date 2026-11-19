package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Will mainly hold all the main in memory data for the home screen specifically.
 * Methods will be implemented when needed when implementing JetPack Compose.
 */
@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final SystemService systemService;
    private final Executor executor;

    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> subtitle = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<CategoryViewModel> pinnedCategory = new MutableLiveData<>();

    @Inject
    public HomeViewModel(CategoryService categoryService, TrackingService trackingService, SystemService systemService, Executor executor) {
        this.categoryService = categoryService;
        this.trackingService = trackingService;
        this.systemService = systemService;
        this.executor = executor;
        CategoryViewModel categoryViewModel = new CategoryViewModel(categoryService, trackingService, systemService, executor);
        categoryViewModel.setSelectedCategory(1L);
        this.pinnedCategory.setValue(categoryViewModel);
    }

    public LiveData<CategoryViewModel> getPinnedCategory() {
        return pinnedCategory;
    }
}
