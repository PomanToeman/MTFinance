package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.mtfinance.src.roomdatabase.AppDatabase;
import com.example.mtfinance.src.roomdatabase.CategoryDao;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CategoryRepositoryTest {
    private AppDatabase database;
    private CategoryDao categoryDao;
    private CategoryRepository repository;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        categoryDao = database.categoryDao();
        repository = new CategoryRepository(categoryDao);
    }

    @After
    public void closeDb() {
        database.close();
        TrackingUtlis.resetCategoryCounter();
        TrackingUtlis.resetTransactionCounter();
    }

    @Test
    public void testDefaultCategoriesPopulated() {
        // Repository constructor calls populateDefaultCategories
        List<Category> all = repository.getAllCategories();
        // General Category + 2 default categories
        assertEquals(5, all.size());
        
        Category general = repository.getGeneralCategory();
        assertNotNull(general);
        assertEquals("General Category", general.getName());
    }

    @Test
    public void testInsertCategory() {
        Category newCat = new Category("Entertainment", "Movies etc", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        long id = repository.insert(newCat);
        assertTrue(id > 0);
        
        List<Category> all = repository.getAllCategories();
        assertEquals(6, all.size());
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException.class)
    public void testUniqueNameConstraint() {
        // "Groceries" is already in defaultCategories
        Category duplicate = new Category("Groceries", "Duplicate", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        repository.insert(duplicate);
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException.class)
    public void testUniqueNameConstraintCaseInsensitive() {
        // "groceries" should conflict with "Groceries" because of NOCASE collation
        Category duplicate = new Category("groceries", "Duplicate", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        repository.insert(duplicate);
    }

    @Test
    public void testGetCategoryById() {
        Category cat = new Category("TestCat", "Desc", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        long id = repository.insert(cat);

        Category retrieved = categoryDao.getById(id);
        assertNotNull(retrieved);
        assertEquals("TestCat", retrieved.getName());
    }

    @Test
    public void testAutomaticParentInsertion() {
        Category parent = new Category("Parent", "I am not in DB", BigDecimal.valueOf(200), TrackingType.EXPENSE);
        Category child = new Category("Child", "I am also not in DB", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        child.setParent(parent);

        // This should trigger recursive insertion of parent
        repository.insert(child);

        // Verify both are in the database
        List<Category> all = repository.getAllCategories();
        boolean foundParent = false;
        boolean foundChild = false;
        for (Category c : all) {
            if (c.getName().equals("Parent")) foundParent = true;
            if (c.getName().equals("Child")) foundChild = true;
        }
        assertTrue("Parent should have been automatically inserted", foundParent);
        assertTrue("Child should have been inserted", foundChild);
    }

    @Test
    public void testAutomaticGrandparentInsertion() {
        Category grandParent = new Category("Grandparent", "Top", BigDecimal.valueOf(500), TrackingType.EXPENSE);
        Category parent = new Category("ParentName", "Middle", BigDecimal.valueOf(200), TrackingType.EXPENSE);
        Category child = new Category("ChildName", "Bottom", BigDecimal.valueOf(50), TrackingType.EXPENSE);

        parent.setParent(grandParent);
        child.setParent(parent);

        // Insert only the child
        repository.insert(child);

        // Verify the entire chain is inserted
        List<Category> all = repository.getAllCategories();
        boolean foundGP = false;
        boolean foundP = false;
        for (Category c : all) {
            if (c.getName().equals("Grandparent")) foundGP = true;
            if (c.getName().equals("ParentName")) foundP = true;
        }
        assertTrue("Grandparent should have been inserted recursively", foundGP);
        assertTrue("Parent should have been inserted recursively", foundP);
    }

    @Test
    public void testDeleteCategoryRemovesFromDatabase() {
        Category toDelete = new Category("ToDelete", "Desc", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        repository.insert(toDelete);
        
        // Find it in DB to get the ID/object
        List<Category> allBefore = repository.getAllCategories();
        Category found = null;
        for (Category c : allBefore) {
            if (c.getName().equals("ToDelete")) found = c;
        }
        assertNotNull(found);

        repository.deleteCategory(found);

        List<Category> allAfter = repository.getAllCategories();
        boolean stillExists = false;
        for (Category c : allAfter) {
            if (c.getName().equals("ToDelete")) {
                stillExists = true;
                break;
            }
        }
        assertFalse("Category should be removed from database", stillExists);
    }

    @Test
    public void testDeleteCategoryReassignsChildren() {

        Category parent = new Category("ParentToDelete", "Desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category child = new Category("ChildToReassign", "Desc", BigDecimal.valueOf(50), TrackingType.EXPENSE);



        child.setParent(parent);
        assertNotNull(child.getParentId());

        repository.insert(child); // Inserts both

        assertEquals(7, repository.getAllCategories().size());


        // Find the parent object with its ID
        Category parentInDb = null;
        for (Category c : repository.getAllCategories()) {
            if (c.getName().equals("ParentToDelete")) parentInDb = c;
        }
        assertNotNull(parentInDb);
        assertEquals(repository.getGeneralCategory().getCategoryId().longValue(), parentInDb.getParentId().longValue());

        // Logic: deleteCategory calls makeChildrenCongruent()
        // which moves children of 'parent' to 'root'
        repository.deleteCategory(parent);

        Category childInDb = null;
        for (Category c : repository.getAllCategories()) {
            if (c.getName().equals("ChildToReassign")) childInDb = c;
        }


        // Verify child now has Root as parent
        // Note: Repository tests on in-memory objects and DB.
        // In your implementation, 'child' object is updated in memory.

        // assertTrue("Root should now have the child", root.getChildren(false).contains(child));
        assertNotNull("Child should now be in DB", childInDb);
        assertEquals(repository.getGeneralCategory().getCategoryId().longValue(),  child.getParentId().longValue());


    }



    @Test
    public void testGetCategoryByIdRestored() {
        // Setup hierarchy: Root -> Parent -> Child
        Category root = repository.getGeneralCategory();
        Category parent = new Category("Parent", "Parent desc", BigDecimal.valueOf(200), TrackingType.EXPENSE);
        Category child = new Category("Child", "Child desc", BigDecimal.valueOf(50), TrackingType.EXPENSE);

        parent.setParent(root);
        child.setParent(parent);

        repository.insert(child); // Inserts parent recursively

        // Fetch using the standard Room getById - parent/children will be null/empty in memory
        Category flatChild = repository.getCategoryById(child.getCategoryId());
        assertNotNull(flatChild);
        assertNull("Standard Room fetch should not have parent object", flatChild.getParent());
        assertTrue("Standard Room fetch should not have children", flatChild.getChildren(false).isEmpty());

        // Fetch using Restored method
        Category restoredChild = repository.getCategoryByIdRestored(child.getCategoryId());
        assertNotNull(restoredChild);
        
        // Verify Parent is restored
        assertNotNull("Restored fetch should have parent object", restoredChild.getParent());
        assertEquals("Parent name should match", "Parent", restoredChild.getParent().getName());
        
        // Verify Grandparent (Root) is restored
        assertNotNull("Restored fetch should have grandparent object", restoredChild.getParent().getParent());
        assertEquals("Grandparent should be General Category", root.getName(), restoredChild.getParent().getParent().getName());

        // Verify Children restoration (check Parent's children)
        Category restoredParent = restoredChild.getParent();
        assertFalse("Restored parent should have children", restoredParent.getChildren(false).isEmpty());
        assertTrue("Restored parent should contain the child", restoredParent.getChildren(false).contains(restoredChild));
    }

    @Test
    public void testDefaultCategoriesTypes() {
        List<Category> all = repository.getAllCategories();
        
        Category general = null;
        Category income = null;
        Category transfer = null;
        
        for (Category c : all) {
            if (c.getName().equals("General Category")) general = c;
            if (c.getName().equals("Income")) income = c;
            if (c.getName().equals("Account Transfer")) transfer = c;
        }
        
        assertNotNull(general);
        assertEquals(TrackingType.EXPENSE, general.getType());
        
        assertNotNull(income);
        assertEquals(TrackingType.INCOME, income.getType());
        
        assertNotNull(transfer);
        assertEquals(TrackingType.ACCOUNT_TRANSFERS, transfer.getType());
    }

    @Test
    public void testInsertNonExpenseCategoryFails() {
        Category incomeSub = new Category("Salary", "", BigDecimal.valueOf(1000), TrackingType.INCOME);
        long id = repository.insert(incomeSub);
        assertEquals(-1L, id);
        
        Category transferSub = new Category("Bank Transfer", "", BigDecimal.valueOf(100), TrackingType.ACCOUNT_TRANSFERS);
        long id2 = repository.insert(transferSub);
        assertEquals(-1L, id2);
    }
}
