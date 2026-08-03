package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;


import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<Transaction>> filteredTransactions;
    private final MutableLiveData<SearchCriteria> searchCriteria = new MutableLiveData<>(new SearchCriteria("", null));

    private final MutableLiveData<Transaction> selectedTransaction = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoriesUnderSelectedTransaction = new MutableLiveData<>();
    @Inject
    public TransactionViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        this.allTransactions = trackingRepository.getAllTransactions();

        // to automatically switch sources whenever searchQuery changes.
        this.filteredTransactions = Transformations.switchMap(searchCriteria, query -> {
            if (query == null || (query.getQuery() == null || query.getQuery().trim().isEmpty()) && query.getTypeFilter() == null) {
                return allTransactions;
            }
            return trackingRepository.searchTransactionsWithType(query.getTrimmedQuery(), query.getTypeFilter());
        });


    }

    /**
     * To show more defined data of a specific transaction.
     * @param id - the ID of the transaction, must already be within the database.
     */
    public void setSelectedTransaction(Long id) {
        Transaction transaction = trackingRepository.getTransactionById(id);
        if (transaction != null) {
            selectedTransaction.setValue(transaction);
            categoriesUnderSelectedTransaction.setValue(trackingRepository.getCategoriesByTransactionId(id));
        }

    }

    /**
     * This automatically changes the filteredTransactions.
     * null or empty query will return all transactions.
     * @param query - the query to search for.
     */
    public void setSearchQuery(String query) {
        SearchCriteria searchCriteria = this.searchCriteria.getValue();
        searchCriteria.setQuery(query);

        this.searchCriteria.setValue(searchCriteria);

    }

    public LiveData<String> getSearchQuery() {
        return Transformations.map(searchCriteria, searchCriteria ->{
            if (searchCriteria != null) {
                return searchCriteria.getQuery();
            }
            return "";
        });

    }

    public void setTypeFilter(TrackingType typeFilter) {
        SearchCriteria searchCriteria = this.searchCriteria.getValue() != null ? this.searchCriteria.getValue() : new SearchCriteria("", null);
        searchCriteria.setTypeFilter(typeFilter);
        this.searchCriteria.setValue(searchCriteria);

    }

    public LiveData<TrackingType> getTypeFilter() {
        return Transformations.map(searchCriteria, searchCriteria ->{
            if (searchCriteria != null) {
                return searchCriteria.getTypeFilter();
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
