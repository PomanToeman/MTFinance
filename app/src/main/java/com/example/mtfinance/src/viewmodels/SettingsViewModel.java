package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.messages.ErrorMessages;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.DefaultSettings;
import com.example.mtfinance.src.trackingengine.SettingsPreferenceViolationException;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import com.example.mtfinance.src.viewmodels.interfaces.SaveableFormViewModel;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Modernized Form ViewModel managing persistent system configurations and provisional settings state.
 */
@HiltViewModel
public class SettingsViewModel extends ViewModel implements SaveableFormViewModel<SettingsViewModel.SystemPreferenceErrorFields> {

    public enum SystemPreferenceErrorFields {
        THEME_MODE, DATE_FORMAT, CURRENCY_FORMAT, START_DAY_OF_MONTH
    }

    private final SystemService systemService;
    private final SystemRepository systemRepository;
    private final TrackingRepository trackingRepository;
    private final Executor executor;
    private final Logger logger;
    private SystemPreferences persistedPreferences = null;

    private final MutableLiveData<Map<SystemPreferenceErrorFields, String>> fieldErrors = new MutableLiveData<>(new EnumMap<>(SystemPreferenceErrorFields.class));
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> saveStatusColor = new MutableLiveData<>("GRAY");
    private final MutableLiveData<SettingsSaveState> saveState = new MutableLiveData<>(SettingsSaveState.UNCHANGED);
    private final MutableLiveData<String> genericErrorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");

    private final MutableLiveData<String> provisionalThemeMode = new MutableLiveData<>("SYSTEM");
    private final MutableLiveData<DateFormat> provisionalDateFormat = new MutableLiveData<>(DateFormat.DD_MM_YYYY);
    private final MutableLiveData<CurrencyFormat> provisionalCurrencyFormat = new MutableLiveData<>(CurrencyFormat.NZD);
    private final MutableLiveData<String> provisionalStartDayOfMonthInput = new MutableLiveData<>("1");
    private final MutableLiveData<Boolean> provisionalIncludeProvisional = new MutableLiveData<>(true);

