package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.src.repositories.TrackingService;
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
    private TrackingService trackingService;

    private TransactionViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
        transactionsLiveData.setValue(new ArrayList<>());
        when(trackingService.getAllTransactions()).thenReturn(transactionsLiveData);

        viewModel = new TransactionViewModel(trackingService);
    }

    @Test
    public void constructor_loadsAllTransactions() {
        verify(trackingService).getAllTransactions();
        assertNotNull(viewModel.getAllTransactions().getValue());
    }

    @Test
    public void setSelectedTransaction_updatesLiveData() {
        Long transactionId = 1L;
        Transaction transaction = new Transaction.Builder("Lunch", BigDecimal.valueOf(10.50))
                .description("Sandwich")
                .type(TrackingType.EXPENSE)
                .build();
        transaction.setTransactionId(transactionId);

        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Food", "Eating out", BigDecimal.valueOf(100), TrackingType.EXPENSE));

        when(trackingService.getTransactionById(transactionId)).thenReturn(transaction);
        when(trackingService.getCategoriesByTransactionId(transactionId)).thenReturn(categories);

        viewModel.setSelectedTransaction(transactionId);

        verify(trackingService).getTransactionById(transactionId);
        verify(trackingService).getCategoriesByTransactionId(transactionId);
        assertEquals(transaction, viewModel.getSelectedTransaction().getValue());
        assertEquals(categories, viewModel.getCategoriesUnderSelectedTransaction().getValue());
    }

    @Test
    public void getTransactions_returnsDataFromRepository() {
        List<Transaction> transactionsList = new ArrayList<>();
        transactionsList.add(new Transaction.Builder("Salary", BigDecimal.valueOf(3000))
                .description("Monthly pay")
                .type(TrackingType.INCOME)
                .build());

        MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
        transactionsLiveData.setValue(transactionsList);

        when(trackingService.getAllTransactions()).thenReturn(transactionsLiveData);

        TransactionViewModel newViewModel = new TransactionViewModel(trackingService);

        assertEquals(transactionsList, newViewModel.getAllTransactions().getValue());
    }

    @Test
    public void setSearchQuery_updatesSearchQueryLiveData() {
        viewModel.setSearchQuery("  Pizza  ");
        assertEquals("Pizza", viewModel.getSearchQuery().getValue());
    }

    @Test
    public void filterTransactions_withValidQuery_triggersSwitchMap() {
        String query = "Rent";
        List<Transaction> searchResults = new ArrayList<>();
        searchResults.add(new Transaction.Builder("Monthly Rent", BigDecimal.valueOf(1500)).build());

        MutableLiveData<List<Transaction>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(trackingService.searchTransactionsWithType(query, null)).thenReturn(repoLiveData);

        viewModel.getFilteredTransactions().observeForever(res -> {});
        viewModel.setSearchQuery(query);

        verify(trackingService).searchTransactionsWithType(query, null);
        assertEquals(searchResults, viewModel.getFilteredTransactions().getValue());
    }

    @Test
    public void filterTransactions_withQueryAndType_triggersSwitchMap() {
        String query = "Pizza";
        TrackingType type = TrackingType.EXPENSE;
        List<Transaction> searchResults = new ArrayList<>();
        searchResults.add(new Transaction.Builder("Pizza Hut", BigDecimal.valueOf(25)).type(type).build());

        MutableLiveData<List<Transaction>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(trackingService.searchTransactionsWithType(query, type)).thenReturn(repoLiveData);

        viewModel.getFilteredTransactions().observeForever(res -> {});
        viewModel.setTypeFilter(type);
        viewModel.setSearchQuery(query);

        verify(trackingService).searchTransactionsWithType(query, type);
        assertEquals(searchResults, viewModel.getFilteredTransactions().getValue());
    }

    @Test
    public void setTypeFilter_updatesTypeFilterLiveData() {
        viewModel.setTypeFilter(TrackingType.INCOME);
        assertEquals(TrackingType.INCOME, viewModel.getTypeFilter().getValue());
    }

    @Test
    public void filterTransactions_withEmptyQuery_returnsAllTransactions() {
        List<Transaction> allTransactions = new ArrayList<>();
        allTransactions.add(new Transaction.Builder("Grocery", BigDecimal.valueOf(50)).build());

        MutableLiveData<List<Transaction>> allTransactionsLiveData = new MutableLiveData<>();
        allTransactionsLiveData.setValue(allTransactions);

        when(trackingService.getAllTransactions()).thenReturn(allTransactionsLiveData);

        viewModel = new TransactionViewModel(trackingService);
        viewModel.getFilteredTransactions().observeForever(res -> {});
        viewModel.setSearchQuery("");

        assertEquals(allTransactions, viewModel.getFilteredTransactions().getValue());
    }
}
