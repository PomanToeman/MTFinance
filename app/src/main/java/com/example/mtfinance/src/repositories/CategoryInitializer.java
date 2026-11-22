package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.messages.ErrorMessages;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Singleton initializer dedicated to performing asynchronous category seeding and default category repopulation.
 */
@Singleton
public class CategoryInitializer {

    private final CategoryTreeHelper categoryTreeHelper;
    private final Executor executor;
    private final Logger logger;

    /**
     * Constructs a CategoryInitializer with required CategoryTreeHelper, background Executor, and Logger.
     * @param categoryTreeHelper helper managing category tree seeding
     * @param executor background executor for offloading Room database writes
     * @param logger injected logger instance
     */
    @Inject
    public CategoryInitializer(CategoryTreeHelper categoryTreeHelper, Executor executor, Logger logger) {
        this.categoryTreeHelper = categoryTreeHelper;
        this.executor = executor;
        this.logger = logger;
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
                logger.log(Level.WARNING, ErrorMessages.DEFAULT_CATEGORY_INIT_NOTICE.getMessage(e.getMessage()), e);
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
