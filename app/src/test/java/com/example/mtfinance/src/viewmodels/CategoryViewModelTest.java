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
import com.example.mtfinance.src.repositories.SystemService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@ExtendWith(InstantExecutorExtension.class)
public class CategoryViewModelTest {

    private static final Long CATEGORY_ID_ONE = 1L;
    private static final BigDecimal BUDGET_HUNDRED = BigDecimal.valueOf(100.0);
    private static final BigDecimal TOTAL_SPENT_FORTY = BigDecimal.valueOf(40.0);
    private static final BigDecimal EXPECTED_REMAINING_SIXTY = BigDecimal.valueOf(60.0);

    private static final LocalDate TEST_DATE_NOV_18 = LocalDate.of(2026, 11, 18);
    private static final LocalDate EXPECTED_PERIOD_START_NOV_14 = LocalDate.of(2026, 11, 14);
    private static final LocalDate EXPECTED_PERIOD_END_DEC_13 = LocalDate.of(2026, 12, 13);

    private static final LocalDate TEST_DATE_JAN_31 = LocalDate.of(2026, 1, 31);
    private static final LocalDate EXPECTED_PERIOD_END_FEB_27 = LocalDate.of(2026, 2, 27);

    private static final int START_DAY_FOURTEEN = 14;
    private static final int START_DAY_THIRTY_ONE = 31;
    private static final int MONTH_OFFSET_MINUS_SIX = -6;
    private static final int ZERO_MONTH_OFFSET = 0;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private SystemService systemService;

    private CategoryViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        MutableLiveData<List<CategoryWithTransactions>> allCategoriesLiveData = new MutableLiveData<>();
        allCategoriesLiveData.setValue(new ArrayList<>());
        when(trackingService.getAllCategoriesWithTransactions()).thenReturn(allCategoriesLiveData);

        viewModel = new CategoryViewModel(categoryService, trackingService, systemService, synchronousExecutor);

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
        Category category = new Category("Groceries", "", BUDGET_HUNDRED, TrackingType.EXPENSE);
        category.setCategoryId(CATEGORY_ID_ONE);
        CategoryWithTransactions cwt = new CategoryWithTransactions();
        cwt.category = category;

        when(trackingService.getCategoryWithTransactionsByCategoryId(CATEGORY_ID_ONE)).thenReturn(cwt);
        when(categoryService.getCategoryByIdRestored(CATEGORY_ID_ONE)).thenReturn(category);

        when(trackingService.getTotalInCategory(any(), anyBoolean(), any(), any())).thenReturn(TOTAL_SPENT_FORTY);

        Map<CategoryWithTransactions, BigDecimal> childTotals = new HashMap<>();
        when(trackingService.getChildrenTotals(any(), any(), any())).thenReturn(childTotals);

        CountDownLatch latch = new CountDownLatch(1);
        viewModel.getSelectedCategory().observeForever(selected -> {
            if (selected != null) latch.countDown();
        });

        viewModel.setSelectedCategory(CATEGORY_ID_ONE);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertAll(
            () -> assertEquals(TOTAL_SPENT_FORTY, viewModel.getTotalIncludingSub().getValue()),
            () -> assertEquals(0, EXPECTED_REMAINING_SIXTY.compareTo(viewModel.getRemaining().getValue())),
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

    @Test
    public void calculatePayPeriodDates_withCustomStartDay_computesCorrectRange() {
        LocalDate[] dates = CategoryViewModel.calculatePayPeriodDates(START_DAY_FOURTEEN, ZERO_MONTH_OFFSET, true, TEST_DATE_NOV_18);
        assertEquals(EXPECTED_PERIOD_START_NOV_14, dates[0]);
        assertEquals(EXPECTED_PERIOD_END_DEC_13, dates[1]);
    }

    @Test
    public void calculatePayPeriodDates_startDay31_shortMonthNoOverlap() {
        LocalDate[] dates = CategoryViewModel.calculatePayPeriodDates(START_DAY_THIRTY_ONE, ZERO_MONTH_OFFSET, true, TEST_DATE_JAN_31);
        assertEquals(TEST_DATE_JAN_31, dates[0]);
        assertEquals(EXPECTED_PERIOD_END_FEB_27, dates[1]);
    }

    @Test
    public void jumpToMonthOffset_updatesMonthOffsetValue() {
        viewModel.jumpToMonthOffset(MONTH_OFFSET_MINUS_SIX);
        assertEquals(MONTH_OFFSET_MINUS_SIX, viewModel.getMonthOffsetValue());
    }
}
