package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionViewModel extends ViewModel {
    private final TransactionService transactionService;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<Transaction>> filteredTransactions;
    private final MutableLiveData<SearchCriteria> searchCriteria = new MutableLiveData<>(new SearchCriteria("", null));

    private final MutableLiveData<Transaction> selectedTransaction = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoriesUnderSelectedTransaction = new MutableLiveData<>();

    @Inject
    public TransactionViewModel(TransactionService transactionService) {
        this.transactionService = transactionService;
        this.allTransactions = transactionService.getAllLive();

        this.filteredTransactions = Transformations.switchMap(searchCriteria, query -> {
            if (query == null || (query.getQuery() == null || query.getQuery().trim().isEmpty()) && query.getTypeFilter() == null) {
                return allTransactions;
            }
            return transactionService.searchTransactionsWithType(query.getTrimmedQuery(), query.getTypeFilter());
        });
    }

    public void setSelectedTransaction(Long id) {
        Transaction transaction = transactionService.getById(id);
        if (transaction != null) {
            selectedTransaction.setValue(transaction);
        }
    }

    public void setSearchQuery(String query) {
        SearchCriteria criteria = this.searchCriteria.getValue();
        if (criteria == null) {
            criteria = new SearchCriteria("", null);
        }
        criteria.setQuery(query);
        this.searchCriteria.setValue(criteria);
    }

    public LiveData<String> getSearchQuery() {
        return Transformations.map(searchCriteria, criteria -> {
            if (criteria != null) {
                return criteria.getQuery();
            }
            return "";
        });
    }

    public void setTypeFilter(TrackingType typeFilter) {
        SearchCriteria criteria = this.searchCriteria.getValue() != null ? this.searchCriteria.getValue() : new SearchCriteria("", null);
        criteria.setTypeFilter(typeFilter);
        this.searchCriteria.setValue(criteria);
    }

    public LiveData<TrackingType> getTypeFilter() {
        return Transformations.map(searchCriteria, criteria -> {
            if (criteria != null) {
                return criteria.getTypeFilter();
            }
            return null;
        });
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public LiveData<Transaction> getSelectedTransaction() {
        return selectedTransaction;
    }

    public LiveData<List<Category>> getCategoriesUnderSelectedTransaction() {
        return categoriesUnderSelectedTransaction;
    }

    public void resetSelectedTransaction() {
        selectedTransaction.setValue(null);
        categoriesUnderSelectedTransaction.setValue(null);
    }
}
