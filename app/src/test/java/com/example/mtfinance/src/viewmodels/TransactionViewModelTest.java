package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(InstantExecutorExtension.class)
public class TransactionViewModelTest {

    @Mock
    private TransactionService transactionService;

    private TransactionViewModel viewModel;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
        transactionsLiveData.setValue(new ArrayList<>());
        when(transactionService.getAllLive()).thenReturn(transactionsLiveData);

        viewModel = new TransactionViewModel(transactionService);
    }

    @Test
    public void constructor_loadsAllTransactions() {
        verify(transactionService).getAllLive();
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

        when(transactionService.getById(transactionId)).thenReturn(transaction);

        viewModel.setSelectedTransaction(transactionId);

        assertAll(
            () -> verify(transactionService).getById(transactionId),
            () -> assertEquals(transaction, viewModel.getSelectedTransaction().getValue())
        );
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

        when(transactionService.getAllLive()).thenReturn(transactionsLiveData);

        TransactionViewModel newViewModel = new TransactionViewModel(transactionService);

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

        when(transactionService.searchTransactionsWithType(query, null)).thenReturn(repoLiveData);

        viewModel.getFilteredTransactions().observeForever(res -> {});
        viewModel.setSearchQuery(query);

        assertAll(
            () -> verify(transactionService).searchTransactionsWithType(query, null),
            () -> assertEquals(searchResults, viewModel.getFilteredTransactions().getValue())
        );
    }

    @Test
    public void filterTransactions_withQueryAndType_triggersSwitchMap() {
        String query = "Pizza";
        TrackingType type = TrackingType.EXPENSE;
        List<Transaction> searchResults = new ArrayList<>();
        searchResults.add(new Transaction.Builder("Pizza Hut", BigDecimal.valueOf(25)).type(type).build());

        MutableLiveData<List<Transaction>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        when(transactionService.searchTransactionsWithType(query, type)).thenReturn(repoLiveData);

        viewModel.getFilteredTransactions().observeForever(res -> {});
        viewModel.setTypeFilter(type);
        viewModel.setSearchQuery(query);

        assertAll(
            () -> verify(transactionService).searchTransactionsWithType(query, type),
            () -> assertEquals(searchResults, viewModel.getFilteredTransactions().getValue())
        );
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

        when(transactionService.getAllLive()).thenReturn(allTransactionsLiveData);

        viewModel = new TransactionViewModel(transactionService);
        viewModel.getFilteredTransactions().observeForever(res -> {});
        viewModel.setSearchQuery("");

        assertEquals(allTransactions, viewModel.getFilteredTransactions().getValue());
    }
}
