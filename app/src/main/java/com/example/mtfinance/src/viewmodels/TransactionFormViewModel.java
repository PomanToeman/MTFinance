package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionFormViewModel extends ViewModel {
    // instance fields
    private final TrackingRepository trackingRepository;
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<Long> categoryId = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> amount = new MutableLiveData<>();
    private final MutableLiveData<TrackingType> type = new MutableLiveData<>();
    private MutableLiveData<LocalDateTime> date = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();


    @Inject
    public TransactionFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;

    }

    // setters
    public void setName(String name) {
        this.name.setValue(name);
    }
    public void setDescription(String description) {
        this.description.setValue(TrackingUtlis.determineDescription(description));
    }
    public void setCategoryId(Long categoryId) {
        this.categoryId.setValue(categoryId);
    }
    public void setAmount(BigDecimal amount) {
        this.amount.setValue(amount);
    }
    public void setType(TrackingType type) {
        if (type != null) {
            this.type.setValue(type);
        }
    }
    public void setDate(LocalDateTime date) {
        this.date.setValue(date);
    }
    public void setDate(LocalDate date) {
        this.date.setValue(date.atStartOfDay());
    }


    // private setters
    private void setIsLoading(boolean isLoading) {
        this.isLoading.setValue(isLoading);
    }
    private void setErrorMessage(String errorMessage) {
        this.errorMessage.setValue(errorMessage);
    }
    private void setSuccessMessage(String successMessage) {
        this.successMessage.setValue(successMessage);
    }


    /**
     * resets to default values.
     */
    public void clear() {
        name.setValue("");
        description.setValue("");
        categoryId.setValue(null);
        amount.setValue(BigDecimal.ZERO);
        type.setValue(TrackingType.EXPENSE);
        errorMessage.setValue("");
        successMessage.setValue("");
        isLoading.setValue(false);

    }

    public void saveTransaction() {
        try {
            setIsLoading(true);
            validateForm();
            // Create Transaction
            Transaction newTransaction = new Transaction.Builder(name.getValue(), amount.getValue()).description(description.getValue()).type(type.getValue()).date(date.getValue()).build();
            trackingRepository.insertTransaction(newTransaction, categoryId.getValue());
            setErrorMessage("");
            setSuccessMessage("Transaction saved successfully");
            clear();
        }
        catch (Exception e) {
            setErrorMessage("Cannot save transaction: " + e.getMessage());
            setSuccessMessage("");
        }
        finally {
            setIsLoading(false);
            }
    }

    private void validateForm() throws IllegalArgumentException{
        if (name.getValue() == null || name.getValue().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        TrackingUtlis.checkAmount(amount.getValue());
        if (categoryId.getValue() == null) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }
        if (trackingRepository.getCategoryByIdRestored(categoryId.getValue()) == null) {
            throw new IllegalArgumentException("Category does not exist.");
        }


    }

    // public getters

    public LiveData<String> getName() {
        return name;
    }
    public LiveData<String> getDescription() {
        return description;
    }
    public LiveData<Long> getCategoryId() {
        return categoryId;
    }
    public LiveData<BigDecimal> getAmount() {
        return amount;
    }
    public LiveData<TrackingType> getType() {
        return type;
    }
    public LiveData<LocalDateTime> getDate() {
        return date;
    }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }








}
