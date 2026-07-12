package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();
    private final MutableLiveData<Transaction> selectedTransaction = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoriesUnderSelectedTransaction = new MutableLiveData<>();
    @Inject
    public TransactionViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        loadTransactions();
    }

    private void loadTransactions() {
        transactions.setValue(trackingRepository.getAllTransactions());
    }

    public void setSelectedTransaction(Long id) {
        Transaction transaction = trackingRepository.getTransactionById(id);
        if (transaction != null) {
            selectedTransaction.setValue(transaction);
            categoriesUnderSelectedTransaction.setValue(trackingRepository.findCategoriesByTransactionId(id));
        }

    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    public LiveData<Transaction> getSelectedTransaction() {
        return selectedTransaction;
    }

    public LiveData<List<Category>> getCategoriesUnderSelectedTransaction() {
        return categoriesUnderSelectedTransaction;
    }



}