    @Inject
    public SettingsViewModel(SystemService systemService, SystemRepository systemRepository, TrackingRepository trackingRepository, Executor executor, Logger logger) {
        this.systemService = systemService;
        this.systemRepository = systemRepository;
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        this.logger = logger;
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
                logger.log(Level.SEVERE, ErrorMessages.FAILED_DELETE_APP_DATA.getMessage(e.getMessage()), e);
                updateLiveData(genericErrorMessage, ErrorMessages.FAILED_DELETE_APP_DATA.getMessage(e.getMessage()));
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
                logger.log(Level.SEVERE, ErrorMessages.FAILED_DELETE_TRANSACTIONS.getMessage(e.getMessage()), e);
                updateLiveData(genericErrorMessage, ErrorMessages.FAILED_DELETE_TRANSACTIONS.getMessage(e.getMessage()));
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
            this.persistedPreferences = prefs;
            if (prefs != null) {
                // Only load persisted preferences into provisional fields if UNCHANGED (no active user edits)
                if (saveState.getValue() == SettingsSaveState.UNCHANGED || saveState.getValue() == null) {
                    updateLiveData(provisionalThemeMode, prefs.getThemeMode());
                    updateLiveData(provisionalDateFormat, DateFormat.fromStringOrDefault(prefs.getDateFormat(), DateFormat.DD_MM_YYYY));
                    updateLiveData(provisionalCurrencyFormat, CurrencyFormat.fromString(prefs.getCurrencyFormat()));
                    updateLiveData(provisionalStartDayOfMonthInput, String.valueOf(prefs.getStartDayOfMonth()));
                    updateLiveData(provisionalIncludeProvisional, prefs.isIncludeProvisional());
                }
            }
        } else {
            this.persistedPreferences = null;
        }
        if (saveState.getValue() == SettingsSaveState.UNCHANGED || saveState.getValue() == null) {
            updateLiveData(saveStatusColor, "GRAY");
            updateLiveData(saveState, SettingsSaveState.UNCHANGED);
            resetFieldErrors();
        }
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
            resetFieldError(SystemPreferenceErrorFields.THEME_MODE);
            reevaluateState();
        }
    }

    public void setProvisionalDateFormat(DateFormat format) {
        if (format != null) {
            updateLiveData(provisionalDateFormat, format);
            resetFieldError(SystemPreferenceErrorFields.DATE_FORMAT);
            reevaluateState();
        }
    }

    public void setProvisionalCurrencyFormat(CurrencyFormat currencyFormat) {
        if (currencyFormat != null) {
            updateLiveData(provisionalCurrencyFormat, currencyFormat);
            resetFieldError(SystemPreferenceErrorFields.CURRENCY_FORMAT);
            reevaluateState();
        }
    }

    public void setProvisionalStartDayOfMonth(String dayString) {
        String val = dayString != null ? dayString.trim() : "";
        updateLiveData(provisionalStartDayOfMonthInput, val);

        if (val.isEmpty()) {
            setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, ErrorMessages.START_DAY_EMPTY.getMessage());
        } else {
            try {
                int day = Integer.parseInt(val);
                if (day < DefaultSettings.MIN_START_DAY || day > DefaultSettings.MAX_START_DAY) {
                    setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, ErrorMessages.START_DAY_RANGE.getMessage());
                } else {
                    resetFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH);
                }
            } catch (NumberFormatException e) {
                setFieldError(SystemPreferenceErrorFields.START_DAY_OF_MONTH, ErrorMessages.START_DAY_RANGE.getMessage());
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
            errors.put(SystemPreferenceErrorFields.START_DAY_OF_MONTH, ErrorMessages.START_DAY_EMPTY.getMessage());
        } else {
            try {
                int day = Integer.parseInt(dayStr.trim());
                if (day < DefaultSettings.MIN_START_DAY || day > DefaultSettings.MAX_START_DAY) {
                    errors.put(SystemPreferenceErrorFields.START_DAY_OF_MONTH, ErrorMessages.START_DAY_RANGE.getMessage());
                }
            } catch (NumberFormatException e) {
                errors.put(SystemPreferenceErrorFields.START_DAY_OF_MONTH, ErrorMessages.START_DAY_RANGE.getMessage());
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
        reevaluateStateSync();
    }

    private void reevaluateStateSync() {
        SystemPreferences persisted = this.persistedPreferences;
        if (persisted == null) {
            updateLiveData(saveStatusColor, hasFieldErrors() ? "RED" : "GRAY");
            updateLiveData(saveState, hasFieldErrors() ? SettingsSaveState.ERROR : SettingsSaveState.UNCHANGED);
            return;
        }

        String currentDayStr = provisionalStartDayOfMonthInput.getValue() != null ? provisionalStartDayOfMonthInput.getValue() : "1";
        int parsedDay = 1;
        try {
            parsedDay = Integer.parseInt(currentDayStr.trim());
        } catch (NumberFormatException ignored) {
        }

        boolean changed = !persisted.getThemeMode().equalsIgnoreCase(provisionalThemeMode.getValue())
                || !persisted.getDateFormat().equalsIgnoreCase(provisionalDateFormat.getValue() != null ? provisionalDateFormat.getValue().name() : "")
                || !persisted.getCurrencyFormat().equalsIgnoreCase(provisionalCurrencyFormat.getValue() != null ? provisionalCurrencyFormat.getValue().name() : "")
                || persisted.getStartDayOfMonth() != parsedDay
                || persisted.isIncludeProvisional() != (provisionalIncludeProvisional.getValue() == null || provisionalIncludeProvisional.getValue());

        if (hasFieldErrors()) {
            updateLiveData(saveStatusColor, "RED");
            updateLiveData(saveState, SettingsSaveState.ERROR);
        } else if (changed) {
            updateLiveData(saveStatusColor, "ORANGE");
            updateLiveData(saveState, SettingsSaveState.PROVISIONAL);
        } else {
            updateLiveData(saveStatusColor, "GRAY");
            updateLiveData(saveState, SettingsSaveState.UNCHANGED);
        }
    }

    public void saveChanges() {
        updateLiveData(isLoading, true);
        executor.execute(() -> {
            try {
                Map<SystemPreferenceErrorFields, String> errors = validateSettingsConfigurations();
                if (hasFieldErrors(errors) || hasFieldErrors()) {
                    updateLiveData(saveStatusColor, "RED");
                    updateLiveData(saveState, SettingsSaveState.ERROR);
                    updateLiveData(isLoading, false);
                    return;
                }

                int parsedStartDay = 1;
                try {
                    parsedStartDay = Integer.parseInt(provisionalStartDayOfMonthInput.getValue());
                } catch (NumberFormatException e) {
                    throw new SettingsPreferenceViolationException(ErrorMessages.INVALID_START_DAY.getMessage(provisionalStartDayOfMonthInput.getValue()));
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

                this.persistedPreferences = prefs;
                updateLiveData(saveState, SettingsSaveState.SAVED);
                updateLiveData(saveStatusColor, "GREEN");
                updateLiveData(successMessage, "Settings saved successfully!");
                updateLiveData(genericErrorMessage, "");
                reevaluateState();
            } catch (SettingsPreferenceViolationException e) {
                rollbackProvisionalChanges();
                updateLiveData(genericErrorMessage, ErrorMessages.PREFERENCE_VIOLATION.getMessage(e.getMessage()));
                updateLiveData(saveStatusColor, "RED");
                updateLiveData(saveState, SettingsSaveState.ERROR);
            } catch (Exception e) {
                logger.log(Level.SEVERE, ErrorMessages.FAILED_SAVE_SETTINGS.getMessage(e.getMessage()), e);
                rollbackProvisionalChanges();
                updateLiveData(genericErrorMessage, ErrorMessages.FAILED_SAVE_SETTINGS.getMessage(e.getMessage()));
                updateLiveData(saveStatusColor, "RED");
                updateLiveData(saveState, SettingsSaveState.ERROR);
            } finally {
                updateLiveData(isLoading, false);
            }
        });
    }

    public void rollbackProvisionalChanges() {
        SystemPreferences prefs = this.persistedPreferences;
        if (prefs != null) {
            updateLiveData(provisionalThemeMode, prefs.getThemeMode());
            updateLiveData(provisionalDateFormat, DateFormat.fromStringOrDefault(prefs.getDateFormat(), DateFormat.DD_MM_YYYY));
            updateLiveData(provisionalCurrencyFormat, CurrencyFormat.fromString(prefs.getCurrencyFormat()));
            updateLiveData(provisionalStartDayOfMonthInput, String.valueOf(prefs.getStartDayOfMonth()));
            updateLiveData(provisionalIncludeProvisional, prefs.isIncludeProvisional());
        }
        updateLiveData(saveStatusColor, "GRAY");
        updateLiveData(saveState, SettingsSaveState.UNCHANGED);
        resetFieldErrors();
    }

    public void discardProvisionalChanges() {
        rollbackProvisionalChanges();
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

    public LiveData<SystemPreferences> getSystemPreferences() {
        return systemService.getPreferencesLive();
    }

    public LiveData<Map<SystemPreferenceErrorFields, String>> getFieldErrors() {
        return fieldErrors;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getSaveStatusColor() {
        return saveStatusColor;
    }

    public LiveData<SettingsSaveState> getSaveState() {
        return saveState;
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
