package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;


import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<Transaction>> filteredTransactions;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Transaction> selectedTransaction = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoriesUnderSelectedTransaction = new MutableLiveData<>();
    @Inject
    public TransactionViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        this.allTransactions = trackingRepository.getAllTransactions();

        // to automatically switch sources whenever searchQuery changes.
        this.filteredTransactions = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return allTransactions;
            }
            return trackingRepository.searchTransactions(query);
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
            categoriesUnderSelectedTransaction.setValue(trackingRepository.findCategoriesByTransactionId(id));
        }

    }
    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query.trim() : "");
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
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



}
