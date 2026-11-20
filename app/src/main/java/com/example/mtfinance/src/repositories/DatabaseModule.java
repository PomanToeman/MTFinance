package com.example.mtfinance.src.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.repositories.roomdatabase.SystemPreferencesDao;
import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import jakarta.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DatabaseModule {

    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(Application application) {
        return Room.databaseBuilder(application, AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public static CategoryDao provideCategoryDao(AppDatabase appDatabase) {
        return appDatabase.categoryDao();
    }

    @Provides
    public static TransactionDao provideTransactionDao(@NonNull AppDatabase appDatabase) {
        return appDatabase.transactionDao();
    }

    @Provides
    public static CategoryTransactionDao provideCategoryTransactionDao(AppDatabase appDatabase) {
        return appDatabase.categoryTransactionDao();
    }

    @Provides
    public static SystemPreferencesDao provideSystemPreferencesDao(AppDatabase appDatabase) {
        return appDatabase.systemPreferencesDao();
    }

    @Provides
    @Singleton
    public static CategoryTreeHelper provideCategoryTreeHelper(CategoryDao categoryDao) {
        return new CategoryTreeHelper(categoryDao);
    }

    @Provides
    @Singleton
    public static CategoryInitializer provideCategoryInitializer(CategoryTreeHelper categoryTreeHelper, Executor executor) {
        return new CategoryInitializer(categoryTreeHelper, executor);
    }

    @Provides
    @Singleton
    public static CategoryService provideCategoryService(CategoryDao categoryDao, CategoryTreeHelper categoryTreeHelper) {
        return new CategoryService(categoryDao, categoryTreeHelper);
    }

    @Provides
    @Singleton
    static CategoryRepository provideCategoryRepository(AppDatabase appDatabase,
                                                        CategoryDao categoryDao,
                                                        CategoryTreeHelper categoryTreeHelper,
                                                        CategoryInitializer categoryInitializer) {
        return new CategoryRepository(appDatabase, categoryDao, categoryTreeHelper, categoryInitializer);
    }

    @Provides
    @Singleton
    static TransactionRepository provideTransactionRepository(TransactionDao transactionDao) {
        return new TransactionRepository(transactionDao);
    }

    @Provides
    @Singleton
    static TrackingRepository provideTrackingRepository(CategoryRepository categoryRepository,
                                                       TransactionRepository transactionRepository,
                                                       CategoryTransactionDao categoryWithTransactionsDao,
                                                       CategoryDao categoryDao,
                                                       TransactionDao transactionDao,
                                                       CategoryTreeHelper categoryTreeHelper) {
        return new TrackingRepository(categoryRepository, transactionRepository, categoryWithTransactionsDao, categoryDao, transactionDao, categoryTreeHelper);
    }

    @Provides
    @Singleton
    static SystemService provideSystemService(SystemPreferencesDao systemPreferencesDao, Executor executor) {
        return new SystemService(systemPreferencesDao, executor);
    }

    @Provides
    @Singleton
    static SystemRepository provideSystemRepository(SystemPreferencesDao systemPreferencesDao, Executor executor) {
        return new SystemRepository(systemPreferencesDao, executor);
    }

    @Provides
    @Singleton
    public static Executor provideExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
