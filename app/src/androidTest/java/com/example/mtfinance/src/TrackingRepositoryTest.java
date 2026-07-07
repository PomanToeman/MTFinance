package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.mtfinance.src.roomdatabase.AppDatabase;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TrackingRepositoryTest {
    private AppDatabase database;
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private TrackingRepository trackingRepository;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        categoryRepository = new CategoryRepository(database.categoryDao());
        transactionRepository = new TransactionRepository(database.transactionDao());

        trackingRepository = new TrackingRepository(categoryRepository, transactionRepository, database.categoryTransactionDao());
    }

    @After
    public void closeDb() {
        database.close();
        TrackingUtlis.resetCategoryCounter();
        TrackingUtlis.resetTransactionCounter();
    }

    @Test
    public void testInsertCategory() {
        Category category = new Category("Entertainment", "Movies, games", BigDecimal.valueOf(100));
        trackingRepository.insertCategory(category);

        List<Category> allCategories = categoryRepository.getAllCategories();
        // 3 default (General, Groceries, Utilities) + 1 new
        assertEquals(4, allCategories.size());

        boolean found = false;
        for (Category c : allCategories) {
            if (c.getName().equals("Entertainment")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testInsertTransactionWithCategoryId() {
        Category category = new Category("Dining", "Eating out", BigDecimal.valueOf(200));
        categoryRepository.insert(category);
        Long catId = category.getCategoryId();

        Transaction transaction = new Transaction.Builder("Pizza", BigDecimal.valueOf(25.0)).build();
        trackingRepository.insertTransaction(transaction, catId);

        // Verify transaction is in DB
        List<Transaction> allTransactions = transactionRepository.getAll();
        assertEquals(1, allTransactions.size());
        assertEquals("Pizza", allTransactions.get(0).getName());

        // Verify it's under the category
        List<Category> associatedCategories = trackingRepository.findCategoriesByTransactionId(allTransactions.get(0).getTransactionId());
        boolean found = false;
        for (Category c : associatedCategories) {
            if (c.getCategoryId().equals(catId)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testInsertTransactionDefault() {
        Transaction transaction = new Transaction.Builder("Generic Expense", BigDecimal.valueOf(10.0)).build();
        trackingRepository.insertTransactionDefault(transaction);

        // Verify transaction is in DB
        List<Transaction> allTransactions = transactionRepository.getAll();
        assertEquals(1, allTransactions.size());

        // Verify it's under General Category
        Category general = categoryRepository.getGeneralCategory();
        List<Category> associatedCategories = trackingRepository.findCategoriesByTransactionId(allTransactions.get(0).getTransactionId());
        boolean found = false;
        for (Category c : associatedCategories) {
            if (c.getCategoryId().equals(general.getCategoryId())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testFindCategoriesByTransactionId() {
        Category cat1 = new Category("Cat1", "", BigDecimal.valueOf(100));
        Category cat2 = new Category("Cat2", "", BigDecimal.valueOf(100));
        categoryRepository.insert(cat1);
        categoryRepository.insert(cat2);

        Transaction transaction = new Transaction.Builder("Shared Transaction", BigDecimal.valueOf(50.0)).build();
        trackingRepository.insertTransaction(transaction, cat1.getCategoryId());

        // Associate with second category
        com.example.mtfinance.src.roomdatabase.CategoryTransactionCrossRef crossRef = 
            new com.example.mtfinance.src.roomdatabase.CategoryTransactionCrossRef(cat2.getCategoryId(), transaction.getTransactionId());
        database.categoryTransactionDao().insertCrossRef(crossRef);

        List<Category> foundCategories = trackingRepository.findCategoriesByTransactionId(transaction.getTransactionId());
        assertEquals(2, foundCategories.size());

        boolean found1 = false;
        boolean found2 = false;
        for (Category c : foundCategories) {
            if (c.getName().equals("Cat1")) found1 = true;
            if (c.getName().equals("Cat2")) found2 = true;
        }
        assertTrue(found1);
        assertTrue(found2);
    }
}
