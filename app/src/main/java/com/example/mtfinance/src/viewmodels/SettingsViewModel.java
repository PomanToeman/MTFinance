package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.messages.GeneralMessages;
import com.example.mtfinance.src.trackingengine.DefaultSettings;
import com.example.mtfinance.src.trackingengine.SettingsPreferenceViolationException;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import com.example.mtfinance.src.viewmodels.interfaces.SaveableFormViewModel;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel implements SaveableFormViewModel<SettingsViewModel.SystemPreferenceErrorFields> {

    public enum SystemPreferenceErrorFields {
        THEME_MODE, DATE_FORMAT, CURRENCY_FORMAT, START_DAY_OF_MONTH
    }

    private final SystemService systemService;
    private final SystemRepository systemRepository;
    private final TrackingRepository trackingRepository;
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
    private final MutableLiveData<String> provisionalStartDayOfMonthInput = new MutableLiveData<>("1");
    private final MutableLiveData<Boolean> provisionalIncludeProvisional = new MutableLiveData<>(true);

    @Inject
    public SettingsViewModel(SystemService systemService, SystemRepository systemRepository, TrackingRepository trackingRepository, Executor executor) {
        this.systemService = systemService;
        this.systemRepository = systemRepository;
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        resetFieldErrors();
        initializeFromPersisted();
    }

    public void deleteAllAppData() {
        executor.execute(() -> {
            try {
                updateLiveData(isLoading, true);
                trackingRepository.deleteAllAppData();
                updateLiveData(successMessage, "All app data deleted successfully.");
            } catch (Exception e) {
                updateLiveData(genericErrorMessage, "Failed to delete app data: " + e.getMessage());
            } finally {
                updateLiveData(isLoading, false);
            }
        });
    }

    public void deleteTransactionsOnly() {
        executor.execute(() -> {
            try {
                updateLiveData(isLoading, true);
                trackingRepository.deleteAllTransactions();
                updateLiveData(successMessage, "All transactions deleted successfully (categories retained).");
            } catch (Exception e) {
                updateLiveData(genericErrorMessage, "Failed to delete transactions: " + e.getMessage());
            } finally {
                updateLiveData(isLoading, false);
            }
        });
    }

    public void initializeFromPersisted() {
        if (executor != null) {
            executor.execute(this::initializeFromPersistedSync);
        } else {
            initializeFromPersistedSync();
        }
    }

    private void initializeFromPersistedSync() {
        if (systemService != null) {
            SystemPreferences prefs = systemService.getPreferencesSync();
            if (prefs != null) {
                updateLiveData(provisionalThemeMode, prefs.getThemeMode());
                updateLiveData(provisionalDateFormat, DateFormat.valueOf(prefs.getDateFormat()));
                updateLiveData(provisionalCurrencyFormat, CurrencyFormat.fromString(prefs.getCurrencyFormat()));
                updateLiveData(provisionalStartDayOfMonthInput, String.valueOf(prefs.getStartDayOfMonth()));
                updateLiveData(provisionalIncludeProvisional, prefs.isIncludeProvisional());
            }
        }
        updateLiveData(changesMade, false);
        updateLiveData(saveStatusColor, "GRAY");
        resetFieldErrors();
    }

    public void resetFieldErrors() {
        updateLiveData(fieldErrors, new EnumMap<>(SystemPreferenceErrorFields.class));
        updateLiveData(genericErrorMessage, "");
    }

    @Override
    public void resetFieldError(SystemPreferenceErrorFields field) {
        Map<SystemPreferenceErrorFields, String> currentMap = fieldErrors.getValue();
        if (currentMap != null && currentMap.containsKey(field)) {
            Map<SystemPreferenceErrorFields, String> newMap = new EnumMap<>(currentMap);
            newMap.remove(field);
            updateLiveData(fieldErrors, newMap);
        }
    }

    @Override
    public void resetMessages() {
        updateLiveData(genericErrorMessage, "");
        updateLiveData(successMessage, "");
    }

    @Override
    public void save() {
        saveChanges();
    }

    @Override
    public void clear() {
        resetToDefaults();
    }

    public LiveData<String> getErrorMessage() {
        return genericErrorMessage;
    }

    public boolean hasFieldErrors() {
        Map<SystemPreferenceErrorFields, String> current = fieldErrors.getValue();
        return current != null && !current.isEmpty();
    }

    public boolean hasFieldErrors(Map<SystemPreferenceErrorFields, String> errors) {
        return errors != null && !errors.isEmpty();
    }

    private void setFieldError(SystemPreferenceErrorFields field, String message) {
        Map<SystemPreferenceErrorFields, String> currentMap = fieldErrors.getValue();
        Map<SystemPreferenceErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(SystemPreferenceErrorFields.class);
        if (message == null || message.trim().isEmpty()) {
            newMap.remove(field);
        } else {
            newMap.put(field, message);
        }
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
        if (format != null ) {
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
        String val = dayString != null ? dayString.trim() : "";
        updateLiveData(provisionalStartDayOfMonthInput, val);

        if (val.isEmpty()) {
            setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day of month cannot be empty");
        } else {
            try {
                int day = Integer.parseInt(val);
                if (day < DefaultSettings.MIN_START_DAY || day > DefaultSettings.MAX_START_DAY) {
                    setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day must be between " + DefaultSettings.MIN_START_DAY + " and " + DefaultSettings.MAX_START_DAY);
                } else {
                    setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "");
                }
            } catch (Exception e) {
                setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day must be a valid number between " + DefaultSettings.MIN_START_DAY + " and " + DefaultSettings.MAX_START_DAY);
            }
        }
        reevaluateState();
    }

    public Map<SystemPreferenceErrorFields, String> validateSettingsConfigurations() {
        Map<SystemPreferenceErrorFields, String> errors = new EnumMap<>(SystemPreferenceErrorFields.class);

        String themeMode = provisionalThemeMode.getValue();
        if (themeMode == null || themeMode.trim().isEmpty()) {
            errors.put(SystemPreferenceErrorFields.THEME_MODE, "Theme mode cannot be empty");
        }

        DateFormat dateFormat = provisionalDateFormat.getValue();
        if (dateFormat == null) {
            errors.put(SystemPreferenceErrorFields.DATE_FORMAT, "Date format cannot be empty");
        }

        CurrencyFormat currencyFormat = provisionalCurrencyFormat.getValue();
        if (currencyFormat == null) {
            errors.put(SystemPreferenceErrorFields.CURRENCY_FORMAT, "Currency format cannot be empty");
        }

        String dayStr = provisionalStartDayOfMonthInput.getValue();
        if (dayStr == null || dayStr.trim().isEmpty()) {
            errors.put(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day of month cannot be empty");
        } else {
            try {
                int day = Integer.parseInt(dayStr.trim());
                if (day < DefaultSettings.MIN_START_DAY || day > DefaultSettings.MAX_START_DAY) {
                    errors.put(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day must be between " + DefaultSettings.MIN_START_DAY + " and " + DefaultSettings.MAX_START_DAY);
                }
            } catch (Exception e) {
                errors.put(SystemPreferenceErrorFields.START_DAY_OF_MONTH, "Start day must be a valid number between " + DefaultSettings.MIN_START_DAY + " and " + DefaultSettings.MAX_START_DAY);
            }
        }

        updateLiveData(fieldErrors, errors);
        reevaluateState();
        return errors;
    }

    public void setProvisionalIncludeProvisional(boolean include) {
        updateLiveData(provisionalIncludeProvisional, include);
        reevaluateState();
    }

    public void reevaluateState() {
        if (executor != null) {
            executor.execute(this::reevaluateStateSync);
        } else {
            reevaluateStateSync();
        }
    }

    private void reevaluateStateSync() {
        SystemPreferences persisted = systemService != null ? systemService.getPreferencesSync() : null;
        if (persisted == null) {
            updateLiveData(changesMade, false);
            updateLiveData(saveStatusColor, hasFieldErrors() ? "RED" : "GRAY");
            return;
        }

        String currentDayStr = provisionalStartDayOfMonthInput.getValue() != null ? provisionalStartDayOfMonthInput.getValue() : "1";
        int parsedDay = 1;
        try {
            parsedDay = Integer.parseInt(currentDayStr.trim());
        } catch (Exception e) {
        }

        boolean changed = !persisted.getThemeMode().equalsIgnoreCase(provisionalThemeMode.getValue())
                || !persisted.getDateFormat().equalsIgnoreCase(provisionalDateFormat.getValue() != null ? provisionalDateFormat.getValue().name() : "")
                || !persisted.getCurrencyFormat().equalsIgnoreCase(provisionalCurrencyFormat.getValue() != null ? provisionalCurrencyFormat.getValue().name() : "")
                || persisted.getStartDayOfMonth() != parsedDay
                || persisted.isIncludeProvisional() != (provisionalIncludeProvisional.getValue() == null || provisionalIncludeProvisional.getValue());

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
                Map<SystemPreferenceErrorFields, String> errors = validateSettingsConfigurations();
                if (hasFieldErrors(errors) || hasFieldErrors()) {
                    updateLiveData(saveStatusColor, "RED");
                    updateLiveData(isLoading, false);
                    return;
                }

                int parsedStartDay = 1;
                try {
                    parsedStartDay = Integer.parseInt(provisionalStartDayOfMonthInput.getValue());
                } catch (Exception e) {
                    throw new SettingsPreferenceViolationException("Invalid start day of month value: " + provisionalStartDayOfMonthInput.getValue());
                }

                SystemPreferences prefs = new SystemPreferences();
                prefs.setThemeMode(provisionalThemeMode.getValue());
                prefs.setDateFormat(provisionalDateFormat.getValue() != null ? provisionalDateFormat.getValue().name() : DateFormat.DD_MM_YYYY.name());
                CurrencyFormat cur = provisionalCurrencyFormat.getValue() != null ? provisionalCurrencyFormat.getValue() : CurrencyFormat.NZD;
                prefs.setCurrencyFormat(cur.name());
                prefs.setCurrencySymbol(cur.getSymbol());
                prefs.setStartDayOfMonth(parsedStartDay);
                prefs.setIncludeProvisional(provisionalIncludeProvisional.getValue() == null || provisionalIncludeProvisional.getValue());

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
        updateLiveData(provisionalStartDayOfMonthInput, "1");
        updateLiveData(provisionalIncludeProvisional, true);
        resetFieldErrors();
        reevaluateState();
    }

    public void discardProvisionalChanges() {
        initializeFromPersisted();
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

    public LiveData<String> getProvisionalStartDayOfMonthInput() {
        return provisionalStartDayOfMonthInput;
    }

    public LiveData<Boolean> getProvisionalIncludeProvisional() {
        return provisionalIncludeProvisional;
    }
}
