package com.example.mtfinance.src.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.trackingengine.TransactionViolationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TrackingRepositoryTest {

    private static final String GENERAL_CATEGORY_NAME = "General Category";
    private static final String INCOME_CATEGORY_NAME = "Income";
    private static final String TRANSFER_CATEGORY_NAME = "Account Transfer";
    private static final BigDecimal BUDGET_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal BUDGET_TWO_HUNDRED = BigDecimal.valueOf(200);
    private static final BigDecimal BUDGET_FIVE_HUNDRED = BigDecimal.valueOf(500);
    private static final BigDecimal BUDGET_THOUSAND = BigDecimal.valueOf(1000);

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
        CategoryTreeHelper helper = new CategoryTreeHelper(database.categoryDao());
        CategoryInitializer initializer = new CategoryInitializer(helper, java.util.concurrent.Executors.newSingleThreadExecutor());
        initializer.populateDefaultCategoriesSync();
        categoryService = new CategoryService(database.categoryDao(), helper);
        transactionService = new TransactionService(database.transactionDao());
        trackingService = new TrackingService(database.categoryDao(), database.transactionDao(), database.categoryTransactionDao(), categoryService);

        categoryRepository = new CategoryRepository(database, database.categoryDao(), helper, initializer);
        transactionRepository = new TransactionRepository(database.transactionDao());
        trackingRepository = new TrackingRepository(categoryRepository, transactionRepository, database.categoryTransactionDao(), database.categoryDao(), database.transactionDao(), helper);
    }

    @After
    public void tearDown() {
        database.close();
        TrackingUtlis.resetCategoryCounter();
        TrackingUtlis.resetTransactionCounter();
    }

    @Test
    public void insertCategory_ValidCategory_PersistsSuccessfully() {
        // Arrange
        Category category = new Category("Entertainment", "Movies, games", BUDGET_HUNDRED, TrackingType.EXPENSE);

        // Act
        trackingRepository.insertCategory(category);
        List<Category> allCategories = categoryService.getAllCategories();
        boolean found = allCategories.stream().anyMatch(c -> c.getName().equals("Entertainment"));

        // Assert
        assertEquals(7, allCategories.size());
        assertTrue(found);
    }

    @Test
    public void insertTransaction_WithValidCategoryId_PersistsUnderCategory() {
        // Arrange
        Category category = new Category("Dining", "Eating out", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(category);
        Long catId = category.getCategoryId();
        Transaction transaction = new Transaction.Builder("Pizza", BigDecimal.valueOf(25.0)).build();

        // Act
        trackingRepository.insertTransaction(transaction, catId);
        List<Transaction> allTransactions = transactionService.getAll();
        List<Category> associatedCategories = trackingService.getCategoriesByTransactionId(allTransactions.get(0).getTransactionId());
        boolean found = associatedCategories.stream().anyMatch(c -> c.getCategoryId().equals(catId));

        // Assert
        assertEquals(1, allTransactions.size());
        assertEquals("Pizza", allTransactions.get(0).getName());
        assertTrue(found);
    }

    @Test
    public void insertTransactionDefault_ValidTransaction_PersistsUnderGeneralCategory() {
        // Arrange
        Transaction transaction = new Transaction.Builder("Generic Expense", BigDecimal.valueOf(10.0)).build();

        // Act
        trackingRepository.insertTransactionDefault(transaction);
        List<Transaction> allTransactions = transactionService.getAll();
        Category general = categoryService.getGeneralCategory();
        List<Category> associatedCategories = trackingService.getCategoriesByTransactionId(allTransactions.get(0).getTransactionId());
        boolean found = associatedCategories.stream().anyMatch(c -> c.getCategoryId().equals(general.getCategoryId()));

        // Assert
        assertEquals(1, allTransactions.size());
        assertTrue(found);
    }

    @Test
    public void findCategoriesByTransactionId_TransactionWithMultipleCategories_ReturnsAllCategories() {
        // Arrange
        Category cat1 = new Category("Cat1", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category cat2 = new Category("Cat2", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat1);
        categoryRepository.insert(cat2);

        Transaction transaction = new Transaction.Builder("Shared Transaction", BigDecimal.valueOf(50.0)).build();
        trackingRepository.insertTransaction(transaction, cat1.getCategoryId());

        // Act
        CategoryTransactionCrossRef crossRef = new CategoryTransactionCrossRef(cat2.getCategoryId(), transaction.getTransactionId());
        database.categoryTransactionDao().insertCrossRef(crossRef);
        List<Category> foundCategories = trackingService.getCategoriesByTransactionId(transaction.getTransactionId());
        boolean found1 = foundCategories.stream().anyMatch(c -> c.getName().equals("Cat1"));
        boolean found2 = foundCategories.stream().anyMatch(c -> c.getName().equals("Cat2"));

        // Assert
        assertEquals(2, foundCategories.size());
        assertTrue(found1);
        assertTrue(found2);
    }

    @Test
    public void getTotalInCategory_SimpleCategory_CalculatesCorrectSum() {
        // Arrange
        Category category = new Category("PaknSave", "Food stuff", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(category);

        Transaction t1 = new Transaction.Builder("Apple", BigDecimal.valueOf(2.50)).build();
        Transaction t2 = new Transaction.Builder("Milk", BigDecimal.valueOf(3.50)).build();
        trackingRepository.insertTransaction(t1, category.getCategoryId());
        trackingRepository.insertTransaction(t2, category.getCategoryId());

        // Act
        BigDecimal total = trackingService.getTotalInCategory(category, false);

        // Assert
        assertEquals(0, BigDecimal.valueOf(6.00).compareTo(total));
    }

    @Test
    public void getTotalInCategory_WithSubcategories_CalculatesSumCorrectly() {
        // Arrange
        Category parent = new Category("Transport", "Commute", BigDecimal.valueOf(300), TrackingType.EXPENSE);
        Category child = new Category("Fuel", "Gasoline", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        child.setParent(parent);
        categoryRepository.insert(child);

        Transaction t1 = new Transaction.Builder("Train ticket", BigDecimal.valueOf(15.00)).build();
        Transaction t2 = new Transaction.Builder("Petrol", BigDecimal.valueOf(45.00)).build();
        trackingRepository.insertTransaction(t1, parent.getCategoryId());
        trackingRepository.insertTransaction(t2, child.getCategoryId());

        // Act
        BigDecimal totalWithSubs = trackingService.getTotalInCategory(parent, true);
        BigDecimal totalWithoutSubs = trackingService.getTotalInCategory(parent, false);

        // Assert
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(totalWithSubs));
        assertEquals(0, BigDecimal.valueOf(15.00).compareTo(totalWithoutSubs));
    }

    @Test
    public void getCategoryWithTransactionsByCategoryId_ExistingId_ReturnsCategoryAndTransactions() {
        // Arrange
        Category category = new Category("TestCat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(category);
        Transaction t = new Transaction.Builder("T1", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(t, category.getCategoryId());

        // Act
        CategoryWithTransactions result = trackingService.getCategoryWithTransactionsByCategoryId(category.getCategoryId());

        // Assert
        assertNotNull(result);
        assertEquals(category.getCategoryId(), result.category.getCategoryId());
        assertEquals(1, result.transactions.size());
    }

    @Test
    public void getCategoryWithTransactionsByParentId_ExistingParentId_ReturnsChildCategories() {
        // Arrange
        Category parent = new Category("Parent", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        Category child = new Category("Child", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        child.setParent(parent);
        categoryRepository.insert(child);

        // Act
        List<CategoryWithTransactions> results = trackingService.getCategoryWithTransactionsByParentId(parent.getCategoryId());

        // Assert
        assertEquals(1, results.size());
        assertEquals(child.getCategoryId(), results.get(0).category.getCategoryId());
    }

    @Test
    public void getTransactionIdsForCategory_ExistingCategory_ReturnsAllAssociatedIds() {
        // Arrange
        Category category = new Category("TestCat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(category);
        Transaction t1 = new Transaction.Builder("T1", BigDecimal.valueOf(10)).build();
        Transaction t2 = new Transaction.Builder("T2", BigDecimal.valueOf(20)).build();
        trackingRepository.insertTransaction(t1, category.getCategoryId());
        trackingRepository.insertTransaction(t2, category.getCategoryId());

        // Act
        List<Long> ids = trackingService.getTransactionIdsForCategory(category.getCategoryId());

        // Assert
        assertEquals(2, ids.size());
        assertTrue(ids.contains(t1.getTransactionId()));
        assertTrue(ids.contains(t2.getTransactionId()));
    }

    @Test
    public void getAllCategoriesWithTransactions_ValidDatabase_ReturnsAllCategoriesWithTransactions() throws InterruptedException {
        // Arrange
        Category cat = new Category("Cat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);

        // Act
        List<CategoryWithTransactions> result = getValue(trackingService.getAllCategoriesWithTransactions());

        // Assert
        assertNotNull(result);
        assertEquals(7, result.size());
    }

    @Test
    public void getCategoriesWithTransactionsByIds_ValidIds_ReturnsMatchingCategories() {
        // Arrange
        Category cat1 = new Category("Cat1", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category cat2 = new Category("Cat2", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat1);
        categoryRepository.insert(cat2);

        // Act
        List<Long> ids = Arrays.asList(cat1.getCategoryId(), cat2.getCategoryId());
        List<CategoryWithTransactions> result = trackingService.getCategoriesWithTransactionsByIds(ids);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void getTransactionsForCategory_WithAndWithoutSubcategories_ReturnsCorrectTransactions() {
        // Arrange
        Category parent = new Category("Parent", "", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("Child", "", BigDecimal.valueOf(200), TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        child.setParent(parent);
        categoryRepository.insert(child);

        Transaction t1 = new Transaction.Builder("T1", BigDecimal.valueOf(10)).build();
        Transaction t2 = new Transaction.Builder("T2", BigDecimal.valueOf(20)).build();
        trackingRepository.insertTransaction(t1, parent.getCategoryId());
        trackingRepository.insertTransaction(t2, child.getCategoryId());

        // Act
        Set<Transaction> parentOnly = trackingService.getTransactionsForCategory(parent, false);
        Set<Transaction> parentAndChild = trackingService.getTransactionsForCategory(parent, true);

        // Assert
        assertEquals(1, parentOnly.size());
        assertTrue(parentOnly.contains(t1));
        assertEquals(2, parentAndChild.size());
        assertTrue(parentAndChild.contains(t1));
        assertTrue(parentAndChild.contains(t2));
    }

    @Test
    public void getChildrenTotals_ValidParentAndChildren_CalculatesExclusiveChildTotals() {
        // Arrange
        Category parent = new Category("Home", "", BUDGET_THOUSAND, TrackingType.EXPENSE);
        Category child1 = new Category("Rent", "", BigDecimal.valueOf(600), TrackingType.EXPENSE);
        Category child2 = new Category("Food", "", BigDecimal.valueOf(300), TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        child1.setParent(parent);
        child2.setParent(parent);
        categoryRepository.insert(child1);
        categoryRepository.insert(child2);

        Transaction t1 = new Transaction.Builder("Rent Payment", BigDecimal.valueOf(600)).build();
        Transaction t2 = new Transaction.Builder("Groceries", BigDecimal.valueOf(50)).build();
        Transaction tShared = new Transaction.Builder("Shared", BigDecimal.valueOf(10)).build();

        trackingRepository.insertTransaction(t1, child1.getCategoryId());
        trackingRepository.insertTransaction(t2, child2.getCategoryId());
        trackingRepository.insertTransaction(tShared, child1.getCategoryId());
        trackingRepository.insertRelationship(tShared.getTransactionId(), child2.getCategoryId());

        CategoryWithTransactions parentCWT = trackingService.getCategoryWithTransactionsByCategoryId(parent.getCategoryId());
        parentCWT.category = categoryService.getCategoryByIdRestored(parent.getCategoryId());

        // Act
        Map<CategoryWithTransactions, BigDecimal> totals = trackingService.getChildrenTotals(parentCWT, LocalDate.MIN, LocalDate.MAX);

        // Assert
        BigDecimal rentTotal = totals.entrySet().stream()
                .filter(e -> e.getKey().category.getName().equals("Rent"))
                .map(Map.Entry::getValue).findFirst().orElse(BigDecimal.ZERO);
        BigDecimal foodTotal = totals.entrySet().stream()
                .filter(e -> e.getKey().category.getName().equals("Food"))
                .map(Map.Entry::getValue).findFirst().orElse(BigDecimal.ZERO);

        assertEquals(2, totals.size());
        assertEquals(0, BigDecimal.valueOf(610).compareTo(rentTotal));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(foodTotal));
    }

    @Test
    public void branchIsolationAndRestoration_DeepHierarchy_MaintainsIsolationAndRestoresSuccessfully() {
        // Arrange
        Category general = categoryService.getGeneralCategory();

        Category l2Main = new Category("L2 Main", "Main branch level 2", BUDGET_THOUSAND, TrackingType.EXPENSE);
        l2Main.setParent(general);
        categoryRepository.insert(l2Main);

        Category l3Main = new Category("L3 Main", "Main branch level 3", BigDecimal.valueOf(800), TrackingType.EXPENSE);
        l3Main.setParent(l2Main);
        categoryRepository.insert(l3Main);

        Category l4Main = new Category("L4 Main", "Main branch level 4", BigDecimal.valueOf(600), TrackingType.EXPENSE);
        l4Main.setParent(l3Main);
        categoryRepository.insert(l4Main);

        Category l5Main = new Category("L5 Main", "Main branch level 5", BigDecimal.valueOf(400), TrackingType.EXPENSE);
        l5Main.setParent(l4Main);
        categoryRepository.insert(l5Main);

        Category l2Other = new Category("L2 Other", "Other branch level 2", BUDGET_THOUSAND, TrackingType.EXPENSE);
        l2Other.setParent(general);
        categoryRepository.insert(l2Other);

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

        Transaction tOther = new Transaction.Builder("T Other", BUDGET_THOUSAND).build();
        trackingRepository.insertTransaction(tOther, l2Other.getCategoryId());

        Transaction tRoot = new Transaction.Builder("T Root", BigDecimal.valueOf(500)).build();
        trackingRepository.insertTransaction(tRoot, general.getCategoryId());

        // Act
        BigDecimal resultTotal = trackingService.getTotalInCategory(l2Main, true);
        Category restoredL2 = categoryService.getCategoryByIdRestored(l2Main.getCategoryId());

        // Assert
        assertEquals(0, mainTotal.compareTo(resultTotal));
        assertNotNull(restoredL2);
        assertEquals(general.getCategoryId(), restoredL2.getParent().getCategoryId());
        assertEquals(1, restoredL2.getChildren(false).size());
    }

    @Test
    public void updateCategory_ExistingCategory_UpdatesFieldsSuccessfully() {
        // Arrange
        Category cat = new Category("Initial Name", "Initial Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);

        // Act
        cat.setName("Updated Name");
        cat.setDescription("Updated Desc");
        cat.setMonthlyBudget(BigDecimal.valueOf(200));
        trackingRepository.updateCategory(cat);

        // Assert
        Category updated = categoryService.getCategoryById(cat.getCategoryId());
        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(updated.getMonthlyBudget()));
    }

    @Test
    public void updateTransaction_ExistingTransaction_UpdatesFieldsSuccessfully() {
        // Arrange
        Transaction t = new Transaction.Builder("Old Name", BigDecimal.valueOf(50)).build();
        Long originalId = transactionRepository.insert(t);

        // Act
        Transaction updatedT = new Transaction.Builder("New Name", BigDecimal.valueOf(75)).build();
        updatedT.setTransactionId(originalId);
        trackingRepository.updateTransaction(updatedT);

        // Assert
        Transaction result = transactionService.getById(originalId);
        assertEquals("New Name", result.getName());
        assertEquals(0, BigDecimal.valueOf(75).compareTo(result.getAmount()));
    }

    @Test
    public void updateCategoryTree_ChildBudgetIncrease_PropagatesToParentBudget() {
        // Arrange
        Category parent = new Category("Parent", "Parent desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("Child", "Child desc", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        categoryRepository.insert(parent);
        child.setParent(parent);
        categoryRepository.insert(child);

        Category restoredChild = categoryService.getCategoryByIdRestored(child.getCategoryId());

        // Act
        restoredChild.setMonthlyBudget(BigDecimal.valueOf(150));
        trackingRepository.updateCategoryTree(restoredChild);
        Category updatedParent = categoryService.getCategoryById(parent.getCategoryId());

        // Assert
        assertEquals(0, BigDecimal.valueOf(150).compareTo(restoredChild.getParent().getMonthlyBudget()));
        assertEquals(0, BigDecimal.valueOf(150).compareTo(updatedParent.getMonthlyBudget()));
    }

    @Test
    public void getCategoryByIdRestoredExplicit_ExistingCategory_RestoresSuccessfully() {
        // Arrange
        Category cat = new Category("RestoredTest", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);

        // Act
        Category restored = categoryService.getCategoryByIdRestored(cat.getCategoryId());

        // Assert
        assertNotNull(restored);
        assertEquals(cat.getCategoryId(), restored.getCategoryId());
        assertNotNull(restored.getParent());
        assertEquals(GENERAL_CATEGORY_NAME, restored.getParent().getName());
    }

    @Test(expected = TransactionViolationException.class)
    public void insertTransaction_IncomeTypeWithExpenseCategory_ThrowsTransactionViolationException() {
        // Arrange
        Transaction incomeTrans = new Transaction.Builder("Salary", BigDecimal.valueOf(3000))
                .type(TrackingType.INCOME)
                .build();
        Category expenseCat = new Category("Books", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);

        // Act
        trackingRepository.insertTransaction(incomeTrans, expenseCat.getCategoryId());
    }

    @Test(expected = TransactionViolationException.class)
    public void insertTransaction_TransferTypeWithExpenseCategory_ThrowsTransactionViolationException() {
        // Arrange
        Transaction transferTrans = new Transaction.Builder("Bank Transfer", BigDecimal.valueOf(500))
                .type(TrackingType.ACCOUNT_TRANSFERS)
                .build();
        Category expenseCat = new Category("Gadgets", "", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);

        // Act
        trackingRepository.insertTransaction(transferTrans, expenseCat.getCategoryId());
    }

    @Test
    public void insertTransaction_ExpenseType_RespectsProvidedCategoryId() {
        // Arrange
        Transaction expenseTrans = new Transaction.Builder("Book Purchase", BigDecimal.valueOf(20))
                .type(TrackingType.EXPENSE)
                .build();
        Category expenseCat = new Category("Education", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);

        // Act
        trackingRepository.insertTransaction(expenseTrans, expenseCat.getCategoryId());
        List<Category> associatedCategories = trackingService.getCategoriesByTransactionId(expenseTrans.getTransactionId());

        // Assert
        assertEquals(1, associatedCategories.size());
        assertEquals("Education", associatedCategories.get(0).getName());
    }

    @Test
    public void insertRelationship_ValidTransAndCat_CreatesAssociation() {
        // Arrange
        Category cat = new Category("Cat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        Transaction trans = new Transaction.Builder("Trans", BigDecimal.valueOf(10)).build();
        transactionRepository.insert(trans);

        // Act
        trackingRepository.insertRelationship(trans.getTransactionId(), cat.getCategoryId());
        List<Category> associated = trackingService.getCategoriesByTransactionId(trans.getTransactionId());

        // Assert
        assertTrue(associated.stream().anyMatch(c -> c.getCategoryId().equals(cat.getCategoryId())));
    }

    @Test
    public void deleteRelationship_ExistingAssociation_RemovesAssociation() {
        // Arrange
        Category cat1 = new Category("Cat1", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category cat2 = new Category("Cat2", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat1);
        categoryRepository.insert(cat2);

        Transaction trans = new Transaction.Builder("Trans", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(trans, cat1.getCategoryId());
        trackingRepository.insertRelationship(trans.getTransactionId(), cat2.getCategoryId());

        // Act
        trackingRepository.deleteRelationship(trans.getTransactionId(), cat2.getCategoryId());
        List<Category> associated = trackingService.getCategoriesByTransactionId(trans.getTransactionId());

        // Assert
        assertEquals(1, associated.size());
        assertEquals(cat1.getCategoryId(), associated.get(0).getCategoryId());
    }

    @Test(expected = IllegalStateException.class)
    public void deleteRelationship_OnlyOneAssociationRemaining_ThrowsException() {
        // Arrange
        Category cat = new Category("Cat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        Transaction trans = new Transaction.Builder("Trans", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(trans, cat.getCategoryId());

        // Act & Assert
        trackingRepository.deleteRelationship(trans.getTransactionId(), cat.getCategoryId());
    }

    @Test
    public void deleteTransaction_ExistingTransaction_DeletesTransactionAndAssociations() {
        // Arrange
        Category cat = new Category("Cat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        Transaction trans = new Transaction.Builder("Trans", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(trans, cat.getCategoryId());
        Long transId = trans.getTransactionId();

        // Act
        trackingRepository.deleteTransaction(transId);

        // Assert
        assertFalse(transactionService.transactionExists(transId));
        assertTrue(trackingService.getCategoriesByTransactionId(transId).isEmpty());
    }

    @Test
    public void deleteCategory_KeepTransactions_MovesTransactionsToRoot() {
        // Arrange
        Category cat = new Category("Cat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        Transaction trans = new Transaction.Builder("Trans", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(trans, cat.getCategoryId());
        Long catId = cat.getCategoryId();

        // Act
        trackingRepository.deleteCategory(catId, false);
        List<Category> associated = trackingService.getCategoriesByTransactionId(trans.getTransactionId());

        // Assert
        assertFalse(categoryService.categoryExists(catId));
        assertTrue(transactionService.transactionExists(trans.getTransactionId()));
        assertEquals(1, associated.size());
        assertEquals(GENERAL_CATEGORY_NAME, associated.get(0).getName());
    }

    @Test
    public void deleteCategory_DeleteTransactions_RemovesCategoryAndTransactions() {
        // Arrange
        Category cat = new Category("Cat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        Transaction trans = new Transaction.Builder("Trans", BigDecimal.valueOf(10)).build();
        trackingRepository.insertTransaction(trans, cat.getCategoryId());
        Long catId = cat.getCategoryId();
        Long transId = trans.getTransactionId();

        // Act
        trackingRepository.deleteCategory(catId, true);

        // Assert
        assertFalse(categoryService.categoryExists(catId));
        assertFalse(transactionService.transactionExists(transId));
    }

    @Test
    public void getTotalInCategory_WithDateRange_CalculatesSumCorrectly() {
        // Arrange
        Category category = new Category("GroceriesRangeTest", "", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(category);

        LocalDate startDate = LocalDate.of(2023, 1, 10);
        LocalDate endDate = LocalDate.of(2023, 1, 20);

        Transaction t1 = new Transaction.Builder("Bread", BigDecimal.valueOf(3.0)).date(LocalDate.of(2023, 1, 15)).build();
        Transaction t2 = new Transaction.Builder("Milk", BigDecimal.valueOf(4.0)).date(LocalDate.of(2023, 1, 5)).build();
        Transaction t3 = new Transaction.Builder("Eggs", BigDecimal.valueOf(5.0)).date(LocalDate.of(2023, 1, 25)).build();
        Transaction t4 = new Transaction.Builder("Butter", BigDecimal.valueOf(6.0)).date(startDate).build();
        Transaction t5 = new Transaction.Builder("Cheese", BigDecimal.valueOf(7.0)).date(endDate).build();

        trackingRepository.insertTransaction(t1, category.getCategoryId());
        trackingRepository.insertTransaction(t2, category.getCategoryId());
        trackingRepository.insertTransaction(t3, category.getCategoryId());
        trackingRepository.insertTransaction(t4, category.getCategoryId());
        trackingRepository.insertTransaction(t5, category.getCategoryId());

        // Act
        BigDecimal total = trackingService.getTotalInCategory(category, false, startDate, endDate);

        // Assert (t1 + t4 + t5 = 3 + 6 + 7 = 16)
        assertEquals(0, BigDecimal.valueOf(16.0).compareTo(total));
    }

    @Test(expected = TransactionViolationException.class)
    public void deleteTransaction_NonExistentTransaction_ThrowsTransactionViolationException() {
        trackingRepository.deleteTransaction(99999L);
    }

    @Test(expected = TransactionViolationException.class)
    public void deleteRelationship_NonExistentTransaction_ThrowsTransactionViolationException() {
        Category cat = new Category("NonExitingRelCat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        trackingRepository.deleteRelationship(99999L, cat.getCategoryId());
    }

    @Test(expected = CategoryViolationException.class)
    public void deleteRelationship_NonExistentCategory_ThrowsCategoryViolationException() {
        Category cat = new Category("ValidRelCat", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(cat);
        Transaction trans = new Transaction.Builder("ValidTrans", BigDecimal.TEN).build();
        trackingRepository.insertTransaction(trans, cat.getCategoryId());
        trackingRepository.deleteRelationship(trans.getTransactionId(), 99999L);
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
}
