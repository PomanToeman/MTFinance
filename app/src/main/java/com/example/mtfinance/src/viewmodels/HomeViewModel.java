package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;

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


    @Inject
    public HomeViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

}
