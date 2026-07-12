package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CategoryViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private TrackingRepository trackingRepository;

    private CategoryViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock the initial all-categories call
        MutableLiveData<List<CategoryWithTransactions>> allCategoriesLiveData = new MutableLiveData<>();
        allCategoriesLiveData.setValue(new ArrayList<>());
        when(trackingRepository.getAllCategoriesWithTransactions()).thenReturn(allCategoriesLiveData);
        
        viewModel = new CategoryViewModel(trackingRepository);
    }

    @Test
    public void constructor_loadsCategoriesFromRepository() {
        // Assert
        verify(trackingRepository).getAllCategoriesWithTransactions();
        assertNotNull(viewModel.getAllCategories().getValue());
    }

    @Test
    public void setSearchQuery_updatesSearchQueryLiveData() {
        // Act
        viewModel.setSearchQuery("  Groceries  ");

        // Assert
        assertEquals("Groceries", viewModel.getSearchQuery().getValue());
    }

    @Test
    public void filterCategories_withValidQuery_triggersSwitchMap() {
        // Arrange
        String query = "Food";
        List<CategoryWithTransactions> searchResults = new ArrayList<>();
        MutableLiveData<List<CategoryWithTransactions>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(trackingRepository.searchCategories(query)).thenReturn(repoLiveData);

        // ACTIVATE the switchMap by adding an observer
        viewModel.getFilteredCategories().observeForever(res -> {});

        // Act
        viewModel.setSearchQuery(query);

        // Assert
        verify(trackingRepository).searchCategories(query);
        assertEquals(searchResults, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void filterCategories_withEmptyQuery_returnsAllCategories() {
        // Arrange
        List<CategoryWithTransactions> allCats = new ArrayList<>();
        CategoryWithTransactions cat = new CategoryWithTransactions();
        cat.category = new Category("Test", "", BigDecimal.ONE, TrackingType.EXPENSE);
        allCats.add(cat);
        
        // Mock the LiveData behavior
        MutableLiveData<List<CategoryWithTransactions>> allCategoriesLiveData = new MutableLiveData<>();
        allCategoriesLiveData.setValue(allCats);
        when(trackingRepository.getAllCategoriesWithTransactions()).thenReturn(allCategoriesLiveData);
        
        // Re-instantiate to pick up new mock
        viewModel = new CategoryViewModel(trackingRepository);

        // ACTIVATE the switchMap
        viewModel.getFilteredCategories().observeForever(res -> {});

        // Act
        viewModel.setSearchQuery("");

        // Assert
        assertEquals(allCats, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void setSelectedCategory_updatesOnBackgroundThread() throws InterruptedException {
        // Arrange
        Long categoryId = 10L;
        CategoryWithTransactions cwt = new CategoryWithTransactions();
        cwt.category = new Category("Initial", "", BigDecimal.TEN, TrackingType.EXPENSE);
        cwt.category.setCategoryId(categoryId);
        
        Category restored = new Category("Restored", "", BigDecimal.TEN, TrackingType.EXPENSE);
        restored.setCategoryId(categoryId);

        when(trackingRepository.getCategoryWithTransactionsByCategoryId(categoryId)).thenReturn(cwt);
        when(trackingRepository.getCategoryByIdRestored(categoryId)).thenReturn(restored);

        CountDownLatch latch = new CountDownLatch(1);
        viewModel.getSelectedCategory().observeForever(selected -> {
            if (selected != null && selected.category != null && "Restored".equals(selected.category.getName())) {
                latch.countDown();
            }
        });

        // Act
        viewModel.setSelectedCategory(categoryId);

        // Assert
        boolean updated = latch.await(2, TimeUnit.SECONDS);
        assertTrue("Should have updated selected category", updated);
        assertNotNull(viewModel.getSelectedCategory().getValue());
        assertEquals("Restored", viewModel.getSelectedCategory().getValue().category.getName());
    }
}
