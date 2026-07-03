package com.example.mtfinance.src;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;



import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RunWith(AndroidJUnit4.class)
public class TransactionRepositoryTest {
    private AppDatabase database;
    private TransactionRepository repository;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        TransactionDao transactionDao = database.transactionDao();
        repository = new TransactionRepository(transactionDao);
    }

    @After
    public void closeDb() {
        database.close();
        TrackingUtlis.resetTransactionCounter();
    }


    @Test
    public void testInsertTransaction() {
        Transaction transaction = new Transaction.Builder("Test Transaction", BigDecimal.valueOf(100)).build();
        Long id = repository.insert(transaction);
        assertTrue(id > 0);

    }
}
