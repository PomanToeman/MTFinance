package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;

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
    }

    @Test
    public void testDefaultCategoriesPopulated() {
        // Repository constructor calls populateDefaultCategories
        List<Category> all = repository.getAllCategories();
        // General Category + 2 default categories
        assertEquals(3, all.size());
        
        Category general = repository.getGeneralCategory();
        assertNotNull(general);
        assertEquals("General Category", general.getName());
    }

    @Test
    public void testInsertCategory() {
        Category newCat = new Category("Entertainment", "Movies etc", BigDecimal.valueOf(50));
        long id = repository.insert(newCat);
        assertTrue(id > 0);
        
        List<Category> all = repository.getAllCategories();
        assertEquals(4, all.size());
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException.class)
    public void testUniqueNameConstraint() {
        // "Groceries" is already in defaultCategories
        Category duplicate = new Category("Groceries", "Duplicate", BigDecimal.valueOf(100));
        repository.insert(duplicate);
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException.class)
    public void testUniqueNameConstraintCaseInsensitive() {
        // "groceries" should conflict with "Groceries" because of NOCASE collation
        Category duplicate = new Category("groceries", "Duplicate", BigDecimal.valueOf(100));
        repository.insert(duplicate);
    }

    @Test
    public void testGetCategoryById() {
        Category cat = new Category("TestCat", "Desc", BigDecimal.valueOf(50));
        long id = repository.insert(cat);

        Category retrieved = categoryDao.getById(id);
        assertNotNull(retrieved);
        assertEquals("TestCat", retrieved.getName());
    }

    @Test
    public void testAutomaticParentInsertion() {
        Category parent = new Category("Parent", "I am not in DB", BigDecimal.valueOf(200));
        Category child = new Category("Child", "I am also not in DB", BigDecimal.valueOf(50));
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
        Category grandParent = new Category("Grandparent", "Top", BigDecimal.valueOf(500));
        Category parent = new Category("ParentName", "Middle", BigDecimal.valueOf(200));
        Category child = new Category("ChildName", "Bottom", BigDecimal.valueOf(50));

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
        Category toDelete = new Category("ToDelete", "Desc", BigDecimal.valueOf(50));
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
        Category root = repository.getGeneralCategory();
        Category parent = new Category("ParentToDelete", "Desc", BigDecimal.valueOf(100));
        Category child = new Category("ChildToReassign", "Desc", BigDecimal.valueOf(50));


        child.setParent(parent);

        repository.insert(child); // Inserts both

        // Find the parent object with its ID
        Category parentInDb = null;
        for (Category c : repository.getAllCategories()) {
            if (c.getName().equals("ParentToDelete")) parentInDb = c;
        }
        
        // Logic: deleteCategory calls makeChildrenCongruent()
        // which moves children of 'parent' to 'root'
        repository.deleteCategory(parent);

        // Verify child now has Root as parent
        // Note: Repository tests on in-memory objects and DB. 
        // In your implementation, 'child' object is updated in memory.
        assertEquals("Child should now point to Grandparent (Root)", root, child.getParent());
        assertTrue("Root should now have the child", root.getChildren(false).contains(child));
    }
}
