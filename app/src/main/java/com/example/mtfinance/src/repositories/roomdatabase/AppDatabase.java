package com.example.mtfinance.src.repositories.roomdatabase;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.SystemPreferences;
import com.example.mtfinance.src.trackingengine.Transaction;

@Database(entities = {Category.class, Transaction.class, CategoryTransactionCrossRef.class, SystemPreferences.class}, version = 5)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract CategoryDao categoryDao();
    public abstract TransactionDao transactionDao();
    public abstract CategoryTransactionDao categoryTransactionDao();
    public abstract SystemPreferencesDao systemPreferencesDao();
}
