package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;

import dagger.hilt.android.lifecycle.HiltViewModel;
import jakarta.inject.Inject;

@HiltViewModel
public class TransactionImportViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;

    @Inject
    public TransactionImportViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

}
