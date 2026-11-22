package com.example.mtfinance.src.repositories;

import java.util.logging.Logger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module providing application-wide java.util.logging.Logger dependency.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class LoggingModule {

    @Provides
    @Singleton
    public static Logger provideLogger() {
        return Logger.getLogger("MTFinance");
    }
}
