package com.example.mtfinance.src.repositories;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TrackingStressTest {

    private static final String STRESS_CATEGORY_NAME = "Stress Test";
    private static final String STRESS_CATEGORY_DESC = "Large volume of transactions";
    private static final BigDecimal STRESS_CATEGORY_BUDGET = BigDecimal.valueOf(100000);
    private static final BigDecimal AMOUNT_PER_TRANS = BigDecimal.valueOf(1.50);
    private static final int TRANSACTION_COUNT = 1000;

    private AppDatabase database;
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private TrackingRepository trackingRepository;
    private CategoryService categoryService;
    private TransactionService transactionService;
    private TrackingService trackingService;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        categoryService = new CategoryService(database.categoryDao(), true);
        transactionService = new TransactionService(database.transactionDao());
        trackingService = new TrackingService(database.categoryDao(), database.transactionDao(), database.categoryTransactionDao(), categoryService);

        categoryRepository = new CategoryRepository(database.categoryDao());
        transactionRepository = new TransactionRepository(database.transactionDao());
        trackingRepository = new TrackingRepository(categoryRepository, transactionRepository, database.categoryTransactionDao(), database.categoryDao(), database.transactionDao());
    }

    @After
    public void tearDown() {
        database.close();
        TrackingUtlis.resetCategoryCounter();
        TrackingUtlis.resetTransactionCounter();
    }

    @Test
    public void stressTestTransactions_LargeVolumeOfInserts_CalculatesTotalAndSizeAccurately() {
        Category stressCat = new Category(STRESS_CATEGORY_NAME, STRESS_CATEGORY_DESC, STRESS_CATEGORY_BUDGET, TrackingType.EXPENSE);
        categoryRepository.insert(stressCat);

        BigDecimal expectedTotal = BigDecimal.ZERO;

        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            Transaction transaction = new Transaction.Builder("Trans " + i, AMOUNT_PER_TRANS).build();
            trackingRepository.insertTransaction(transaction, stressCat.getCategoryId());
            expectedTotal = expectedTotal.add(AMOUNT_PER_TRANS);
        }

        BigDecimal calculatedTotal = trackingService.getTotalInCategory(stressCat, false);
        List<Long> transactionIds = trackingService.getTransactionIdsForCategory(stressCat.getCategoryId());

        assertEquals(0, expectedTotal.compareTo(calculatedTotal));
        assertEquals(TRANSACTION_COUNT, transactionIds.size());
    }
}
