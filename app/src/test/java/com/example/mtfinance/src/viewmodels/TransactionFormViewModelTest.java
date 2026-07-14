package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

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
    public void saveTransaction_validData_insertsIntoRepository() {
        // Arrange
        long categoryId = 1L;
        Category category = new Category("Groceries", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        category.setCategoryId(categoryId);
        when(trackingRepository.categoryExists(categoryId)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(categoryId)).thenReturn(category);

        viewModel.setName("Milk");
        viewModel.setAmount(BigDecimal.valueOf(3.50));
        viewModel.setCategoryId(categoryId);
        viewModel.setType(TrackingType.EXPENSE);
        viewModel.setDate(LocalDate.now());

        // Act
        viewModel.saveTransaction();

        // Assert
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(trackingRepository).insertTransaction(transactionCaptor.capture(), eq(categoryId));
        
        Transaction captured = transactionCaptor.getValue();
        assertEquals("Milk", captured.getName());
        assertEquals(0, BigDecimal.valueOf(3.50).compareTo(captured.getAmount()));
        assertEquals(TrackingType.EXPENSE, captured.getType());
        assertEquals("Transaction saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void saveTransaction_emptyName_failsValidation() {
        // Arrange
        viewModel.setName("");
        viewModel.setAmount(BigDecimal.valueOf(10));

        // Act
        viewModel.saveTransaction();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("Name cannot be empty"));
    }

    @Test
    public void saveTransaction_invalidAmount_failsValidation() {
        // Arrange
        viewModel.setName("Test");
        viewModel.setAmount(BigDecimal.valueOf(-1));

        // Act
        viewModel.saveTransaction();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("Invalid amount"));
    }

    @Test
    public void saveTransaction_nullCategory_failsValidation() {
        // Arrange
        viewModel.setName("Test");
        viewModel.setAmount(BigDecimal.valueOf(10));
        viewModel.setCategoryId(null);

        // Act
        viewModel.saveTransaction();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("Category cannot be empty"));
    }

    @Test
    public void saveTransaction_nonExistentCategory_failsValidation() {
        // Arrange
        long categoryId = 99L;
        when(trackingRepository.categoryExists(categoryId)).thenReturn(false);

        viewModel.setName("Test");
        viewModel.setAmount(BigDecimal.valueOf(10));
        viewModel.setCategoryId(categoryId);

        // Act
        viewModel.saveTransaction();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("Category does not exist"));
    }

    @Test
    public void saveTransaction_fiftyTransactions_insertsAll() {
        // Arrange
        long categoryId = 1L;
        Category category = new Category("Test", "", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
        category.setCategoryId(categoryId);
        when(trackingRepository.categoryExists(categoryId)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(categoryId)).thenReturn(category);

        int count = 50;

        // Act
        for (int i = 1; i <= count; i++) {
            viewModel.setName("Transaction " + i);
            viewModel.setAmount(BigDecimal.valueOf(i));
            viewModel.setCategoryId(categoryId);
            viewModel.setType(TrackingType.EXPENSE);
            viewModel.setDate(LocalDate.now());
            
            viewModel.saveTransaction();
        }

        // Assert
        verify(trackingRepository, times(count)).insertTransaction(any(Transaction.class), eq(categoryId));
        assertEquals("Transaction saved successfully", viewModel.getSuccessMessage().getValue());
        assertEquals("", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void saveTransaction_duplicateTransaction_failsValidation() {
        // Arrange
        long categoryId = 1L;
        when(trackingRepository.categoryExists(categoryId)).thenReturn(true);
        when(trackingRepository.transactionHashExists(any(String.class))).thenReturn(true);

        viewModel.setName("Duplicate");
        viewModel.setAmount(BigDecimal.TEN);
        viewModel.setCategoryId(categoryId);

        // Act
        viewModel.saveTransaction();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("already exists"));
    }

    @Test
    public void editTransaction_updatesOnlyDescription() {
        // Arrange
        long transactionId = 123L;
        Transaction existingTransaction = new Transaction.Builder("Dinner", new BigDecimal("50.00"))
                .description("Old Description")
                .type(TrackingType.EXPENSE)
                .build();
        existingTransaction.setTransactionId(transactionId);

        when(trackingRepository.transactionExists(transactionId)).thenReturn(true);
        when(trackingRepository.getTransactionById(transactionId)).thenReturn(existingTransaction);
        when(trackingRepository.getCategoryIdsByTransactionId(transactionId)).thenReturn(Collections.singletonList(1L));
        when(trackingRepository.categoryExists(1L)).thenReturn(true);

        // Act - Load for editing
        viewModel.setTransactionId(transactionId);
        
        // Assert loaded state
        assertEquals("Dinner", viewModel.getName().getValue());
        assertEquals("Old Description", viewModel.getDescription().getValue());

        // Act - Change description and other fields (which should be blocked)
        viewModel.setDescription("New Description");
        viewModel.setName("New Name (Should be ignored)");
        viewModel.setAmount(new BigDecimal("999"));

        viewModel.saveTransaction();

        // Assert - verify repository update
        verify(trackingRepository).updateTransaction(existingTransaction);
        assertEquals("New Description", existingTransaction.getDescription());
        assertEquals("Dinner", existingTransaction.getName()); // Should NOT have changed
        assertEquals(0, new BigDecimal("50.00").compareTo(existingTransaction.getAmount())); // Should NOT have changed
    }

    @Test
    public void clear_resetsFields() {
        // Arrange
        viewModel.setName("To be cleared");
        viewModel.setAmount(BigDecimal.TEN);
        
        // Act
        viewModel.clear();
        
        // Assert
        assertEquals("", viewModel.getName().getValue());
        assertEquals(0, BigDecimal.ZERO.compareTo(viewModel.getAmount().getValue()));
    }
}
