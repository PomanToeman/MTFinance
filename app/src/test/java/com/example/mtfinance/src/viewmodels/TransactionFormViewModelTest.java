package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new TransactionFormViewModel(trackingService, categoryService, transactionService, trackingWriteRepository, synchronousExecutor);
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
}
