package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.mtfinance.src.repositories.CategoryRepository;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionRepository;
import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    private AppDatabase database;
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private TrackingRepository trackingRepository;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        categoryRepository = new CategoryRepository(database.categoryDao(), true);
        transactionRepository = new TransactionRepository(database.transactionDao());
        trackingRepository = new TrackingRepository(categoryRepository, transactionRepository, database.categoryTransactionDao());
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
        List<Category> allCategories = categoryRepository.getAllCategories();

        // Assert
        assertEquals(7, allCategories.size());
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
    public void insertTransaction_WithValidCategoryId_PersistsUnderCategory() {
        // Arrange
        Category category = new Category("Dining", "Eating out", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        categoryRepository.insert(category);
        Long catId = category.getCategoryId();
        Transaction transaction = new Transaction.Builder("Pizza", BigDecimal.valueOf(25.0)).build();

        // Act
        trackingRepository.insertTransaction(transaction, catId);
        List<Transaction> allTransactions = transactionRepository.getAll();

        // Assert
        assertEquals(1, allTransactions.size());
        assertEquals("Pizza", allTransactions.get(0).getName());

        List<Category> associatedCategories = trackingRepository.getCategoriesByTransactionId(allTransactions.get(0).getTransactionId());
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
    public void insertTransactionDefault_ValidTransaction_PersistsUnderGeneralCategory() {
        // Arrange
        Transaction transaction = new Transaction.Builder("Generic Expense", BigDecimal.valueOf(10.0)).build();

        // Act
        trackingRepository.insertTransactionDefault(transaction);
        List<Transaction> allTransactions = transactionRepository.getAll();

        // Assert
        assertEquals(1, allTransactions.size());
        Category general = categoryRepository.getGeneralCategory();
        List<Category> associatedCategories = trackingRepository.getCategoriesByTransactionId(allTransactions.get(0).getTransactionId());
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
        List<Category> foundCategories = trackingRepository.getCategoriesByTransactionId(transaction.getTransactionId());

        // Assert
        assertEquals(2, foundCategories.size());
    }

    @Test
    public void getCategoryByIdRestoredExplicit_ExistingCategory_RestoresSuccessfully() {
        // Arrange
        Category cat = new Category("RestoredTest", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        trackingRepository.insertCategory(cat);

        // Act
        Category restored = trackingRepository.getCategoryByIdRestored(cat.getCategoryId());

        // Assert
        assertNotNull(restored);
        assertEquals(cat.getCategoryId(), restored.getCategoryId());
        assertNotNull(restored.getParent());
        assertEquals(GENERAL_CATEGORY_NAME, restored.getParent().getName());
    }

    @Test
    public void insertTransaction_IncomeType_RedirectsToIncomeCategory() {
        // Arrange
        Transaction incomeTrans = new Transaction.Builder("Salary", BigDecimal.valueOf(3000))
                .type(TrackingType.INCOME)
                .build();
        Category expenseCat = new Category("Books", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);

        // Act
        trackingRepository.insertTransaction(incomeTrans, expenseCat.getCategoryId());

        // Assert
        List<Category> associatedCategories = trackingRepository.getCategoriesByTransactionId(incomeTrans.getTransactionId());
        assertEquals(1, associatedCategories.size());
        assertEquals(INCOME_CATEGORY_NAME, associatedCategories.get(0).getName());
        assertEquals(TrackingType.INCOME, associatedCategories.get(0).getType());
    }

    @Test
    public void insertTransaction_TransferType_RedirectsToTransferCategory() {
        // Arrange
        Transaction transferTrans = new Transaction.Builder("Bank Transfer", BigDecimal.valueOf(500))
                .type(TrackingType.ACCOUNT_TRANSFERS)
                .build();
        Category expenseCat = new Category("Gadgets", "", BigDecimal.valueOf(500), TrackingType.EXPENSE);
        categoryRepository.insert(expenseCat);

        // Act
        trackingRepository.insertTransaction(transferTrans, expenseCat.getCategoryId());

        // Assert
        List<Category> associatedCategories = trackingRepository.getCategoriesByTransactionId(transferTrans.getTransactionId());
        assertEquals(1, associatedCategories.size());
        assertEquals(TRANSFER_CATEGORY_NAME, associatedCategories.get(0).getName());
        assertEquals(TrackingType.ACCOUNT_TRANSFERS, associatedCategories.get(0).getType());
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

        // Assert
        List<Category> associatedCategories = trackingRepository.getCategoriesByTransactionId(expenseTrans.getTransactionId());
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

        // Assert
        List<Category> associated = trackingRepository.getCategoriesByTransactionId(trans.getTransactionId());
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

        // Assert
        List<Category> associated = trackingRepository.getCategoriesByTransactionId(trans.getTransactionId());
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
        assertFalse(trackingRepository.transactionExists(transId));
        assertTrue(trackingRepository.getCategoriesByTransactionId(transId).isEmpty());
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

        // Assert
        assertFalse(trackingRepository.categoryExists(catId));
        assertTrue(trackingRepository.transactionExists(trans.getTransactionId()));
        List<Category> associated = trackingRepository.getCategoriesByTransactionId(trans.getTransactionId());
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
        assertFalse(trackingRepository.categoryExists(catId));
        assertFalse(trackingRepository.transactionExists(transId));
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
        BigDecimal total = trackingRepository.getTotalInCategory(category, false, startDate, endDate);

        // Assert (t1 + t4 + t5 = 3 + 6 + 7 = 16)
        assertEquals(0, BigDecimal.valueOf(16.0).compareTo(total));
    }
}
