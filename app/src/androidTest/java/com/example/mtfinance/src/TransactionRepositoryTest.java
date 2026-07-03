package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TransactionRepositoryTest {
    private AppDatabase database;
    private TransactionDao transactionDao;
    private TransactionRepository repository;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        transactionDao = database.transactionDao();
        repository = new TransactionRepository(transactionDao);
    }

    @After
    public void closeDb() {
        database.close();
        TrackingUtlis.resetTransactionCounter();
    }

    // ========================================
    // INSERT TESTS
    // ========================================

    @Test
    public void testInsertTransaction() {
        Transaction transaction = new Transaction.Builder("Dinner", BigDecimal.valueOf(50.0)).build();
        Long id = repository.insert(transaction);
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testInsertMultipleTransactions() {
        Transaction t1 = new Transaction.Builder("Lunch", BigDecimal.valueOf(15.0)).build();
        Transaction t2 = new Transaction.Builder("Coffee", BigDecimal.valueOf(5.0)).build();
        
        repository.insert(t1);
        repository.insert(t2);
        
        List<Transaction> all = repository.getAll();
        assertEquals(2, all.size());
    }

    // ========================================
    // GET ALL TESTS
    // ========================================

    @Test
    public void testGetAllTransactionsEmpty() {
        List<Transaction> all = repository.getAll();
        assertTrue(all.isEmpty());
    }

    @Test
    public void testGetAllTransactions() {
        Transaction t1 = new Transaction.Builder("First", BigDecimal.valueOf(10.0)).build();
        Transaction t2 = new Transaction.Builder("Second", BigDecimal.valueOf(20.0)).build();
        
        repository.insert(t1);
        repository.insert(t2);
        
        List<Transaction> all = repository.getAll();
        assertEquals(2, all.size());
    }

    // ========================================
    // GET BY ID TESTS
    // ========================================

    @Test
    public void testGetById() {
        Transaction transaction = new Transaction.Builder("Gas", BigDecimal.valueOf(40.0)).build();
        Long id = repository.insert(transaction);
        
        Transaction retrieved = repository.getById(id);
        assertNotNull(retrieved);
        assertEquals("Gas", retrieved.getName());
    }

    @Test
    public void testGetByIdNotFound() {
        Transaction retrieved = repository.getById(999L);
        assertNull(retrieved);
    }
}
