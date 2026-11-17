package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.trackingengine.SettingsPreferenceViolationException;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    public enum SystemPreferenceErrorFields {
        THEME_MODE, DATE_FORMAT, CURRENCY_FORMAT, START_DAY_OF_MONTH
    }

    private final SystemService systemService;
    private final SystemRepository systemRepository;
    private final Executor executor;

    private final MutableLiveData<Map<SystemPreferenceErrorFields, String>> fieldErrors = new MutableLiveData<>(new EnumMap<>(SystemPreferenceErrorFields.class));
    private final MutableLiveData<Boolean> changesMade = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> saveStatusColor = new MutableLiveData<>("GRAY");
    private final MutableLiveData<String> genericErrorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");

    private final MutableLiveData<String> provisionalThemeMode = new MutableLiveData<>("SYSTEM");
    private final MutableLiveData<DateFormat> provisionalDateFormat = new MutableLiveData<>(DateFormat.DD_MM_YYYY);
    private final MutableLiveData<CurrencyFormat> provisionalCurrencyFormat = new MutableLiveData<>(CurrencyFormat.NZD);
    private final MutableLiveData<Integer> provisionalStartDayOfMonth = new MutableLiveData<>(1);
    private final MutableLiveData<Boolean> provisionalIncludeProvisional = new MutableLiveData<>(true);

    @Inject
    public SettingsViewModel(SystemService systemService, SystemRepository systemRepository) {
        this(systemService, systemRepository, Executors.newSingleThreadExecutor());
    }

    public SettingsViewModel(SystemService systemService, SystemRepository systemRepository, Executor executor) {
        this.systemService = systemService;
        this.systemRepository = systemRepository;
        this.executor = executor;
        resetFieldErrors();
        initializeFromPersisted();
    }

    public void initializeFromPersisted() {
        if (systemService != null) {
            SystemPreferences prefs = systemService.getPreferencesSync();
            if (prefs != null) {
                provisionalThemeMode.setValue(prefs.getThemeMode());
                provisionalDateFormat.setValue(DateFormat.valueOf(prefs.getDateFormat()));
                provisionalCurrencyFormat.setValue(CurrencyFormat.fromString(prefs.getCurrencyFormat()));
                provisionalStartDayOfMonth.setValue(prefs.getStartDayOfMonth());
                provisionalIncludeProvisional.setValue(prefs.isIncludeProvisional());
            }
        }
        updateLiveData(changesMade, false);
        updateLiveData(saveStatusColor, "GRAY");
        resetFieldErrors();
    }

    public void resetFieldErrors() {
        Map<SystemPreferenceErrorFields, String> errors = new EnumMap<>(SystemPreferenceErrorFields.class);
        for (SystemPreferenceErrorFields field : SystemPreferenceErrorFields.values()) {
            errors.put(field, "");
        }
        updateLiveData(fieldErrors, errors);
        updateLiveData(genericErrorMessage, "");
    }

    public boolean hasFieldErrors() {
        Map<SystemPreferenceErrorFields, String> current = fieldErrors.getValue();
        if (current == null || current.isEmpty()) return false;
        for (String err : current.values()) {
            if (err != null && !err.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setFieldError(SystemPreferenceErrorFields field, String message) {
        Map<SystemPreferenceErrorFields, String> currentMap = fieldErrors.getValue();
        Map<SystemPreferenceErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(SystemPreferenceErrorFields.class);
        newMap.put(field, message != null ? message : "");
        updateLiveData(fieldErrors, newMap);
        reevaluateState();
    }

    public void setProvisionalThemeMode(String themeMode) {
        if (themeMode != null) {
            updateLiveData(provisionalThemeMode, themeMode);
            setFieldError(SystemPreferenceErrorFields.THEME_MODE, "");
            reevaluateState();
        }
    }

    public void setProvisionalDateFormat(DateFormat format) {
        if (format != null) {
            updateLiveData(provisionalDateFormat, format);
            setFieldError(SystemPreferenceErrorFields.DATE_FORMAT, "");
            reevaluateState();
        }
    }

    public void setProvisionalCurrencyFormat(CurrencyFormat currencyFormat) {
        if (currencyFormat != null) {
            updateLiveData(provisionalCurrencyFormat, currencyFormat);
            setFieldError(SystemPreferenceErrorFields.CURRENCY_FORMAT, "");
            reevaluateState();
        }
    }

    public void setProvisionalStartDayOfMonth(String dayString) {
        try {
            int day = Integer.parseInt(dayString.trim());
            if (day < 1 || day > 31) {
                setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day must be between 1 and 31");
            } else {
                updateLiveData(provisionalStartDayOfMonth, day);
                setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "");
            }
        } catch (Exception e) {
            setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day must be a valid number between 1 and 31");
        }
        reevaluateState();
    }

    public void setProvisionalIncludeProvisional(boolean include) {
        updateLiveData(provisionalIncludeProvisional, include);
        reevaluateState();
    }

    public void reevaluateState() {
        SystemPreferences persisted = systemService != null ? systemService.getPreferencesSync() : null;
        if (persisted == null) {
            updateLiveData(changesMade, false);
            updateLiveData(saveStatusColor, hasFieldErrors() ? "RED" : "GRAY");
            return;
        }

        boolean changed = !persisted.getThemeMode().equalsIgnoreCase(provisionalThemeMode.getValue())
                || !persisted.getDateFormat().equalsIgnoreCase(provisionalDateFormat.getValue() != null ? provisionalDateFormat.getValue().name() : "")
                || !persisted.getCurrencyFormat().equalsIgnoreCase(provisionalCurrencyFormat.getValue() != null ? provisionalCurrencyFormat.getValue().name() : "")
                || persisted.getStartDayOfMonth() != (provisionalStartDayOfMonth.getValue() != null ? provisionalStartDayOfMonth.getValue() : 1)
                || persisted.isIncludeProvisional() != (provisionalIncludeProvisional.getValue() != null ? provisionalIncludeProvisional.getValue() : true);

        updateLiveData(changesMade, changed);

        if (hasFieldErrors()) {
            updateLiveData(saveStatusColor, "RED");
        } else if (changed) {
            updateLiveData(saveStatusColor, "ORANGE");
        } else {
            updateLiveData(saveStatusColor, "GRAY");
        }
    }

    public void saveChanges() {
        updateLiveData(isLoading, true);
        executor.execute(() -> {
            try {
                if (hasFieldErrors()) {
                    updateLiveData(saveStatusColor, "RED");
                    updateLiveData(isLoading, false);
                    return;
                }

                SystemPreferences prefs = new SystemPreferences();
                prefs.setThemeMode(provisionalThemeMode.getValue());
                prefs.setDateFormat(provisionalDateFormat.getValue() != null ? provisionalDateFormat.getValue().name() : DateFormat.DD_MM_YYYY.name());
                CurrencyFormat cur = provisionalCurrencyFormat.getValue() != null ? provisionalCurrencyFormat.getValue() : CurrencyFormat.NZD;
                prefs.setCurrencyFormat(cur.name());
                prefs.setCurrencySymbol(cur.getSymbol());
                prefs.setStartDayOfMonth(provisionalStartDayOfMonth.getValue() != null ? provisionalStartDayOfMonth.getValue() : 1);
                prefs.setIncludeProvisional(provisionalIncludeProvisional.getValue() != null ? provisionalIncludeProvisional.getValue() : true);

                if (systemRepository != null) {
                    systemRepository.updatePreferences(prefs);
                }

                updateLiveData(changesMade, false);
                updateLiveData(saveStatusColor, "GREEN");
                updateLiveData(successMessage, "Settings saved successfully!");
                updateLiveData(genericErrorMessage, "");
            } catch (SettingsPreferenceViolationException e) {
                rollbackProvisionalChanges();
                updateLiveData(genericErrorMessage, "Preference configuration violation occurred. Changes rolled back.");
                updateLiveData(saveStatusColor, "RED");
            } catch (Exception e) {
                rollbackProvisionalChanges();
                updateLiveData(genericErrorMessage, "Failed to save settings: " + e.getMessage());
                updateLiveData(saveStatusColor, "RED");
            } finally {
                updateLiveData(isLoading, false);
            }
        });
    }

    public void rollbackProvisionalChanges() {
        initializeFromPersisted();
    }

    public void resetToDefaults() {
        updateLiveData(provisionalThemeMode, "SYSTEM");
        updateLiveData(provisionalDateFormat, DateFormat.DD_MM_YYYY);
        updateLiveData(provisionalCurrencyFormat, CurrencyFormat.NZD);
        updateLiveData(provisionalStartDayOfMonth, 1);
        updateLiveData(provisionalIncludeProvisional, true);
        resetFieldErrors();
        reevaluateState();
    }

    public void discardProvisionalChanges() {
        initializeFromPersisted();
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

    public LiveData<SystemPreferences> getSystemPreferences() {
        return systemService.getPreferencesLive();
    }

    public LiveData<Map<SystemPreferenceErrorFields, String>> getFieldErrors() {
        return fieldErrors;
    }

    public LiveData<Boolean> getChangesMade() {
        return changesMade;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getSaveStatusColor() {
        return saveStatusColor;
    }

    public LiveData<String> getGenericErrorMessage() {
        return genericErrorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<String> getProvisionalThemeMode() {
        return provisionalThemeMode;
    }

    public LiveData<DateFormat> getProvisionalDateFormat() {
        return provisionalDateFormat;
    }

    public LiveData<CurrencyFormat> getProvisionalCurrencyFormat() {
        return provisionalCurrencyFormat;
    }

    public LiveData<Integer> getProvisionalStartDayOfMonth() {
        return provisionalStartDayOfMonth;
    }

    public LiveData<Boolean> getProvisionalIncludeProvisional() {
        return provisionalIncludeProvisional;
    }
}
