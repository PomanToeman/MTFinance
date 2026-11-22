package com.example.mtfinance.src.repositories;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Singleton initializer dedicated to performing asynchronous category seeding and default category repopulation.
 */
@Singleton
public class CategoryInitializer {

    private final CategoryTreeHelper categoryTreeHelper;
    private final Executor executor;

    /**
     * Constructs a CategoryInitializer with required CategoryTreeHelper and background Executor.
     * @param categoryTreeHelper helper managing category tree seeding
     * @param executor background executor for offloading Room database writes
     */
    @Inject
    public CategoryInitializer(CategoryTreeHelper categoryTreeHelper, Executor executor) {
        this.categoryTreeHelper = categoryTreeHelper;
        this.executor = executor;
        populateDefaultCategoriesAsync();
    }

    /**
     * Asynchronously populates default categories if the category table is currently empty.
     */
    public void populateDefaultCategoriesAsync() {
        executor.execute(() -> {
            try {
                categoryTreeHelper.populateDefaultCategories();
            } catch (Exception e) {
                System.err.println("Default category initialization notice: " + e.getMessage());
            }
        });
    }

    /**
     * Synchronously populates default categories for background transaction contexts or unit tests.
     */
    public void populateDefaultCategoriesSync() {
        categoryTreeHelper.populateDefaultCategories();
    }
}
