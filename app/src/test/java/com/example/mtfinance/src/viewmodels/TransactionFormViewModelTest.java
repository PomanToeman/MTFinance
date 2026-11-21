package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

@ExtendWith(InstantExecutorExtension.class)
public class TransactionFormViewModelTest {

    @Mock
    private TrackingService trackingService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TrackingRepository trackingWriteRepository;

    private TransactionFormViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    private static final String VALID_ISO_DATE_STR = "2026-11-21";
    private static final String VALID_DD_MM_YYYY_DATE_STR = "21/11/2026";
    private static final String INVALID_DATE_STR = "not-a-valid-date";
    private static final int EXPECTED_YEAR = 2026;
    private static final int EXPECTED_MONTH = 11;
    private static final int EXPECTED_DAY = 21;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new TransactionFormViewModel(trackingService, categoryService, transactionService, trackingWriteRepository, synchronousExecutor);

        viewModel.getName().observeForever(s -> {});
        viewModel.getAmount().observeForever(a -> {});
        viewModel.getCategoryIds().observeForever(c -> {});
        viewModel.getFieldErrors().observeForever(m -> {});
    }

    @Test
    public void saveTransaction_multipleCategories_insertsCorrectly() {
        Set<Long> categoryIds = new HashSet<>(Arrays.asList(1L, 2L));
        when(categoryService.verifyExistingIds(categoryIds)).thenReturn(true);

        viewModel.setName("Double Category");
        viewModel.setAmount(BigDecimal.TEN);
        viewModel.setType(TrackingType.EXPENSE);
        viewModel.addCategoryId(1L);
        viewModel.addCategoryId(2L);

        viewModel.saveTransaction();

        verify(trackingWriteRepository).insertTransaction(any(Transaction.class), anyLong());
        verify(trackingWriteRepository, times(categoryIds.size() - 1)).insertRelationship(anyLong(), anyLong());
        assertEquals("Transaction saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "0", "0.00"})
    public void saveTransaction_bvaInvalidAmount_failsValidation(String invalidAmount) {
        Set<Long> categoryIds = new HashSet<>(Arrays.asList(1L));
        when(categoryService.verifyExistingIds(categoryIds)).thenReturn(true);

        viewModel.setName("Valid Name");
        viewModel.setAmount(new BigDecimal(invalidAmount));
        viewModel.addCategoryId(1L);

        viewModel.saveTransaction();

        verify(trackingWriteRepository, never()).insertTransaction(any(), anyLong());
        Map<TransactionFormViewModel.TransactionErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertFalse(errors.get(TransactionFormViewModel.TransactionErrorFields.AMOUNT).isEmpty())
        );
    }

    @Test
    public void saveTransaction_emptyName_populatesFieldErrorAndFailsValidation() {
        Set<Long> categoryIds = new HashSet<>(Arrays.asList(1L));
        when(categoryService.verifyExistingIds(categoryIds)).thenReturn(true);

        viewModel.setName("");
        viewModel.setAmount(BigDecimal.TEN);
        viewModel.addCategoryId(1L);

        viewModel.saveTransaction();

        verify(trackingWriteRepository, never()).insertTransaction(any(), anyLong());
        Map<TransactionFormViewModel.TransactionErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertTrue(errors.get(TransactionFormViewModel.TransactionErrorFields.NAME).contains("Name cannot be empty")),
            () -> assertEquals("", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void saveTransaction_emptyCategories_populatesFieldErrorAndFailsValidation() {
        viewModel.setName("Valid Name");
        viewModel.setAmount(BigDecimal.TEN);

        viewModel.saveTransaction();

        verify(trackingWriteRepository, never()).insertTransaction(any(), anyLong());
        Map<TransactionFormViewModel.TransactionErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertTrue(errors.get(TransactionFormViewModel.TransactionErrorFields.CATEGORY_IDS).contains("Category cannot be empty")),
            () -> assertEquals("", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void deleteTransaction_validId_deletesSuccessfully() {
        long transId = 10L;
        Transaction t = new Transaction.Builder("Dinner", BigDecimal.TEN).build();
        t.setTransactionId(transId);

        when(transactionService.transactionExists(transId)).thenReturn(true);
        when(transactionService.getById(transId)).thenReturn(t);

        viewModel.setTransactionId(transId);
        viewModel.deleteTransaction();

        assertAll(
            () -> verify(trackingWriteRepository).deleteTransaction(transId),
            () -> assertEquals("Transaction deleted successfully", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void setDateText_validIsoDate_setsDateAndClearsError() {
        viewModel.setDateText(VALID_ISO_DATE_STR);
        java.time.LocalDateTime dateVal = viewModel.getDate().getValue();

        assertAll(
            () -> assertNotNull(dateVal),
            () -> assertEquals(EXPECTED_YEAR, dateVal != null ? dateVal.getYear() : 0),
            () -> assertEquals(EXPECTED_MONTH, dateVal != null ? dateVal.getMonthValue() : 0),
            () -> assertEquals(EXPECTED_DAY, dateVal != null ? dateVal.getDayOfMonth() : 0),
            () -> assertEquals("", viewModel.getFieldErrors().getValue().get(TransactionFormViewModel.TransactionErrorFields.DATE))
        );
    }

    @Test
    public void setDateText_validDdMmYyyyDate_setsDateAndClearsError() {
        viewModel.setDateText(VALID_DD_MM_YYYY_DATE_STR);
        java.time.LocalDateTime dateVal = viewModel.getDate().getValue();

        assertAll(
            () -> assertNotNull(dateVal),
            () -> assertEquals(EXPECTED_YEAR, dateVal != null ? dateVal.getYear() : 0),
            () -> assertEquals(EXPECTED_MONTH, dateVal != null ? dateVal.getMonthValue() : 0),
            () -> assertEquals(EXPECTED_DAY, dateVal != null ? dateVal.getDayOfMonth() : 0),
            () -> assertEquals("", viewModel.getFieldErrors().getValue().get(TransactionFormViewModel.TransactionErrorFields.DATE))
        );
    }

    @Test
    public void setDateText_invalidDate_setsFieldError() {
        viewModel.setDateText(INVALID_DATE_STR);

        Map<TransactionFormViewModel.TransactionErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertEquals("Invalid date format. Accepted formats: yyyy-MM-dd or dd/MM/yyyy", errors.get(TransactionFormViewModel.TransactionErrorFields.DATE))
        );
    }
}
