package com.example.mtfinance.src.modules;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.math.BigDecimal;
import java.util.List;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import jakarta.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class defaultCategoriesModule {

    private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(1000);
    @Provides
    @Singleton
    public Category defaultIncomeCategory() {
        return new Category("General Income", "General Income", DEFAULT_BUDGET, TrackingType.INCOME);
    }

    @Provides
    @Singleton
    public Category defaultExpenseCategory() {
        return new Category("General Category", "General tracking for all categories", DEFAULT_BUDGET, TrackingType.EXPENSE);
    }

    @Provides
    @Singleton
    public Category defaultAccountTransferCategory() {
        return new Category("Account Transfer", "Account Transfers", DEFAULT_BUDGET, TrackingType.ACCOUNT_TRANSFERS);
    }


    @Provides
    @Singleton
    public List<Category> defaultExpenseCategories() {
        return List.of(
                new Category("Groceries", "Grocery shopping", DEFAULT_BUDGET, TrackingType.EXPENSE),
                new Category("Utilities", "Utilities", DEFAULT_BUDGET, TrackingType.EXPENSE)
        );
    }
}
