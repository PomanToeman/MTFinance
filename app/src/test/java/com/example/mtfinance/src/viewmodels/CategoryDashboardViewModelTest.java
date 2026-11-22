package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@ExtendWith(InstantExecutorExtension.class)
public class CategoryDashboardViewModelTest {

    private static final Long CATEGORY_ID_ONE = 1L;
    private static final BigDecimal BUDGET_HUNDRED = BigDecimal.valueOf(100.0);
    private static final BigDecimal TOTAL_SPENT_FORTY = BigDecimal.valueOf(40.0);
    private static final BigDecimal EXPECTED_REMAINING_SIXTY = BigDecimal.valueOf(60.0);

    private static final int MONTH_OFFSET_MINUS_SIX = -6;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private SystemService systemService;

    private CategoryDashboardViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        viewModel = new CategoryDashboardViewModel(categoryService, trackingService, systemService, synchronousExecutor);

        viewModel.getSelectedCategory().observeForever(res -> {});
        viewModel.getTotalIncludingSub().observeForever(res -> {});
        viewModel.getRemaining().observeForever(res -> {});
        viewModel.getChildrenTotals().observeForever(res -> {});
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
    public void jumpToMonthOffset_updatesMonthOffsetValue() {
        viewModel.jumpToMonthOffset(MONTH_OFFSET_MINUS_SIX);
        assertEquals(MONTH_OFFSET_MINUS_SIX, viewModel.getMonthOffsetValue());
    }
}
