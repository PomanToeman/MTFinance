package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.repositories.roomdatabase.SystemPreferencesDao;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository (Write operations) for modifying global system configurations.
 */
@Singleton
public class SystemRepository {

    private final SystemPreferencesDao systemPreferencesDao;
    private final Executor executor;

    @Inject
    public SystemRepository(SystemPreferencesDao systemPreferencesDao, Executor executor) {
        this.systemPreferencesDao = systemPreferencesDao;
        this.executor = executor;
    }

    public void updateThemeMode(String themeMode) {
        executor.execute(() -> {
            SystemPreferences prefs = getOrCreatePrefs();
            prefs.setThemeMode(themeMode);
            systemPreferencesDao.savePreferences(prefs);
        });
    }

    public void updateDateFormat(DateFormat format) {
        if (format == null) return;
        executor.execute(() -> {
            SystemPreferences prefs = getOrCreatePrefs();
            prefs.setDateFormat(format.name());
            systemPreferencesDao.savePreferences(prefs);
        });
    }

    public void updateCurrencyFormat(CurrencyFormat currencyFormat) {
        if (currencyFormat == null) return;
        executor.execute(() -> {
            SystemPreferences prefs = getOrCreatePrefs();
            prefs.setCurrencyFormat(currencyFormat.name());
            prefs.setCurrencySymbol(currencyFormat.getSymbol());
            systemPreferencesDao.savePreferences(prefs);
        });
    }

    public void updateStartDayOfMonth(int startDay) {
        final int validatedDay = Math.max(1, Math.min(31, startDay));
        executor.execute(() -> {
            SystemPreferences prefs = getOrCreatePrefs();
            prefs.setStartDayOfMonth(validatedDay);
            systemPreferencesDao.savePreferences(prefs);
        });
    }

    public void updateIncludeProvisional(boolean includeProvisional) {
        executor.execute(() -> {
            SystemPreferences prefs = getOrCreatePrefs();
            prefs.setIncludeProvisional(includeProvisional);
            systemPreferencesDao.savePreferences(prefs);
        });
    }

    private SystemPreferences getOrCreatePrefs() {
        SystemPreferences prefs = systemPreferencesDao.getPreferencesSync();
        if (prefs == null) {
            prefs = new SystemPreferences("SYSTEM");
        }
        return prefs;
    }

    public void updatePreferences(SystemPreferences preferences) {
        if (preferences == null) return;
        executor.execute(() -> systemPreferencesDao.savePreferences(preferences));
    }
}
