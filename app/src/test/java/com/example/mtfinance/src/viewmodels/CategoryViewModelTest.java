package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(InstantExecutorExtension.class)
public class CategoryViewModelTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackingService trackingService;

    private CategoryViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        MutableLiveData<List<CategoryWithTransactions>> allCategoriesLiveData = new MutableLiveData<>();
        allCategoriesLiveData.setValue(new ArrayList<>());
        when(trackingService.getAllCategoriesWithTransactions()).thenReturn(allCategoriesLiveData);

        viewModel = new CategoryViewModel(categoryService, trackingService, synchronousExecutor);

        viewModel.getFilteredCategories().observeForever(res -> {});
        viewModel.getSelectedCategory().observeForever(res -> {});
        viewModel.getTotalIncludingSub().observeForever(res -> {});
        viewModel.getRemaining().observeForever(res -> {});
        viewModel.getChildrenTotals().observeForever(res -> {});
    }

    @Test
    public void constructor_loadsCategoriesFromRepository() {
        verify(trackingService).getAllCategoriesWithTransactions();
        assertNotNull(viewModel.getAllCategories().getValue());
    }

    @Test
    public void setSearchQuery_updatesSearchQueryLiveData() {
        viewModel.setSearchQuery("  Groceries  ");
        assertEquals("Groceries", viewModel.getSearchQuery().getValue());
    }

    @Test
    public void filterCategories_withValidQuery_triggersSwitchMap() {
        String query = "Food";
        List<CategoryWithTransactions> searchResults = new ArrayList<>();
        MutableLiveData<List<CategoryWithTransactions>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(trackingService.searchCategoriesWithType(query, null)).thenReturn(repoLiveData);

        viewModel.setSearchQuery(query);

        verify(trackingService).searchCategoriesWithType(query, null);
        assertEquals(searchResults, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void filterCategories_withQueryAndType_triggersSwitchMap() {
        String query = "Water";
        TrackingType type = TrackingType.EXPENSE;
        List<CategoryWithTransactions> searchResults = new ArrayList<>();
        MutableLiveData<List<CategoryWithTransactions>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(trackingService.searchCategoriesWithType(query, type)).thenReturn(repoLiveData);

        viewModel.setTypeFilter(type);
        viewModel.setSearchQuery(query);

        verify(trackingService).searchCategoriesWithType(query, type);
        assertEquals(searchResults, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void setTypeFilter_updatesTypeFilterLiveData() {
        viewModel.setTypeFilter(TrackingType.INCOME);
        assertEquals(TrackingType.INCOME, viewModel.getTypeFilter().getValue());
    }

    @Test
    public void setSelectedCategory_loadsDashboardData() throws InterruptedException {
        Long categoryId = 1L;
        Category category = new Category("Groceries", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        category.setCategoryId(categoryId);
        CategoryWithTransactions cwt = new CategoryWithTransactions();
        cwt.category = category;

        when(trackingService.getCategoryWithTransactionsByCategoryId(categoryId)).thenReturn(cwt);
        when(categoryService.getCategoryByIdRestored(categoryId)).thenReturn(category);

        BigDecimal totalInc = BigDecimal.valueOf(40);
        when(trackingService.getTotalInCategory(any(), anyBoolean(), any(), any())).thenReturn(totalInc);

        Map<CategoryWithTransactions, BigDecimal> childTotals = new HashMap<>();
        when(trackingService.getChildrenTotals(any(), any(), any())).thenReturn(childTotals);

        CountDownLatch latch = new CountDownLatch(1);
        viewModel.getSelectedCategory().observeForever(selected -> {
            if (selected != null) latch.countDown();
        });

        viewModel.setSelectedCategory(categoryId);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertAll(
            () -> assertEquals(totalInc, viewModel.getTotalIncludingSub().getValue()),
            () -> assertEquals(0, BigDecimal.valueOf(60).compareTo(viewModel.getRemaining().getValue())),
            () -> assertEquals(childTotals, viewModel.getChildrenTotals().getValue())
        );
    }

    @Test
    public void resetSelectedCategory_clearsDashboardData() {
        viewModel.resetSelectedCategory();

        assertAll(
            () -> assertNull(viewModel.getSelectedCategory().getValue()),
            () -> assertEquals(BigDecimal.ZERO, viewModel.getTotalIncludingSub().getValue()),
            () -> assertEquals(BigDecimal.ZERO, viewModel.getRemaining().getValue()),
            () -> assertNull(viewModel.getChildrenTotals().getValue())
        );
    }
}
