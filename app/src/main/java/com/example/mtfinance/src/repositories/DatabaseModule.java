package com.example.mtfinance.src.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionDao;
import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dagger.Binds;
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
        return Room.databaseBuilder(application, AppDatabase.class, "app_database").allowMainThreadQueries().build();
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
    @Singleton
    static CategoryRepository provideCategoryRepository(CategoryDao categoryDao) {
        return new CategoryRepository(categoryDao);
    }

    @Provides
    @Singleton
    static TransactionRepository provideTransactionRepository(TransactionDao transactionDao) {
        return new TransactionRepository(transactionDao);
    }

    @Provides
    @Singleton
    static TrackingRepository provideTrackingRepository(CategoryRepository categoryRepository, TransactionRepository transactionRepository, CategoryTransactionDao categoryWithTransactionsDao) {
        return new TrackingRepository(categoryRepository, transactionRepository, categoryWithTransactionsDao);
    }

    @Binds
    @Singleton
    public abstract TrackingWriteRepository bindTrackingWriteRepository(TrackingRepository trackingRepository);

    @Binds
    @Singleton
    public abstract CategoryService bindCategoryService(CategoryServiceImpl categoryServiceImpl);

    @Binds
    @Singleton
    public abstract TransactionService bindTransactionService(TransactionServiceImpl transactionServiceImpl);

    @Binds
    @Singleton
    public abstract TrackingService bindTrackingService(TrackingServiceImpl trackingServiceImpl);

    @Provides
    @Singleton
    public static Executor provideExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
