package com.example.mtfinance.src.modules;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.example.mtfinance.src.repositories.CategoryRepository;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionRepository;
import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
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
public class databaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(Application application) {
        return Room.databaseBuilder(application, AppDatabase.class, "app_database").allowMainThreadQueries().build();
    }

    @Provides
    public CategoryDao provideCategoryDao(AppDatabase appDatabase) {
        return appDatabase.categoryDao();
    }

    @Provides
    public TransactionDao provideTransactionDao(@NonNull AppDatabase appDatabase) {
        return appDatabase.transactionDao();
    }

    @Provides
    public CategoryTransactionDao provideCategoryTransactionDao(AppDatabase appDatabase) {
        return appDatabase.categoryTransactionDao();
    }

    @Provides
    @Singleton
    public CategoryRepository provideCategoryRepository(CategoryDao categoryDao) {
        return new CategoryRepository(categoryDao);
    }

    @Provides
    @Singleton
    public TransactionRepository provideTransactionRepository(TransactionDao transactionDao) {
        return new TransactionRepository(transactionDao);
    }

    @Provides
    @Singleton
    public TrackingRepository provideTrackingRepository(CategoryRepository categoryRepository, TransactionRepository transactionRepository, CategoryTransactionDao categoryWithTransactionsDao) {
        return new TrackingRepository(categoryRepository, transactionRepository, categoryWithTransactionsDao);
    }

    @Provides
    @Singleton
    public Executor provideExecutor() {
        return Executors.newFixedThreadPool(4);
    }










}
