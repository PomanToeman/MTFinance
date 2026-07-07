package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.mtfinance.src.roomdatabase.AppDatabase;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void testFindTotalInCategorySimple() {
        Category category = new Category("PaknSave", "Food stuff", BigDecimal.valueOf(500));
        categoryRepository.insert(category);

        Transaction t1 = new Transaction.Builder("Apple", BigDecimal.valueOf(2.50)).build();
        Transaction t2 = new Transaction.Builder("Milk", BigDecimal.valueOf(3.50)).build();

        trackingRepository.insertTransaction(t1, category.getCategoryId());
        trackingRepository.insertTransaction(t2, category.getCategoryId());

        BigDecimal total = trackingRepository.findTotalInCategory(category, false);
        assertEquals(0, BigDecimal.valueOf(6.00).compareTo(total));
    }

    @Test
    public void testFindTotalInCategoryWithSubcategories() {
        Category parent = new Category("Transport", "Commute", BigDecimal.valueOf(300));
        Category child = new Category("Fuel", "Gasoline", BigDecimal.valueOf(100));
        categoryRepository.insert(parent);
        child.setParent(parent);
        categoryRepository.insert(child);

        Transaction t1 = new Transaction.Builder("Train ticket", BigDecimal.valueOf(15.00)).build();
        Transaction t2 = new Transaction.Builder("Petrol", BigDecimal.valueOf(45.00)).build();

        trackingRepository.insertTransaction(t1, parent.getCategoryId());
        trackingRepository.insertTransaction(t2, child.getCategoryId());

        // Total for parent including subcategories
        BigDecimal totalWithSubs = trackingRepository.findTotalInCategory(parent, true);
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(totalWithSubs));

        // Total for parent excluding subcategories
        BigDecimal totalWithoutSubs = trackingRepository.findTotalInCategory(parent, false);
        assertEquals(0, BigDecimal.valueOf(15.00).compareTo(totalWithoutSubs));
    }

    @Test
    public void testGetCategoryWithTransactionsByCategoryId() {
        Category category = new Category("TestCat", "", BigDecimal.valueOf(100));
        categoryRepository.insert(category);
        Transaction t = new Transaction.Builder("T1", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(t, category.getCategoryId());

        CategoryWithTransactions result = trackingRepository.getCategoryWithTransactionsByCategoryId(category.getCategoryId());
        assertNotNull(result);
        assertEquals(category.getCategoryId(), result.category.getCategoryId());
        assertEquals(1, result.transactions.size());
    }

    @Test
    public void testGetCategoryWithTransactionsByParentId() {
        Category parent = new Category("Parent", "", BigDecimal.valueOf(100));
        categoryRepository.insert(parent);
        Category child = new Category("Child", "", BigDecimal.valueOf(50));
        child.setParent(parent);
        categoryRepository.insert(child);

        List<CategoryWithTransactions> results = trackingRepository.getCategoryWithTransactionsByParentId(parent.getCategoryId());
        assertEquals(1, results.size());
        assertEquals(child.getCategoryId(), results.get(0).category.getCategoryId());
    }

    @Test
    public void testGetTransactionIdsForCategory() {
        Category category = new Category("TestCat", "", BigDecimal.valueOf(100));
        categoryRepository.insert(category);
        Transaction t1 = new Transaction.Builder("T1", BigDecimal.valueOf(10)).build();
        Transaction t2 = new Transaction.Builder("T2", BigDecimal.valueOf(20)).build();
        trackingRepository.insertTransaction(t1, category.getCategoryId());
        trackingRepository.insertTransaction(t2, category.getCategoryId());

        List<Long> ids = trackingRepository.getTransactionIdsForCategory(category.getCategoryId());
        assertEquals(2, ids.size());
        assertTrue(ids.contains(t1.getTransactionId()));
        assertTrue(ids.contains(t2.getTransactionId()));
    }

    @Test
    public void testGetAllCategoriesWithTransactions() throws InterruptedException {
        Category cat = new Category("Cat", "", BigDecimal.valueOf(100));
        trackingRepository.insertCategory(cat);

        List<CategoryWithTransactions> result = getValue(trackingRepository.getAllCategoriesWithTransactions());
        assertNotNull(result);
        // 3 default + 1 new = 4
        assertEquals(4, result.size());
    }

    @Test
    public void testGetCategoriesWithTransactionsByIds() throws InterruptedException {
        Category cat1 = new Category("Cat1", "", BigDecimal.valueOf(100));
        Category cat2 = new Category("Cat2", "", BigDecimal.valueOf(100));
        categoryRepository.insert(cat1);
        categoryRepository.insert(cat2);

        List<Long> ids = Arrays.asList(cat1.getCategoryId(), cat2.getCategoryId());
        List<CategoryWithTransactions> result = getValue(trackingRepository.getCategoriesWithTransactionsByIds(ids));
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    private <T> T getValue(LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final androidx.lifecycle.Observer<T> observer = new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T t) {
                data[0] = t;
                latch.countDown();
            }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            liveData.observeForever(observer);
        });
        latch.await(2, TimeUnit.SECONDS);
        return (T) data[0];
    }

    @Test
    public void testDeepAndWideCategoryHierarchyWithManyTransactions() {
        // Create root
        Category root = new Category("Business", "All business expenses", BigDecimal.valueOf(10000));
        categoryRepository.insert(root);

        BigDecimal expectedTotal = BigDecimal.ZERO;

        // Level 1: 5 children
        for (int i = 0; i < 5; i++) {
            Category l1 = new Category("Dept " + i, "Level 1 Department", BigDecimal.valueOf(1000));
            l1.setParent(root);
            categoryRepository.insert(l1);

            Transaction t1 = new Transaction.Builder("L1 Trans " + i, BigDecimal.valueOf(100)).build();
            trackingRepository.insertTransaction(t1, l1.getCategoryId());
            expectedTotal = expectedTotal.add(t1.getAmount());

            // Level 2: 4 grandchildren per level 1 child (Total 20 grandchildren)
            for (int j = 0; j < 4; j++) {
                Category l2 = new Category("Team " + i + "-" + j, "Level 2 Team", BigDecimal.valueOf(200));
                l2.setParent(l1);
                categoryRepository.insert(l2);

                Transaction t2 = new Transaction.Builder("L2 Trans " + i + "-" + j, BigDecimal.valueOf(50)).build();
                trackingRepository.insertTransaction(t2, l2.getCategoryId());
                expectedTotal = expectedTotal.add(t2.getAmount());
            }
        }

        // Verify root total including all descendants
        BigDecimal total = trackingRepository.findTotalInCategory(root, true);
        assertEquals(0, expectedTotal.compareTo(total));
    }

    @Test
    public void testStressTestingTransactions() {
        Category stressCat = new Category("Stress Test", "Large volume of transactions", BigDecimal.valueOf(100000));
        categoryRepository.insert(stressCat);

        int count = 100;
        BigDecimal amountPerTrans = BigDecimal.valueOf(1.50);
        BigDecimal expectedTotal = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            Transaction t = new Transaction.Builder("Trans " + i, amountPerTrans).build();
            trackingRepository.insertTransaction(t, stressCat.getCategoryId());
            expectedTotal = expectedTotal.add(amountPerTrans);
        }

        BigDecimal total = trackingRepository.findTotalInCategory(stressCat, false);
        assertEquals(0, expectedTotal.compareTo(total));

        List<Long> transIds = trackingRepository.getTransactionIdsForCategory(stressCat.getCategoryId());
        assertEquals(count, transIds.size());
    }
}
