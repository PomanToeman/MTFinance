package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;

import java.util.concurrent.Executor;

import dagger.hilt.android.lifecycle.HiltViewModel;
import jakarta.inject.Inject;

/**
 * Will mainly hold all the main in memory data for the home screen specifically.
 * Methods will be implemented when needed when implementing JetPack Compose.
 */
@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final TrackingRepository trackingRepository;

    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> subtitle = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<CategoryViewModel> pinnedCategory = new MutableLiveData<>();
    private final Executor executor;


    @Inject
    public HomeViewModel(TrackingRepository trackingRepository, Executor executor) {
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        CategoryViewModel categoryViewModel = new CategoryViewModel(trackingRepository, executor);
        categoryViewModel.setSelectedCategory(1L);
        this.pinnedCategory.setValue(categoryViewModel);
    }


    public LiveData<CategoryViewModel> getPinnedCategory() {
        return pinnedCategory;
    }


}
