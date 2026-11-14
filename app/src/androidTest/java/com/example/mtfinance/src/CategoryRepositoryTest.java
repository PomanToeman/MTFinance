package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.mtfinance.src.repositories.CategoryRepository;
import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CategoryRepositoryTest {

    private static final String GENERAL_CATEGORY_NAME = "General Category";
    private static final String GROCERIES_NAME = "Groceries";
    private static final String ENTERTAINMENT_NAME = "Entertainment";
    private static final String PARENT_NAME = "Parent";
    private static final String CHILD_NAME = "Child";
    private static final BigDecimal BUDGET_FIFTY = BigDecimal.valueOf(50);
    private static final BigDecimal BUDGET_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal BUDGET_TWO_HUNDRED = BigDecimal.valueOf(200);
    private static final BigDecimal BUDGET_FIVE_HUNDRED = BigDecimal.valueOf(500);

    private AppDatabase database;
    private CategoryDao categoryDao;
    private CategoryRepository repository;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        categoryDao = database.categoryDao();
        repository = new CategoryRepository(categoryDao, true);
    }

    @After
    public void tearDown() {
        database.close();
        TrackingUtlis.resetCategoryCounter();
        TrackingUtlis.resetTransactionCounter();
    }

    @Test
    public void constructor_WithDefaultPopulation_PopulatesDefaultCategories() {
        // Arrange / Act
        List<Category> allCategories = repository.getAllCategories();
        Category generalCategory = repository.getGeneralCategory();

        // Assert
        assertEquals(6, allCategories.size());
        assertNotNull(generalCategory);
        assertEquals(GENERAL_CATEGORY_NAME, generalCategory.getName());
    }

    @Test
    public void insert_NewValidCategory_PersistsCategorySuccessfully() {
        // Arrange
        Category newCategory = new Category(ENTERTAINMENT_NAME, "Movies etc", BUDGET_FIFTY, TrackingType.EXPENSE);

        // Act
        long id = repository.insert(newCategory);
        List<Category> allCategories = repository.getAllCategories();

        // Assert
        assertTrue(id > 0);
        assertEquals(7, allCategories.size());
    }

    @Test
    public void insert_DuplicateCategoryName_ThrowsConstraintException() {
        // Arrange
        Category duplicate = new Category(GROCERIES_NAME, "Duplicate", BUDGET_HUNDRED, TrackingType.EXPENSE);

        // Act & Assert
        assertThrows(android.database.sqlite.SQLiteConstraintException.class, () -> {
            repository.insert(duplicate);
        });
    }

    @Test
    public void insert_CaseInsensitiveDuplicateCategoryName_ThrowsConstraintException() {
        // Arrange
        Category duplicate = new Category(GROCERIES_NAME.toLowerCase(), "Duplicate", BUDGET_HUNDRED, TrackingType.EXPENSE);

        // Act & Assert
        assertThrows(android.database.sqlite.SQLiteConstraintException.class, () -> {
            repository.insert(duplicate);
        });
    }

    @Test
    public void getById_ExistingCategoryId_ReturnsCorrectCategory() {
        // Arrange
        Category category = new Category("TestCat", "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        long id = repository.insert(category);

        // Act
        Category retrieved = categoryDao.getById(id);

        // Assert
        assertNotNull(retrieved);
        assertEquals("TestCat", retrieved.getName());
    }

    @Test
    public void insert_ChildWithoutExistingParent_AutomaticallyInsertsParent() {
        // Arrange
        Category parent = new Category(PARENT_NAME, "Not in DB", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category(CHILD_NAME, "Also not in DB", BUDGET_FIFTY, TrackingType.EXPENSE);
        child.setParent(parent);

        // Act
        repository.insert(child);

        // Assert
        List<Category> allCategories = repository.getAllCategories();
        boolean foundParent = false;
        boolean foundChild = false;
        for (Category c : allCategories) {
            if (c.getName().equals(PARENT_NAME)) foundParent = true;
            if (c.getName().equals(CHILD_NAME)) foundChild = true;
        }
        assertTrue(foundParent);
        assertTrue(foundChild);
    }

    @Test
    public void insert_ChildWithGrandparentHierarchy_AutomaticallyInsertsEntireChain() {
        // Arrange
        Category grandParent = new Category("Grandparent", "Top", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        Category parent = new Category("ParentName", "Middle", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("ChildName", "Bottom", BUDGET_FIFTY, TrackingType.EXPENSE);

        parent.setParent(grandParent);
        child.setParent(parent);

        // Act
        repository.insert(child);

        // Assert
        List<Category> allCategories = repository.getAllCategories();
        boolean foundGP = false;
        boolean foundP = false;
        for (Category c : allCategories) {
            if (c.getName().equals("Grandparent")) foundGP = true;
            if (c.getName().equals("ParentName")) foundP = true;
        }
        assertTrue(foundGP);
        assertTrue(foundP);
    }

    @Test
    public void deleteCategory_ExistingCategory_RemovesFromDatabase() {
        // Arrange
        Category toDelete = new Category("ToDelete", "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        repository.insert(toDelete);

        List<Category> allBefore = repository.getAllCategories();
        Category found = null;
        for (Category c : allBefore) {
            if (c.getName().equals("ToDelete")) found = c;
        }
        assertNotNull(found);

        // Act
        repository.deleteCategory(found);

        // Assert
        List<Category> allAfter = repository.getAllCategories();
        boolean stillExists = false;
        for (Category c : allAfter) {
            if (c.getName().equals("ToDelete")) {
                stillExists = true;
                break;
            }
        }
        assertFalse(stillExists);
    }

    @Test
    public void deleteCategory_ParentWithChildren_ReassignsChildrenToGeneralCategory() {
        // Arrange
        Category parent = new Category("ParentToDelete", "Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("ChildToReassign", "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        child.setParent(parent);
        repository.insert(child); // Inserts both

        assertEquals(8, repository.getAllCategories().size());

        // Act
        repository.deleteCategory(parent);

        // Assert
        Category childInDb = null;
        for (Category c : repository.getAllCategories()) {
            if (c.getName().equals("ChildToReassign")) childInDb = c;
        }

        assertNotNull(childInDb);
        assertEquals(repository.getGeneralCategory().getCategoryId().longValue(), child.getParentId().longValue());
    }

    @Test
    public void getCategoryByIdRestored_HierarchicalCategory_RestoresParentAndGrandparentSuccessfully() {
        // Arrange
        Category root = repository.getGeneralCategory();
        Category parent = new Category("Parent", "Parent desc", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category("Child", "Child desc", BUDGET_FIFTY, TrackingType.EXPENSE);

        parent.setParent(root);
        child.setParent(parent);
        repository.insert(child);

        // Act
        Category restoredChild = repository.getCategoryByIdRestored(child.getCategoryId());

        // Assert
        assertNotNull(restoredChild);
        assertNotNull(restoredChild.getParent());
        assertEquals("Parent", restoredChild.getParent().getName());
        assertNotNull(restoredChild.getParent().getParent());
        assertEquals(root.getName(), restoredChild.getParent().getParent().getName());
    }

    @Test
    public void defaultCategories_AllCategories_HaveCorrectTrackingTypes() {
        // Arrange / Act
        List<Category> allCategories = repository.getAllCategories();
        Category general = null;
        Category income = null;
        Category transfer = null;

        for (Category c : allCategories) {
            if (c.getName().equals(GENERAL_CATEGORY_NAME)) general = c;
            if (c.getName().equals("Income")) income = c;
            if (c.getName().equals("Account Transfer")) transfer = c;
        }

        // Assert
        assertNotNull(general);
        assertEquals(TrackingType.EXPENSE, general.getType());

        assertNotNull(income);
        assertEquals(TrackingType.INCOME, income.getType());

        assertNotNull(transfer);
        assertEquals(TrackingType.ACCOUNT_TRANSFERS, transfer.getType());
    }

    @Test
    public void insert_NonExpenseCategory_FailsInsertion() {
        // Arrange
        Category incomeSub = new Category("Salary", "", BigDecimal.valueOf(1000), TrackingType.INCOME);
        Category transferSub = new Category("Bank Transfer", "", BUDGET_HUNDRED, TrackingType.ACCOUNT_TRANSFERS);

        // Act
        long id1 = repository.insert(incomeSub);
        long id2 = repository.insert(transferSub);

        // Assert
        assertEquals(-1L, id1);
        assertEquals(-1L, id2);
    }

    @Test
    public void autoSearchCategoryIds_VariousCriteria_ReturnsCorrectMatchesOrdered() {
        // Arrange
        Category entertainment = new Category(ENTERTAINMENT_NAME, "Fun activities and movies", BUDGET_HUNDRED, TrackingType.EXPENSE);
        Category food = new Category("Fast Food", "Burgers and fries", BUDGET_HUNDRED, TrackingType.EXPENSE);
        repository.insert(entertainment);
        repository.insert(food);

        // Act & Assert
        // 1. Exact Match
        List<Long> exactMatch = repository.autoSearchCategoryIds(ENTERTAINMENT_NAME, TrackingType.EXPENSE);
        assertFalse(exactMatch.isEmpty());
        assertEquals(ENTERTAINMENT_NAME, repository.getCategoryById(exactMatch.get(0)).getName());

        // 2. Partial Prefix Match
        List<Long> prefixMatch = repository.autoSearchCategoryIds("Entertain", TrackingType.EXPENSE);
        assertFalse(prefixMatch.isEmpty());
        assertEquals(ENTERTAINMENT_NAME, repository.getCategoryById(prefixMatch.get(0)).getName());

        // 3. Description Match
        List<Long> descriptionMatch = repository.autoSearchCategoryIds("movies", TrackingType.EXPENSE);
        assertFalse(descriptionMatch.isEmpty());
        assertEquals(ENTERTAINMENT_NAME, repository.getCategoryById(descriptionMatch.get(0)).getName());

        // 4. Type Filtering
        List<Long> wrongType = repository.autoSearchCategoryIds(ENTERTAINMENT_NAME, TrackingType.INCOME);
        assertTrue(wrongType.isEmpty());
    }
}
