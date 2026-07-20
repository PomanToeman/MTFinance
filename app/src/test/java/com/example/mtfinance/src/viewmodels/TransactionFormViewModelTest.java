package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionFormViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private TrackingRepository trackingRepository;

    private TransactionFormViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new TransactionFormViewModel(trackingRepository);
    }

    @Test
    public void saveTransaction_multipleCategories_insertsCorrectly() {
        // Arrange
        Set<Long> categoryIds = new HashSet<>(Arrays.asList(1L, 2L));
        when(trackingRepository.verifyExistingIdsCategories(categoryIds)).thenReturn(true);
        when(trackingRepository.categoryExists(anyLong())).thenReturn(true);

        viewModel.setName("Double Category");
        viewModel.setAmount(BigDecimal.TEN);
        viewModel.setType(TrackingType.EXPENSE);
        viewModel.addCategoryId(1L);
        viewModel.addCategoryId(2L);


        // Act
        viewModel.saveTransaction();

        // Assert
        // Logic in viewModel: boolean first = true; for (Long catId : categoryIds.getValue()) { ... }
        // The first category should trigger insertTransaction
        verify(trackingRepository).insertTransaction(any(Transaction.class), anyLong());
        // Subsequent categories should trigger insertRelationship
        verify(trackingRepository, times(categoryIds.size() - 1)).insertRelationship(anyLong(), anyLong());
        assertEquals("Transaction saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void editTransaction_syncsRelationshipsCorrectly() {
        // Arrange
        long transId = 100L;
        Transaction existing = new Transaction.Builder("Old", BigDecimal.TEN).build();
        existing.setTransactionId(transId);
        
        List<Long> oldCategoryIds = Arrays.asList(1L, 2L); // Categories 1 and 2 were associated
        Set<Long> newCategoryIds = new HashSet<>(Arrays.asList(2L, 3L)); // Now 2 and 3 are associated

        when(trackingRepository.transactionExists(transId)).thenReturn(true);
        when(trackingRepository.getTransactionById(transId)).thenReturn(existing);
        when(trackingRepository.getCategoryIdsByTransactionId(transId)).thenReturn(oldCategoryIds);
        when(trackingRepository.verifyExistingIdsCategories(any())).thenReturn(true);
        when(trackingRepository.categoryExists(anyLong())).thenReturn(true);

        // Act - Load
        viewModel.setTransactionId(transId);
        
        // Act - Edit relationships: Remove 1, Add 3. Category 2 stays.
        viewModel.removeCategoryId(1L);
        viewModel.addCategoryId(3L);
        viewModel.saveTransaction();

        // Assert
        verify(trackingRepository).updateTransaction(existing);
        // Category 3 is new, so insert relationship
        verify(trackingRepository).insertRelationship(transId, 3L);
        // Category 1 is removed, so delete relationship
        verify(trackingRepository).deleteRelationship(transId, 1L);
        // Category 2 was already there, shouldn't be touched by insert/delete again in the sync loop
        verify(trackingRepository, never()).insertRelationship(transId, 2L);
        verify(trackingRepository, never()).deleteRelationship(transId, 2L);
        
        assertEquals("Transaction Updated successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void deleteTransaction_callsRepository() {
        // Arrange
        long transId = 50L;
        Transaction t = new Transaction.Builder("Delete Me", BigDecimal.ONE).build();
        t.setTransactionId(transId);
        
        when(trackingRepository.transactionExists(transId)).thenReturn(true);
        when(trackingRepository.getTransactionById(transId)).thenReturn(t);
        when(trackingRepository.getCategoryIdsByTransactionId(transId)).thenReturn(Collections.singletonList(1L));

        viewModel.setTransactionId(transId);

        // Act
        viewModel.deleteTransaction();

        // Assert
        verify(trackingRepository).deleteTransaction(transId);
        assertEquals("Transaction deleted successfully", viewModel.getSuccessMessage().getValue());
        // Verify fields cleared
        assertEquals("", viewModel.getName().getValue());
        assertEquals(0, BigDecimal.ZERO.compareTo(viewModel.getAmount().getValue()));
    }

    @Test
    public void saveTransaction_emptyName_failsValidation() {
        viewModel.setName("");
        viewModel.setAmount(BigDecimal.valueOf(10));
        viewModel.addCategoryId(1L);
        viewModel.saveTransaction();
        assertTrue(viewModel.getErrorMessage().getValue().contains("Name cannot be empty"));
    }

    @Test
    public void saveTransaction_noCategories_failsValidation() {
        viewModel.setName("Test");
        viewModel.setAmount(BigDecimal.TEN);
        // No addCategoryId called
        viewModel.saveTransaction();
        assertTrue(viewModel.getErrorMessage().getValue().contains("Category cannot be empty"));
    }

    @Test
    public void saveTransaction_nonExistentCategory_failsValidation() {
        viewModel.setName("Test");
        viewModel.setAmount(BigDecimal.TEN);
        viewModel.addCategoryId(99L);
        
        when(trackingRepository.verifyExistingIdsCategories(any())).thenReturn(false);

        viewModel.saveTransaction();
        assertTrue(viewModel.getErrorMessage().getValue().contains("do not exist"));
    }

    @Test
    public void saveTransaction_fiftyTransactions_insertsAll() {
        long categoryId = 1L;
        Set<Long> ids = Collections.singleton(categoryId);
        when(trackingRepository.verifyExistingIdsCategories(any())).thenReturn(true);
        when(trackingRepository.categoryExists(categoryId)).thenReturn(true);

        int count = 50;
        for (int i = 1; i <= count; i++) {
            viewModel.clear();
            viewModel.setName("Trans " + i);
            viewModel.setAmount(BigDecimal.valueOf(i));
            viewModel.addCategoryId(categoryId);
            viewModel.saveTransaction();
        }

        // Each saveTransaction loop should call insertTransaction once for the single category
        verify(trackingRepository, times(count)).insertTransaction(any(Transaction.class), eq(categoryId));
    }
}
