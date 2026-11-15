package com.example.mtfinance.src.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TransactionRepositoryTest {

    private static final String TRANSACTION_NAME_DINNER = "Dinner";
    private static final String TRANSACTION_NAME_LUNCH = "Lunch";
    private static final String TRANSACTION_NAME_COFFEE = "Coffee";
    private static final String TRANSACTION_NAME_GAS = "Gas";
    private static final BigDecimal AMOUNT_FIFTY = BigDecimal.valueOf(50.0);
    private static final BigDecimal AMOUNT_FIFTEEN = BigDecimal.valueOf(15.0);
    private static final BigDecimal AMOUNT_FIVE = BigDecimal.valueOf(5.0);
    private static final BigDecimal AMOUNT_FORTY = BigDecimal.valueOf(40.0);
    private static final long NON_EXISTENT_ID = 999L;

    private AppDatabase database;
    private TransactionDao transactionDao;
    private TransactionRepository repository;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        transactionDao = database.transactionDao();
        repository = new TransactionRepository(transactionDao);
    }

    @After
    public void tearDown() {
        database.close();
        TrackingUtlis.resetTransactionCounter();
    }

    @Test
    public void insert_SingleValidTransaction_ReturnsValidId() {
        // Arrange
        Transaction transaction = new Transaction.Builder(TRANSACTION_NAME_DINNER, AMOUNT_FIFTY).build();

        // Act
        Long id = repository.insert(transaction);

        // Assert
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void insert_MultipleValidTransactions_PersistsAllCorrectly() {
        // Arrange
        Transaction t1 = new Transaction.Builder(TRANSACTION_NAME_LUNCH, AMOUNT_FIFTEEN).build();
        Transaction t2 = new Transaction.Builder(TRANSACTION_NAME_COFFEE, AMOUNT_FIVE).build();

        // Act
        repository.insert(t1);
        repository.insert(t2);
        List<Transaction> allTransactions = repository.getAll();

        // Assert
        assertEquals(2, allTransactions.size());
    }

    @Test
    public void getAll_EmptyDatabase_ReturnsEmptyList() {
        // Arrange / Act
        List<Transaction> allTransactions = repository.getAll();

        // Assert
        assertTrue(allTransactions.isEmpty());
    }

    @Test
    public void getAll_PopulatedDatabase_ReturnsAllTransactions() {
        // Arrange
        Transaction t1 = new Transaction.Builder("First", BigDecimal.valueOf(10.0)).build();
        Transaction t2 = new Transaction.Builder("Second", BigDecimal.valueOf(20.0)).build();
        repository.insert(t1);
        repository.insert(t2);

        // Act
        List<Transaction> allTransactions = repository.getAll();

        // Assert
        assertEquals(2, allTransactions.size());
    }

    @Test
    public void getById_ExistingTransactionId_ReturnsCorrectTransaction() {
        // Arrange
        Transaction transaction = new Transaction.Builder(TRANSACTION_NAME_GAS, AMOUNT_FORTY).build();
        Long id = repository.insert(transaction);

        // Act
        Transaction retrievedTransaction = repository.getById(id);

        // Assert
        assertNotNull(retrievedTransaction);
        assertEquals(TRANSACTION_NAME_GAS, retrievedTransaction.getName());
    }

    @Test
    public void getById_NonExistentTransactionId_ReturnsNull() {
        // Arrange / Act
        Transaction retrievedTransaction = repository.getById(NON_EXISTENT_ID);

        // Assert
        assertNull(retrievedTransaction);
    }

    @Test
    public void getGeneratedHash_ValidTransaction_GeneratesExpectedHashFormat() {
        // Arrange
        Transaction t1 = new Transaction.Builder(TRANSACTION_NAME_DINNER, AMOUNT_FIFTY).build();

        // Act
        String hash = t1.getGeneratedHash();
        Transaction t2 = new Transaction.Builder(TRANSACTION_NAME_DINNER, AMOUNT_FIFTY)
                .date(t1.getDate())
                .build();

        // Assert
        assertNotNull(hash);
        assertTrue(hash.contains(TRANSACTION_NAME_DINNER.toLowerCase()));
        assertTrue(hash.contains(AMOUNT_FIFTY.toString()));
        assertEquals(hash, t2.getGeneratedHash());
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException.class)
    public void insert_DuplicateHashTransaction_ThrowsConstraintException() {
        // Arrange
        Transaction t1 = new Transaction.Builder(TRANSACTION_NAME_DINNER, AMOUNT_FIFTY).build();
        repository.insert(t1);

        Transaction t2 = new Transaction.Builder(TRANSACTION_NAME_DINNER, AMOUNT_FIFTY)
                .date(t1.getDate())
                .build();

        // Act & Assert
        repository.insert(t2);
    }
}
