package com.example.mtfinance.src.trackingengine;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing global system configurations and preferences.
 */
@Entity(tableName = "system_preferences")
public class SystemPreferences {

    @PrimaryKey
    @NonNull
    public Integer id = 1;

    public String themeMode = "SYSTEM";

    public String currencySymbol = "$";

    public String currencyFormat = "NZD";

    public String dateFormat = "DD_MM_YYYY";

    public int startDayOfMonth = 1;

    public boolean includeProvisional = true;

    public SystemPreferences() {
    }

    public SystemPreferences(String themeMode) {
        this.id = 1;
        this.themeMode = themeMode != null ? themeMode : "SYSTEM";
        this.currencySymbol = "$";
        this.currencyFormat = "NZD";
        this.dateFormat = "DD_MM_YYYY";
        this.startDayOfMonth = 1;
        this.includeProvisional = true;
    }

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public String getCurrencyFormat() {
        return currencyFormat;
    }

    public void setCurrencyFormat(String currencyFormat) {
        this.currencyFormat = currencyFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getStartDayOfMonth() {
        return startDayOfMonth;
    }

    public void setStartDayOfMonth(int startDayOfMonth) {
        this.startDayOfMonth = Math.max(1, Math.min(31, startDayOfMonth));
    }

    public boolean isIncludeProvisional() {
        return includeProvisional;
    }

    public void setIncludeProvisional(boolean includeProvisional) {
        this.includeProvisional = includeProvisional;
    }
}
