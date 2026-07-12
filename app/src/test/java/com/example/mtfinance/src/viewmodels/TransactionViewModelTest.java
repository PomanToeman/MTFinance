package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

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
        
        // Mock default behavior for constructor call: return a LiveData object
        MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
        transactionsLiveData.setValue(new ArrayList<>());
        when(trackingRepository.getAllTransactions()).thenReturn(transactionsLiveData);
        
        viewModel = new TransactionViewModel(trackingRepository);
    }

    @Test
    public void constructor_loadsAllTransactions() {
        // Assert
        verify(trackingRepository).getAllTransactions();
        assertNotNull(viewModel.getAllTransactions().getValue());
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
        List<Transaction> transactionsList = new ArrayList<>();
        transactionsList.add(new Transaction.Builder("Salary", BigDecimal.valueOf(3000))
                .description("Monthly pay")
                .type(TrackingType.INCOME)
                .build());
        
        MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
        transactionsLiveData.setValue(transactionsList);
        
        when(trackingRepository.getAllTransactions()).thenReturn(transactionsLiveData);
        
        // Act
        TransactionViewModel newViewModel = new TransactionViewModel(trackingRepository);

        // Assert
        assertEquals(transactionsList, newViewModel.getAllTransactions().getValue());
    }
}
