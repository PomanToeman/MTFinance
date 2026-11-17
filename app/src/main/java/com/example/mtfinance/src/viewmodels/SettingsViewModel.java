package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final SystemService systemService;
    private final SystemRepository systemRepository;

    @Inject
    public SettingsViewModel(SystemService systemService, SystemRepository systemRepository) {
        this.systemService = systemService;
        this.systemRepository = systemRepository;
    }

    public LiveData<SystemPreferences> getSystemPreferences() {
        return systemService.getPreferencesLive();
    }

    public void setThemeMode(String themeMode) {
        if (themeMode != null) {
            systemRepository.updateThemeMode(themeMode);
        }
    }

    public void setDateFormat(DateFormat format) {
        if (format != null) {
            systemRepository.updateDateFormat(format);
        }
    }

    public void setCurrencyFormat(CurrencyFormat currencyFormat) {
        if (currencyFormat != null) {
            systemRepository.updateCurrencyFormat(currencyFormat);
        }
    }

    public void setStartDayOfMonth(int startDay) {
        systemRepository.updateStartDayOfMonth(startDay);
    }

    public void setIncludeProvisional(boolean includeProvisional) {
        systemRepository.updateIncludeProvisional(includeProvisional);
    }
}
