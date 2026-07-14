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

/**
 * Meant to manually enter transactions, Will also be used for the import feature.
 * Cannot edit fields (will add features to edit description and categories later).
 */
@HiltViewModel
public class TransactionFormViewModel extends ViewModel {
    // instance fields
    private final TrackingRepository trackingRepository;
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<Long> categoryId = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> amount = new MutableLiveData<>();
    private final MutableLiveData<TrackingType> type = new MutableLiveData<>();
    private final MutableLiveData<LocalDateTime> date = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Long> transactionId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> editMode = new MutableLiveData<>();


    @Inject
    public TransactionFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        clear(); // set default values

    }

    // setters
    public void setName(String name) {
        if (Boolean.FALSE.equals(editMode.getValue())) {
            this.name.setValue(name);
        }

    }
    public void setDescription(String description) {
        this.description.setValue(TrackingUtlis.determineDescription(description));
    }
    public void setCategoryId(Long categoryId) {
        if (Boolean.FALSE.equals(editMode.getValue())) {
            this.categoryId.setValue(categoryId);
        }

    }
    public void setAmount(BigDecimal amount) {
        if (Boolean.FALSE.equals(editMode.getValue())) {
            this.amount.setValue(amount);
        }
    }
    public void setType(TrackingType type) {

        if (type != null && Boolean.FALSE.equals(editMode.getValue())) {
            this.type.setValue(type);
        }
    }
    public void setDate(LocalDateTime date) {
        if (Boolean.FALSE.equals(editMode.getValue())) {
            this.date.setValue(date);
        }
    }
    public void setDate(LocalDate date) {
        if (Boolean.FALSE.equals(editMode.getValue())) {
            this.date.setValue(date.atStartOfDay());
        }
    }
    public void setTransactionId(Long transactionId) {
        this.transactionId.setValue(transactionId);
        loadTransactionForEditing();
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

    private void loadTransactionForEditing() {
        if (trackingRepository.transactionExists(transactionId.getValue())) {
            Transaction transaction = trackingRepository.getTransactionById(transactionId.getValue());
            setName(transaction.getName());
            setDescription(transaction.getDescription());
            setCategoryId(trackingRepository.getCategoryIdsByTransactionId(transactionId.getValue()).get(0));
            setAmount(transaction.getAmount());
            setType(transaction.getType());
            setDate(transaction.getDate());
            editMode.setValue(true);

        }
        else {
            setErrorMessage("Transaction not found.");
            setTransactionId(null);
        }
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
        date.setValue(LocalDateTime.now());
        transactionId.setValue(null);
        editMode.setValue(false);

    }

    /**
     * Forms must be valid before creating a transaction.
     */
    public void saveTransaction() {
        try {
            setIsLoading(true);
            validateForm();
            // Create Transaction
            if (Boolean.TRUE.equals(editMode.getValue())) {
                Transaction transaction = trackingRepository.getTransactionById(transactionId.getValue());
                transaction.setDescription(description.getValue());
                trackingRepository.updateTransaction(transaction);

            }
            else {
                Transaction newTransaction = new Transaction.Builder(name.getValue(), amount.getValue()).description(description.getValue()).type(type.getValue()).date(date.getValue()).build();
                trackingRepository.insertTransaction(newTransaction, categoryId.getValue());
                setErrorMessage("");
                setSuccessMessage("Transaction saved successfully");
            }


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
        if (!trackingRepository.categoryExists(categoryId.getValue())) {
            throw new IllegalArgumentException("Category does not exist.");
        }

        if (Boolean.FALSE.equals(editMode.getValue())) {
            // Check for identical transaction (duplicate detection)
            Transaction dummy = new Transaction.Builder(name.getValue(), amount.getValue())
                    .description(description.getValue())
                    .type(type.getValue())
                    .date(date.getValue())
                    .build();
            if (trackingRepository.transactionHashExists(dummy.getGeneratedHash())) {
                throw new IllegalArgumentException("Identical transaction already exists.");
            }
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
