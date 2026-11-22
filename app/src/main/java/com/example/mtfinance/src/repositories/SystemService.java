package com.example.mtfinance.src.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.messages.ErrorMessages;
import com.example.mtfinance.src.repositories.roomdatabase.SystemPreferencesDao;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service (Read operations) for accessing global system configurations.
 */
@Singleton
public class SystemService {

    private final SystemPreferencesDao systemPreferencesDao;
    private final Executor executor;
    private final Logger logger;

    @Inject
    public SystemService(SystemPreferencesDao systemPreferencesDao, Executor executor, Logger logger) {
        this.systemPreferencesDao = systemPreferencesDao;
        this.executor = executor;
        this.logger = logger;
        ensureDefaultPreferences();
    }

    private void ensureDefaultPreferences() {
        executor.execute(() -> {
            try {
                SystemPreferences existing = systemPreferencesDao.getPreferencesSync();
                if (existing == null) {
                    systemPreferencesDao.savePreferences(new SystemPreferences("SYSTEM"));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, ErrorMessages.DEFAULT_SYSTEM_PREFERENCE_NOTICE.getMessage(e.getMessage()), e);
            }
        });
    }

    public LiveData<SystemPreferences> getPreferencesLive() {
        return Transformations.map(systemPreferencesDao.getPreferencesLive(), prefs -> {
            if (prefs == null) {
                return new SystemPreferences("SYSTEM");
            }
            return prefs;
        });
    }

    public SystemPreferences getPreferencesSync() {
        SystemPreferences prefs = systemPreferencesDao.getPreferencesSync();
        if (prefs == null) {
            return new SystemPreferences("SYSTEM");
        }
        return prefs;
    }

    public DateFormat getDateFormatSync() {
        SystemPreferences prefs = getPreferencesSync();
        return DateFormat.fromStringOrDefault(prefs.getDateFormat(), DateFormat.DD_MM_YYYY);
    }

    public CurrencyFormat getCurrencyFormatSync() {
        SystemPreferences prefs = getPreferencesSync();
        return CurrencyFormat.fromString(prefs.getCurrencyFormat());
    }

    public int getStartDayOfMonthSync() {
        SystemPreferences prefs = getPreferencesSync();
        return Math.max(1, Math.min(31, prefs.getStartDayOfMonth()));
    }

    public boolean isIncludeProvisionalSync() {
        SystemPreferences prefs = getPreferencesSync();
        return prefs.isIncludeProvisional();
    }
}
