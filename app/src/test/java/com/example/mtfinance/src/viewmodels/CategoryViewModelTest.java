package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class CategoryViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private TrackingRepository trackingRepository;

    private CategoryViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock the initial all-categories call
        MutableLiveData<List<CategoryWithTransactions>> allCategoriesLiveData = new MutableLiveData<>();
        allCategoriesLiveData.setValue(new ArrayList<>());
        when(trackingRepository.getAllCategoriesWithTransactions()).thenReturn(allCategoriesLiveData);
        
        viewModel = new CategoryViewModel(trackingRepository, synchronousExecutor);

        // Observe all LiveData to ensure they are active
        viewModel.getFilteredCategories().observeForever(res -> {});
        viewModel.getSelectedCategory().observeForever(res -> {});
        viewModel.getTotalIncludingSub().observeForever(res -> {});
        viewModel.getRemaining().observeForever(res -> {});
        viewModel.getChildrenTotals().observeForever(res -> {});
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

        when(trackingRepository.searchCategoriesWithType(query, null)).thenReturn(repoLiveData);

        // Act
        viewModel.setSearchQuery(query);

        // Assert
        verify(trackingRepository).searchCategoriesWithType(query, null);
        assertEquals(searchResults, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void filterCategories_withQueryAndType_triggersSwitchMap() {
        // Arrange
        String query = "Water";
        TrackingType type = TrackingType.EXPENSE;
        List<CategoryWithTransactions> searchResults = new ArrayList<>();
        MutableLiveData<List<CategoryWithTransactions>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(trackingRepository.searchCategoriesWithType(query, type)).thenReturn(repoLiveData);

        // Act
        viewModel.setTypeFilter(type);
        viewModel.setSearchQuery(query);

        // Assert
        verify(trackingRepository).searchCategoriesWithType(query, type);
        assertEquals(searchResults, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void setTypeFilter_updatesTypeFilterLiveData() {
        // Act
        viewModel.setTypeFilter(TrackingType.INCOME);

        // Assert
        assertEquals(TrackingType.INCOME, viewModel.getTypeFilter().getValue());
    }

    @Test
    public void setSelectedCategory_loadsDashboardData() throws InterruptedException {
        // Arrange
        Long categoryId = 1L;
        Category category = new Category("Groceries", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        category.setCategoryId(categoryId);
        CategoryWithTransactions cwt = new CategoryWithTransactions();
        cwt.category = category;

        when(trackingRepository.getCategoryWithTransactionsByCategoryId(categoryId)).thenReturn(cwt);
        when(trackingRepository.getCategoryByIdRestored(categoryId)).thenReturn(category);
        
        BigDecimal totalInc = BigDecimal.valueOf(40);
        when(trackingRepository.getTotalInCategory(any(), anyBoolean(), any(), any())).thenReturn(totalInc);
        
        Map<CategoryWithTransactions, BigDecimal> childTotals = new HashMap<>();
        when(trackingRepository.getChildrenTotals(any(), any(), any())).thenReturn(childTotals);

        CountDownLatch latch = new CountDownLatch(1);
        viewModel.getSelectedCategory().observeForever(selected -> {
            if (selected != null) latch.countDown();
        });

        // Act
        viewModel.setSelectedCategory(categoryId);

        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(totalInc, viewModel.getTotalIncludingSub().getValue());
        // Remaining = Budget(100) - totalInc(40) = 60
        assertEquals(0, BigDecimal.valueOf(60).compareTo(viewModel.getRemaining().getValue()));
        assertEquals(childTotals, viewModel.getChildrenTotals().getValue());
    }

    @Test
    public void resetSelectedCategory_clearsDashboardData() {
        // Act
        viewModel.resetSelectedCategory();

        // Assert
        assertNull(viewModel.getSelectedCategory().getValue());
        assertEquals(BigDecimal.ZERO, viewModel.getTotalIncludingSub().getValue());
        assertEquals(BigDecimal.ZERO, viewModel.getRemaining().getValue());
        assertNull(viewModel.getChildrenTotals().getValue());
    }
}
