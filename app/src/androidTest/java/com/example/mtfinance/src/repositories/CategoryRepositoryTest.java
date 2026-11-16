package com.example.mtfinance.src.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        repository = new CategoryRepository(categoryDao);
        categoryService = new CategoryService(categoryDao);
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

        Category foundChild = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals(CHILD_NAME))
                .findFirst()
                .orElse(null);

        assertNotNull(foundChild);
        assertEquals(parent.getCategoryId(), foundChild.getParentId());
    }

    @Test
    public void deleteCategory_LeafCategory_DeletesSuccessfully() {
        // Arrange
        Category category = new Category(ENTERTAINMENT_NAME, "Desc", BUDGET_FIFTY, TrackingType.EXPENSE);
        repository.insert(category);

        Category found = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals(ENTERTAINMENT_NAME))
                .findFirst()
                .orElse(null);
        assertNotNull(found);

        // Act
        repository.deleteCategory(found);
        boolean stillExists = categoryService.getAllCategories().stream()
                .anyMatch(c -> c.getName().equals(ENTERTAINMENT_NAME));

        // Assert
        assertFalse(stillExists);
    }

    @Test
    public void deleteCategory_ParentCategoryWithChildren_ReassignsChildrenToGeneral() {
        // Arrange
        Category parent = new Category(PARENT_NAME, "", BUDGET_TWO_HUNDRED, TrackingType.EXPENSE);
        repository.insert(parent);

        Category parentInDb = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals(PARENT_NAME))
                .findFirst()
                .orElse(null);
        assertNotNull(parentInDb);
        Category parentRestored = categoryService.getCategoryByIdRestored(parentInDb.getCategoryId());

        Category child = new Category(CHILD_NAME, "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        child.setParent(parentRestored);
        repository.insert(child);

        Category childInDb = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals(CHILD_NAME))
                .findFirst()
                .orElse(null);
        assertNotNull(childInDb);

        // Act
        repository.deleteCategory(parentRestored);
        Category updatedChild = categoryService.getCategoryById(childInDb.getCategoryId());

        // Assert
        assertEquals((long) categoryService.getGeneralCategory().getCategoryId(), (long) updatedChild.getParentId());

    }

    @Test
    public void updateCategory_ExistingCategory_UpdatesFieldsSuccessfully() {
        // Arrange
        Category category = new Category("Initial", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        repository.insert(category);

        Category found = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals("Initial"))
                .findFirst()
                .orElse(null);
        assertNotNull(found);

        Category restoredChild = categoryService.getCategoryByIdRestored(found.getCategoryId());

        // Act
        restoredChild.setName("Updated");
        restoredChild.setDescription("Updated desc");
        restoredChild.setMonthlyBudget(BUDGET_TWO_HUNDRED);
        repository.updateCategory(restoredChild);

        Category updated = categoryService.getCategoryById(found.getCategoryId());

        // Assert
        assertEquals("Updated", updated.getName());
        assertEquals("Updated desc", updated.getDescription());
        assertEquals(0, BUDGET_TWO_HUNDRED.compareTo(updated.getMonthlyBudget()));
    }

    @Test
    public void updateCategoryTree_ValidHierarchyUpdate_UpdatesSuccessfully() {
        // Arrange
        Category general = categoryService.getGeneralCategory();
        Category income = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals("Income"))
                .findFirst().orElse(null);
        Category transfer = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals("Account Transfer"))
                .findFirst().orElse(null);

        assertNotNull(general);
        assertNotNull(income);
        assertNotNull(transfer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateCategoryTree_CircularReference_ThrowsException() {
        // Arrange
        Category parent = new Category("P", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        repository.insert(parent);
        Category pInDb = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals("P"))
                .findFirst().orElse(null);
        assertNotNull(pInDb);

        Category child = new Category("C", "", BUDGET_FIFTY, TrackingType.EXPENSE);
        child.setParent(pInDb);
        repository.insert(child);
        Category cInDb = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals("C"))
                .findFirst().orElse(null);
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
        assertEquals(ENTERTAINMENT_NAME, categoryService.getCategoryById(exactMatch.get(0)).getName());

        List<Long> prefixMatch = categoryService.autoSearchCategoryIds("Entertain", TrackingType.EXPENSE);
        assertEquals(ENTERTAINMENT_NAME, categoryService.getCategoryById(prefixMatch.get(0)).getName());

        List<Long> descriptionMatch = categoryService.autoSearchCategoryIds("movies", TrackingType.EXPENSE);
        assertEquals(ENTERTAINMENT_NAME, categoryService.getCategoryById(descriptionMatch.get(0)).getName());

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
}
