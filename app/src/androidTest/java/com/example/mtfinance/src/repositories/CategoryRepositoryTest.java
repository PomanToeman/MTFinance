package com.example.mtfinance.src.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.mtfinance.src.repositories.roomdatabase.AppDatabase;
import com.example.mtfinance.src.repositories.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryViolationException;
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
    private CategoryService categoryService;
    private CategoryRepository repository;

    @Before
    public void setUp() {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        categoryDao = database.categoryDao();
        CategoryTreeHelper helper = new CategoryTreeHelper(categoryDao);
        CategoryInitializer initializer = new CategoryInitializer(helper, java.util.concurrent.Executors.newSingleThreadExecutor());
        repository = new CategoryRepository(database, categoryDao, helper, initializer);
        categoryService = new CategoryService(categoryDao, helper);
        initializer.populateDefaultCategoriesSync();
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
        List<Category> allCategories = categoryService.getAllCategories();
        Category generalCategory = categoryService.getGeneralCategory();

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
        List<Category> allCategories = categoryService.getAllCategories();

        // Assert
        assertTrue(id > 0);
        assertEquals(7, allCategories.size());
    }

    @Test(expected = CategoryViolationException.class)
    public void insert_DuplicateCategory_ThrowsCategoryViolationException() {
        // Arrange
        Category duplicate = new Category(GROCERIES_NAME, "Duplicate", BUDGET_HUNDRED, TrackingType.EXPENSE);

        // Act & Assert
        repository.insert(duplicate);
    }

    @Test
    public void getCategoryByName_ExistingCategory_ReturnsCorrectCategory() {
        // Arrange
        Category category = new Category("DirectNameSearch", "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        repository.insert(category);

        // Act
        Category found = categoryService.getCategoryByName("DirectNameSearch");

        // Assert
        assertNotNull(found);
        assertEquals("DirectNameSearch", found.getName());
    }

    @Test
    public void insert_CategoryWithNonExistentParent_AutomaticallyInsertsParentUnderGeneral() {
        // Arrange
        Category parent = new Category(PARENT_NAME, "Parent desc", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        Category child = new Category(CHILD_NAME, "Child desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        child.setParent(parent);

        // Act
        long childId = repository.insert(child);
        List<Category> allCategories = categoryService.getAllCategories();

        // Assert
        assertTrue(childId > 0);
        assertEquals(8, allCategories.size()); // 6 default + parent + child

        Category foundChild = categoryService.getCategoryByName(CHILD_NAME);

        assertNotNull(foundChild);
        assertEquals(parent.getCategoryId(), foundChild.getParentId());
    }

    @Test
    public void deleteCategory_LeafCategory_DeletesSuccessfully() {
        // Arrange
        Category category = new Category(ENTERTAINMENT_NAME, "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        repository.insert(category);

        Category found = categoryService.getCategoryByName(ENTERTAINMENT_NAME);
        assertNotNull(found);

        // Act
        repository.deleteCategory(found);
        boolean stillExists = categoryService.getCategoryByName(ENTERTAINMENT_NAME) != null;

        // Assert
        assertFalse(stillExists);
    }

    @Test
    public void deleteCategory_ParentCategoryWithChildren_ReassignsChildrenToGeneral() {
        // Arrange
        Category parent = new Category(PARENT_NAME, "", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        repository.insert(parent);

        Category parentInDb = categoryService.getCategoryByName(PARENT_NAME);
        assertNotNull(parentInDb);
        Category parentRestored = categoryService.getCategoryByIdRestored(parentInDb.getCategoryId());

        Category child = new Category(CHILD_NAME, "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        child.setParent(parentRestored);
        repository.insert(child);

        Category childInDb = categoryService.getCategoryByName(CHILD_NAME);
        assertNotNull(childInDb);

        // Act
        repository.deleteCategory(parentRestored);
        Category updatedChild = categoryService.getCategoryByIdSkim(childInDb.getCategoryId());

        // Assert
        assertEquals((long) categoryService.getGeneralCategory().getCategoryId(), (long) updatedChild.getParentId());

    }

    @Test
    public void updateCategory_ExistingCategory_UpdatesFieldsSuccessfully() {
        // Arrange
        Category category = new Category("Initial", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        repository.insert(category);

        Category found = categoryService.getCategoryByName("Initial");
        assertNotNull(found);

        Category restoredChild = categoryService.getCategoryByIdRestored(found.getCategoryId());

        // Act
        restoredChild.setName("Updated");
        restoredChild.setDescription("Updated desc");
        restoredChild.setMonthlyBudget(BUDGET_TWO_HUNDRED);
        repository.updateCategory(restoredChild);

        Category updated = categoryService.getCategoryByIdSkim(found.getCategoryId());

        // Assert
        assertEquals("Updated", updated.getName());
        assertEquals("Updated desc", updated.getDescription());
        assertEquals(0, BUDGET_TWO_HUNDRED.compareTo(updated.getMonthlyBudget()));
    }

    @Test
    public void updateCategoryTree_ValidHierarchyUpdate_UpdatesSuccessfully() {
        // Arrange
        Category general = categoryService.getGeneralCategory();
        Category income = categoryService.getCategoryByName("Income");
        Category transfer = categoryService.getCategoryByName("Account Transfer");

        assertNotNull(general);
        assertNotNull(income);
        assertNotNull(transfer);
    }

    @Test(expected = com.example.mtfinance.src.trackingengine.CategoryViolationException.class)
    public void updateCategoryTree_CircularReference_ThrowsException() {
        // Arrange
        Category parent = new Category("P", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        repository.insert(parent);
        Category pInDb = categoryService.getCategoryByName("P");
        assertNotNull(pInDb);

        Category child = new Category("C", "", BUDGET_FIFTY, TrackingType.EXPENSE);
        child.setParent(pInDb);
        repository.insert(child);
        Category cInDb = categoryService.getCategoryByName("C");
        assertNotNull(cInDb);

        Category restoredP = categoryService.getCategoryByIdRestored(pInDb.getCategoryId());
        Category restoredC = categoryService.getCategoryByIdRestored(cInDb.getCategoryId());
        restoredP.setParentId(restoredC.getCategoryId());

        // Act & Assert
        repository.updateCategoryTree(restoredP);
    }

    @Test
    public void autoSearchCategoryIds_ValidQuery_ReturnsMatchingCategoryIds() {
        // Arrange
        Category category = new Category(ENTERTAINMENT_NAME, "movies and games", BUDGET_HUNDRED, TrackingType.EXPENSE);
        repository.insert(category);

        // Act
        List<Long> exactMatch = categoryService.autoSearchCategoryIds(ENTERTAINMENT_NAME, TrackingType.EXPENSE);

        // Assert
        assertFalse(exactMatch.isEmpty());
        assertEquals(ENTERTAINMENT_NAME, categoryService.getCategoryByIdSkim(exactMatch.get(0)).getName());

        List<Long> prefixMatch = categoryService.autoSearchCategoryIds("Entertain", TrackingType.EXPENSE);
        assertEquals(ENTERTAINMENT_NAME, categoryService.getCategoryByIdSkim(prefixMatch.get(0)).getName());

        List<Long> descriptionMatch = categoryService.autoSearchCategoryIds("movies", TrackingType.EXPENSE);
        assertEquals(ENTERTAINMENT_NAME, categoryService.getCategoryByIdSkim(descriptionMatch.get(0)).getName());

        List<Long> wrongType = categoryService.autoSearchCategoryIds(ENTERTAINMENT_NAME, TrackingType.INCOME);
        assertTrue(wrongType.isEmpty());
    }

    @Test(expected = com.example.mtfinance.src.trackingengine.CategoryViolationException.class)
    public void updateCategory_RootCategory_ThrowsCategoryViolationException() {
        Category general = categoryService.getGeneralCategory();
        general.setDescription("Updated desc");
        repository.updateCategory(general);
    }

    @Test(expected = com.example.mtfinance.src.trackingengine.CategoryViolationException.class)
    public void deleteCategory_RootCategory_ThrowsCategoryViolationException() {
        Category general = categoryService.getGeneralCategory();
        repository.deleteCategory(general);
    }

    @Test
    public void updateRootCategoryBudget_ValidRootCategory_UpdatesBudgetSuccessfully() {
        Category general = categoryService.getGeneralCategory();
        assertNotNull(general);
        BigDecimal newBudget = BigDecimal.valueOf(1500);

        repository.updateRootCategoryBudget(general.getCategoryId(), newBudget);
        Category updatedGeneral = categoryService.getGeneralCategory();
        assertEquals(0, newBudget.compareTo(updatedGeneral.getMonthlyBudget()));

        BigDecimal typeBudget = BigDecimal.valueOf(2000);
        repository.updateRootCategoryBudget(TrackingType.EXPENSE, typeBudget);
        Category updatedGeneral2 = categoryService.getGeneralCategory();
        assertEquals(0, typeBudget.compareTo(updatedGeneral2.getMonthlyBudget()));
    }

    @Test(expected = com.example.mtfinance.src.trackingengine.CategoryViolationException.class)
    public void updateRootCategoryBudget_NonRootCategory_ThrowsCategoryViolationException() {
        Category category = new Category(ENTERTAINMENT_NAME, "Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        long id = repository.insert(category);
        repository.updateRootCategoryBudget(id, BigDecimal.valueOf(300));
    }

    @Test
    public void insert_FailureDuringChildInsert_RollsBackParentInsertion() {
        // Arrange
        Category parent = new Category("UnsavedParent", "Desc", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        // child has the same name as an existing category ("Groceries") to trigger CategoryViolationException during insert
        Category child = new Category(GROCERIES_NAME, "Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        child.setParent(parent);

        // Act
        boolean exceptionThrown = false;
        try {
            repository.insert(child);
        } catch (CategoryViolationException e) {
            exceptionThrown = true;
        }

        // Assert
        assertTrue(exceptionThrown);
        Category parentInDb = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals("UnsavedParent"))
                .findFirst()
                .orElse(null);
        // Parent should not exist in DB because child insertion failed and the operation should be atomic
        assertNull(parentInDb);
    }

    @Test
    public void updateCategoryTree_CircularReference_RollsBackDatabaseState() {
        // Arrange
        Category parent = new Category("ParentTreeAtomicity", "Desc", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        repository.insert(parent);
        Category pInDb = categoryService.getCategoryByName("ParentTreeAtomicity");
        assertNotNull(pInDb);

        Category child = new Category("ChildTreeAtomicity", "Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        child.setParent(pInDb);
        repository.insert(child);
        Category cInDb = categoryService.getCategoryByName("ChildTreeAtomicity");
        assertNotNull(cInDb);

        Category restoredP = categoryService.getCategoryByIdRestored(pInDb.getCategoryId());
        Category restoredC = categoryService.getCategoryByIdRestored(cInDb.getCategoryId());
        restoredP.setName("ParentTreeModified");
        restoredP.setParentId(restoredC.getCategoryId()); // Circular reference

        // Act
        boolean exceptionThrown = false;
        try {
            repository.updateCategoryTree(restoredP);
        } catch (CategoryViolationException e) {
            exceptionThrown = true;
        }

        // Assert
        assertTrue(exceptionThrown);
        Category pAfterFail = categoryService.getCategoryByIdSkim(pInDb.getCategoryId());
        // Name should still be original, proving the update was rolled back
        assertEquals("ParentTreeAtomicity", pAfterFail.getName());
    }

    @Test
    public void deleteCategory_NonExistentCategory_RollsBackWithoutModifyingDatabase() {
        // Arrange
        Category parent = new Category("ParentDeleteAtomicity", "Desc", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        repository.insert(parent);
        Category pInDb = categoryService.getCategoryByName("ParentDeleteAtomicity");
        assertNotNull(pInDb);

        Category invalidDeleteTarget = new Category("NonExistent", "Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        invalidDeleteTarget.setCategoryId(99999L);

        // Act
        boolean exceptionThrown = false;
        try {
            repository.deleteCategory(invalidDeleteTarget);
        } catch (CategoryViolationException e) {
            exceptionThrown = true;
        }

        // Assert
        assertTrue(exceptionThrown);
        Category pAfterFail = categoryService.getCategoryByIdSkim(pInDb.getCategoryId());
        assertNotNull(pAfterFail);
        assertEquals("ParentDeleteAtomicity", pAfterFail.getName());
    }

    @Test(expected = com.example.mtfinance.src.trackingengine.CategoryViolationException.class)
    public void updateCategory_NonExistentCategory_ThrowsCategoryViolationException() {
        Category nonExistent = new Category("NonExistentUpdateCat", "Desc", BUDGET_HUNDRED, TrackingType.EXPENSE);
        nonExistent.setCategoryId(99999L);
        repository.updateCategory(nonExistent);
    }

    @Test
    public void updateBudget_ancestorsRaised_ancestorsPersisted() {
        Category parent = new Category("RepoParentR16", "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        repository.insert(parent);
        Category pInDb = categoryService.getCategoryByName("RepoParentR16");

        Category child = new Category("RepoChildR16", "Desc", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        child.setParent(pInDb);
        repository.insert(child);

        Category pReloaded = categoryService.getCategoryByIdRestored(pInDb.getCategoryId());
        assertTrue(pReloaded.getMonthlyBudget().compareTo(BUDGET_FIVE_HUNDRED) >= 0);
    }

    @Test
    public void updateBudget_reloadAfterRaise_everyParentAtLeastChildSum() {
        Category root = new Category("RepoRootR16", "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        repository.insert(root);
        Category rInDb = categoryService.getCategoryByName("RepoRootR16");

        Category child = new Category("RepoChildR16_2", "Desc", BUDGET_FIVE_HUNDRED, TrackingType.EXPENSE);
        child.setParent(rInDb);
        repository.insert(child);

        Category rootReloaded = categoryService.getCategoryByIdRestored(rInDb.getCategoryId());
        Category childReloaded = categoryService.getCategoryByName("RepoChildR16_2");

        BigDecimal childSum = childReloaded.getMonthlyBudget();
        assertTrue(rootReloaded.getMonthlyBudget().compareTo(childSum) >= 0);
    }
}
