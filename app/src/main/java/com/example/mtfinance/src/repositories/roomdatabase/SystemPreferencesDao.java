package com.example.mtfinance.src.repositories.roomdatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mtfinance.src.trackingengine.SystemPreferences;

@Dao
public interface SystemPreferencesDao {

    @Query("SELECT * FROM system_preferences WHERE id = 1")
    LiveData<SystemPreferences> getPreferencesLive();

    @Query("SELECT * FROM system_preferences WHERE id = 1")
    SystemPreferences getPreferencesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void savePreferences(SystemPreferences preferences);
}
