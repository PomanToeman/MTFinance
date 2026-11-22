package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@ExtendWith(InstantExecutorExtension.class)
public class CategoryViewModelTest {

    private static final LocalDate TEST_DATE_NOV_18 = LocalDate.of(2026, 11, 18);
    private static final LocalDate EXPECTED_PERIOD_START_NOV_14 = LocalDate.of(2026, 11, 14);
    private static final LocalDate EXPECTED_PERIOD_END_DEC_13 = LocalDate.of(2026, 12, 13);

    private static final LocalDate TEST_DATE_JAN_31 = LocalDate.of(2026, 1, 31);
    private static final LocalDate EXPECTED_PERIOD_END_FEB_27 = LocalDate.of(2026, 2, 27);

    private static final int START_DAY_FOURTEEN = 14;
    private static final int START_DAY_THIRTY_ONE = 31;
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
}
