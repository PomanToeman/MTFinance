package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private TrackingRepository trackingRepository;

    private TransactionViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock default behavior for constructor call
        when(trackingRepository.getAllTransactions()).thenReturn(new ArrayList<>());
        
        viewModel = new TransactionViewModel(trackingRepository);
    }

    @Test
    public void constructor_loadsAllTransactions() {
        // Assert
        verify(trackingRepository).getAllTransactions();
        assertNotNull(viewModel.getTransactions().getValue());
    }

    @Test
    public void setSelectedTransaction_updatesLiveData() {
        // Arrange
        Long transactionId = 1L;
        Transaction transaction = new Transaction.Builder("Lunch", BigDecimal.valueOf(10.50))
                .description("Sandwich")
                .type(TrackingType.EXPENSE)
                .build();
        transaction.setTransactionId(transactionId);
        
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Food", "Eating out", BigDecimal.valueOf(100), TrackingType.EXPENSE));

        when(trackingRepository.getTransactionById(transactionId)).thenReturn(transaction);
        when(trackingRepository.findCategoriesByTransactionId(transactionId)).thenReturn(categories);

        // Act
        viewModel.setSelectedTransaction(transactionId);

        // Assert
        verify(trackingRepository).getTransactionById(transactionId);
        verify(trackingRepository).findCategoriesByTransactionId(transactionId);
        assertEquals(transaction, viewModel.getSelectedTransaction().getValue());
        assertEquals(categories, viewModel.getCategoriesUnderSelectedTransaction().getValue());
    }

    @Test
    public void getTransactions_returnsDataFromRepository() {
        // Arrange
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction.Builder("Salary", BigDecimal.valueOf(3000))
                .description("Monthly pay")
                .type(TrackingType.INCOME)
                .build());
        
        // Note: constructor already called in setUp, so we re-instantiate or mock before
        when(trackingRepository.getAllTransactions()).thenReturn(transactions);
        
        // Act
        TransactionViewModel newViewModel = new TransactionViewModel(trackingRepository);

        // Assert
        assertEquals(transactions, newViewModel.getTransactions().getValue());
    }
}
