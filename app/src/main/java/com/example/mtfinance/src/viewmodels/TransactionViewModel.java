package com.example.mtfinance.src.viewmodels;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.viewmodels.utlis.SearchCriteria;

import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionViewModel extends ViewModel {
    private final TransactionService transactionService;
    private final TrackingService trackingService;
    private final CategoryService categoryService;
    private final Executor executor;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<Transaction>> filteredTransactions;
    private final MutableLiveData<SearchCriteria> searchCriteria = new MutableLiveData<>(new SearchCriteria("", null));

    private final MutableLiveData<Transaction> selectedTransaction = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoriesUnderSelectedTransaction = new MutableLiveData<>();

    @Inject
    public TransactionViewModel(TransactionService transactionService, TrackingService trackingService, CategoryService categoryService, Executor executor) {
        this.transactionService = transactionService;
        this.trackingService = trackingService;
        this.categoryService = categoryService;
        this.executor = executor;
        this.allTransactions = transactionService.getAllLive();

        this.filteredTransactions = Transformations.switchMap(searchCriteria, query -> {
            if (query == null || (query.getQuery() == null || query.getQuery().trim().isEmpty()) && query.getTypeFilter() == null) {
                return allTransactions;
            }
            return transactionService.searchTransactionsWithType(query.getTrimmedQuery(), query.getTypeFilter());
        });
    }

    public TransactionViewModel(TransactionService transactionService) {
        this(transactionService, null, null, Runnable::run);
    }

    public void setSelectedTransaction(Long id) {
        if (executor != null) {
            executor.execute(() -> loadSelectedTransaction(id));
        } else {
            loadSelectedTransaction(id);
        }
    }

    private void loadSelectedTransaction(Long id) {
        Transaction transaction = transactionService.getById(id);
        if (transaction != null) {
            updateLiveData(selectedTransaction, transaction);
            if (trackingService != null && categoryService != null) {
                List<Long> catIds = trackingService.getCategoryIdsByTransactionId(id);
                List<Category> cats = categoryService.getCategoriesByIds(catIds);
                updateLiveData(categoriesUnderSelectedTransaction, cats);
            }
        }
    }

    private <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                liveData.setValue(value);
            } else {
                liveData.postValue(value);
            }
        } catch (Exception e) {
            liveData.postValue(value);
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
