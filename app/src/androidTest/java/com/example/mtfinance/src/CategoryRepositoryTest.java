package com.example.mtfinance.src;

import static org.junit.Assert.assertEquals;
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
}
