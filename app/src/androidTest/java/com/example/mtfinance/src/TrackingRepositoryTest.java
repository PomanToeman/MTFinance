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
import com.example.mtfinance.src.trackingengine.TrackingType;
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
        Category category = new Category("Entertainment", "Movies, games", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        trackingRepository.insertCategory(category);

        List<Category> allCategories = categoryRepository.getAllCategories();
        // 3 default (General, Groceries, Utilities) + 1 new
        assertEquals(6, allCategories.size());

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
        Category category = new Category("Dining", "Eating out", BigDecimal.valueOf(200), TrackingType.EXPENSE);
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
        Category cat1 = new Category("Cat1", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category cat2 = new Category("Cat2", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
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
        Category category = new Category("PaknSave", "Food stuff", BigDecimal.valueOf(500), TrackingType.EXPENSE);
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
        Category parent = new Category("Transport", "Commute", BigDecimal.valueOf(300), TrackingType.EXPENSE);
        Category child = new Category("Fuel", "Gasoline", BigDecimal.valueOf(100), TrackingType.EXPENSE);
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
        Category category = new Category("TestCat", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
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
        Category parent = new Category("Parent", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        Category child = new Category("Child", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        child.setParent(parent);
        categoryRepository.insert(child);

        List<CategoryWithTransactions> results = trackingRepository.getCategoryWithTransactionsByParentId(parent.getCategoryId());
        assertEquals(1, results.size());
        assertEquals(child.getCategoryId(), results.get(0).category.getCategoryId());
    }

    @Test
    public void testGetTransactionIdsForCategory() {
        Category category = new Category("TestCat", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
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
        Category cat = new Category("Cat", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);

        List<CategoryWithTransactions> result = getValue(trackingRepository.getAllCategoriesWithTransactions());
        assertNotNull(result);
        // 5 default + 1 new = 4
        assertEquals(6, result.size());
    }

    @Test
    public void testGetCategoriesWithTransactionsByIds() throws InterruptedException {
        Category cat1 = new Category("Cat1", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category cat2 = new Category("Cat2", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
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
        Category root = new Category("Business", "All business expenses", BigDecimal.valueOf(10000), TrackingType.EXPENSE);
        categoryRepository.insert(root);

        BigDecimal expectedTotal = BigDecimal.ZERO;

        // Level 1: 5 children
        for (int i = 0; i < 5; i++) {
            Category l1 = new Category("Dept " + i, "Level 1 Department", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
            l1.setParent(root);
            categoryRepository.insert(l1);

            Transaction t1 = new Transaction.Builder("L1 Trans " + i, BigDecimal.valueOf(100)).build();
            trackingRepository.insertTransaction(t1, l1.getCategoryId());
            expectedTotal = expectedTotal.add(t1.getAmount());

            // Level 2: 4 grandchildren per level 1 child (Total 20 grandchildren)
            for (int j = 0; j < 4; j++) {
                Category l2 = new Category("Team " + i + "-" + j, "Level 2 Team", BigDecimal.valueOf(200), TrackingType.EXPENSE);
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
        Category stressCat = new Category("Stress Test", "Large volume of transactions", BigDecimal.valueOf(100000), TrackingType.EXPENSE);
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

    @Test
    public void testDeepHierarchyBranchIsolationAndRestoration() {
        // Depth 1: General Category (Already exists)
        Category general = categoryRepository.getGeneralCategory();

        // Depth 2: Main Branch
        Category l2Main = new Category("L2 Main", "Main branch level 2", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
        l2Main.setParent(general);
        categoryRepository.insert(l2Main);

        // Depth 3: Main Branch
        Category l3Main = new Category("L3 Main", "Main branch level 3", BigDecimal.valueOf(800), TrackingType.EXPENSE);
        l3Main.setParent(l2Main);
        categoryRepository.insert(l3Main);

        // Depth 4: Main Branch
        Category l4Main = new Category("L4 Main", "Main branch level 4", BigDecimal.valueOf(600), TrackingType.EXPENSE);
        l4Main.setParent(l3Main);
        categoryRepository.insert(l4Main);

        // Depth 5: Main Branch
        Category l5Main = new Category("L5 Main", "Main branch level 5", BigDecimal.valueOf(400), TrackingType.EXPENSE);
        l5Main.setParent(l4Main);
        categoryRepository.insert(l5Main);

        // Different Branch (Isolation Test)
        Category l2Other = new Category("L2 Other", "Other branch level 2", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
        l2Other.setParent(general);
        categoryRepository.insert(l2Other);

        // Add Transactions
        BigDecimal mainTotal = BigDecimal.ZERO;

        Transaction t2 = new Transaction.Builder("T2 Main", BigDecimal.valueOf(20)).build();
        trackingRepository.insertTransaction(t2, l2Main.getCategoryId());
        mainTotal = mainTotal.add(t2.getAmount());

        Transaction t3 = new Transaction.Builder("T3 Main", BigDecimal.valueOf(30)).build();
        trackingRepository.insertTransaction(t3, l3Main.getCategoryId());
        mainTotal = mainTotal.add(t3.getAmount());

        Transaction t4 = new Transaction.Builder("T4 Main", BigDecimal.valueOf(40)).build();
        trackingRepository.insertTransaction(t4, l4Main.getCategoryId());
        mainTotal = mainTotal.add(t4.getAmount());

        Transaction t5 = new Transaction.Builder("T5 Main", BigDecimal.valueOf(50)).build();
        trackingRepository.insertTransaction(t5, l5Main.getCategoryId());
        mainTotal = mainTotal.add(t5.getAmount());

        // Transaction in the other branch (Should be isolated)
        Transaction tOther = new Transaction.Builder("T Other", BigDecimal.valueOf(1000)).build();
        trackingRepository.insertTransaction(tOther, l2Other.getCategoryId());

        // Transaction in the root (Should be isolated from L2 total)
        Transaction tRoot = new Transaction.Builder("T Root", BigDecimal.valueOf(500)).build();
        trackingRepository.insertTransaction(tRoot, general.getCategoryId());

        // 1. Check isolation: Total for L2 Main should only include its descendants
        BigDecimal resultTotal = trackingRepository.findTotalInCategory(l2Main, true);
        assertEquals(0, mainTotal.compareTo(resultTotal));

        // 2. Check restoration: Ensure full hierarchy is restored from DB
        Category restoredL2 = categoryRepository.getCategoryByIdRestored(l2Main.getCategoryId());
        assertNotNull(restoredL2);
        assertEquals(general.getCategoryId(), restoredL2.getParent().getCategoryId()); // Parent check

        // Check children chain
        assertEquals(1, restoredL2.getChildren(false).size());
        Category restoredL3 = restoredL2.getChildren(false).iterator().next();
        assertEquals("L3 Main", restoredL3.getName());

        assertEquals(1, restoredL3.getChildren(false).size());
        Category restoredL4 = restoredL3.getChildren(false).iterator().next();
        assertEquals("L4 Main", restoredL4.getName());

        assertEquals(1, restoredL4.getChildren(false).size());
        Category restoredL5 = restoredL4.getChildren(false).iterator().next();
        assertEquals("L5 Main", restoredL5.getName());
    }

    @Test
    public void testUpdateCategory() {
        Category cat = new Category("Initial Name", "Initial Desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);

        cat.setName("Updated Name");
        cat.setDescription("Updated Desc");
        cat.setMonthlyBudget(BigDecimal.valueOf(200));
        trackingRepository.updateCategory(cat);

        Category updated = categoryRepository.getCategoryById(cat.getCategoryId());
        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(updated.getMonthlyBudget()));
    }

    @Test
    public void testUpdateTransaction() {
        Transaction t = new Transaction.Builder("Old Name", BigDecimal.valueOf(50)).build();
        Long originalId = transactionRepository.insert(t);

        // Create an updated version of the same transaction by reusing the ID
        Transaction updatedT = new Transaction.Builder("New Name", BigDecimal.valueOf(75)).build();
        updatedT.setTransactionId(originalId);
        
        trackingRepository.updateTransaction(updatedT);

        Transaction result = transactionRepository.getById(originalId);
        assertEquals("New Name", result.getName());
        assertEquals(0, BigDecimal.valueOf(75).compareTo(result.getAmount()));
    }

    @Test
    public void testUpdateCategoryTreeWithBudgetChange() {
        Category parent = new Category("Parent", "Parent desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category child = new Category("Child", "Child desc", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        child.setParent(parent);
        categoryRepository.insert(child);

        // Restore to get the full tree in memory
        Category restoredChild = categoryRepository.getCategoryByIdRestored(child.getCategoryId());
        
        // Increase child budget to trigger parent budget increase via determineMinimumBudget()
        // determineMinimumBudget is called inside setMonthlyBudget
        restoredChild.setMonthlyBudget(BigDecimal.valueOf(150));
        
        // Now parent's budget should be 150 in memory
        assertEquals(0, BigDecimal.valueOf(150).compareTo(restoredChild.getParent().getMonthlyBudget()));

        // Update the tree
        trackingRepository.updateCategoryTree(restoredChild);

        // Verify parent in DB is updated
        Category updatedParent = categoryRepository.getCategoryById(parent.getCategoryId());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(updatedParent.getMonthlyBudget()));
    }

    @Test
    public void testGetCategoryByIdRestoredExplicit() {
        Category cat = new Category("RestoredTest", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);
        
        Category restored = trackingRepository.getCategoryByIdRestored(cat.getCategoryId());
        assertNotNull(restored);
        assertEquals(cat.getCategoryId(), restored.getCategoryId());
        // Parent should be General Category by default if none specified
        assertNotNull(restored.getParent());
        assertEquals("General Category", restored.getParent().getName());
    }

    @Test
    public void testInsertIncomeTransactionRedirectsToIncomeCategory() {
        // Create an income transaction
        Transaction incomeTrans = new Transaction.Builder("Salary", BigDecimal.valueOf(3000))
                .type(TrackingType.INCOME)
                .build();
        
        // Try to insert under an expense category
        Category expenseCat = new Category("Books", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);
        
        trackingRepository.insertTransaction(incomeTrans, expenseCat.getCategoryId());
        
        // Verify it was redirected to Income root category
        List<Category> associatedCategories = trackingRepository.findCategoriesByTransactionId(incomeTrans.getTransactionId());
        assertEquals(1, associatedCategories.size());
        assertEquals("Income", associatedCategories.get(0).getName());
        assertEquals(TrackingType.INCOME, associatedCategories.get(0).getType());
    }

    @Test
    public void testInsertAccountTransferTransactionRedirectsToTransferCategory() {
        // Create a transfer transaction
        Transaction transferTrans = new Transaction.Builder("Bank Transfer", BigDecimal.valueOf(500))
                .type(TrackingType.ACCOUNT_TRANSFERS)
                .build();
        
        // Try to insert under an expense category
        Category expenseCat = new Category("Gadgets", "", BigDecimal.valueOf(500), TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);
        
        trackingRepository.insertTransaction(transferTrans, expenseCat.getCategoryId());
        
        // Verify it was redirected to Account Transfer root category
        List<Category> associatedCategories = trackingRepository.findCategoriesByTransactionId(transferTrans.getTransactionId());
        assertEquals(1, associatedCategories.size());
        assertEquals("Account Transfer", associatedCategories.get(0).getName());
        assertEquals(TrackingType.ACCOUNT_TRANSFERS, associatedCategories.get(0).getType());
    }

    @Test
    public void testInsertExpenseTransactionRespectsCategoryId() {
        // Create an expense transaction
        Transaction expenseTrans = new Transaction.Builder("Book Purchase", BigDecimal.valueOf(20))
                .type(TrackingType.EXPENSE)
                .build();
        
        Category expenseCat = new Category("Education", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);
        
        trackingRepository.insertTransaction(expenseTrans, expenseCat.getCategoryId());
        
        // Verify it stayed in the provided category
        List<Category> associatedCategories = trackingRepository.findCategoriesByTransactionId(expenseTrans.getTransactionId());
        assertEquals(1, associatedCategories.size());
        assertEquals("Education", associatedCategories.get(0).getName());
    }

    @Test
    public void testInsertExpenseTransactionUnderWrongCategoryTypeRedirects() {
        // 1. Create an expense transaction
        Transaction expenseTrans = new Transaction.Builder("Lunch", BigDecimal.valueOf(15))
                .type(TrackingType.EXPENSE)
                .build();

        // 2. Attempt to insert under Income category
        Category incomeCat = categoryRepository.getIncomeCategory();
        trackingRepository.insertTransaction(expenseTrans, incomeCat.getCategoryId());

        // Verify it was redirected to General Category (EXPENSE root)
        List<Category> associatedCategories1 = trackingRepository.findCategoriesByTransactionId(expenseTrans.getTransactionId());
        assertEquals(1, associatedCategories1.size());
        assertEquals("General Category", associatedCategories1.get(0).getName());

        // 3. Attempt to insert under Account Transfer category
        Transaction expenseTrans2 = new Transaction.Builder("Dinner", BigDecimal.valueOf(30))
                .type(TrackingType.EXPENSE)
                .build();
        Category transferCat = categoryRepository.getAccountTransferCategory();
        trackingRepository.insertTransaction(expenseTrans2, transferCat.getCategoryId());

        // Verify it was redirected to General Category
        List<Category> associatedCategories2 = trackingRepository.findCategoriesByTransactionId(expenseTrans2.getTransactionId());
        assertEquals(1, associatedCategories2.size());
        assertEquals("General Category", associatedCategories2.get(0).getName());
    }
}
