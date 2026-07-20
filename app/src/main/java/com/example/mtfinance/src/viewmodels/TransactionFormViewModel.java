package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Meant to manually enter transactions, Will also be used for the import feature.
 */
@HiltViewModel
public class TransactionFormViewModel extends ViewModel {
    // instance fields
    private final TrackingRepository trackingRepository;
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<Set<Long>> categoryIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<BigDecimal> amount = new MutableLiveData<>();
    private final MutableLiveData<TrackingType> type = new MutableLiveData<>();
    private final MutableLiveData<LocalDateTime> date = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Long> transactionId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> editMode = new MutableLiveData<>(false);
    private final LiveData<Set<CategoryWithTransactions>> cachedCategories;


    @Inject
    public TransactionFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        cachedCategories = Transformations.switchMap(categoryIds, ids -> {
            if (ids == null || ids.isEmpty()) {
                return new MutableLiveData<>(new HashSet<>());
            }
            return new MutableLiveData<>(new HashSet<>(trackingRepository.getCategoriesWithTransactionsByIds(ids)));
        });

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
    public void addCategoryId(Long categoryId) {
       Set<Long> currentIds = this.categoryIds.getValue();
       if (currentIds == null) {
           currentIds = new HashSet<>();
       }
       currentIds.add(categoryId);
       this.categoryIds.setValue(currentIds);

    }

    public void removeCategoryId(Long categoryId) {
        Set<Long> currentIds = this.categoryIds.getValue();
        if (currentIds == null) {
            return;
        }
        currentIds.remove(categoryId);
        this.categoryIds.setValue(currentIds);
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
        if (transactionId != null) {
            loadTransactionForEditing();
        }
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
            name.setValue(transaction.getName());
            description.setValue(transaction.getDescription());
            
            // Fix: Load ALL category IDs
            Set<Long> ids = new HashSet<>(trackingRepository.getCategoryIdsByTransactionId(transactionId.getValue()));
            categoryIds.setValue(ids);
            
            amount.setValue(transaction.getAmount());
            type.setValue(transaction.getType());
            date.setValue(transaction.getDate());
            editMode.setValue(true);
        }
        else {
            setErrorMessage(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
            transactionId.setValue(null);
            editMode.setValue(false);
        }
    }


    /**
     * resets to default values.
     */
    public void clear() {
        name.setValue("");
        description.setValue("");
        categoryIds.setValue(new HashSet<>());
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


            if (Boolean.TRUE.equals(editMode.getValue())) {
                Transaction transaction = trackingRepository.getTransactionById(transactionId.getValue());
                transaction.setDescription(description.getValue());
                trackingRepository.updateTransaction(transaction);

                List<Long> oldCategoryIds = trackingRepository.getCategoryIdsByTransactionId(transactionId.getValue());
                Set<Long> newCategoryIds = categoryIds.getValue();

                // Add new relationships
                for (Long catId : newCategoryIds) {
                    if (!oldCategoryIds.contains(catId)) {
                        trackingRepository.insertRelationship(transactionId.getValue(), catId);
                    }
                }

                // Remove old relationships
                for (Long catId : oldCategoryIds) {
                    if (!newCategoryIds.contains(catId)) {
                        trackingRepository.deleteRelationship(transactionId.getValue(), catId);
                    }
                }

                setErrorMessage("");
                setSuccessMessage(MessageCli.TRANSACTION_UPDATED.getMessage());

            }
            else {
                Transaction newTransaction = new Transaction.Builder(name.getValue(), amount.getValue())
                        .description(description.getValue())
                        .type(type.getValue())
                        .date(date.getValue())
                        .build();


                boolean first = true;
                for (Long catId : categoryIds.getValue()) {
                    if (first) {
                        trackingRepository.insertTransaction(newTransaction, catId);
                        first = false;
                    } else {
                        trackingRepository.insertRelationship(newTransaction.getTransactionId(), catId);
                    }
                }

                setErrorMessage("");
                setSuccessMessage(MessageCli.TRANSACTION_SAVED.getMessage());
            }


        }
        catch (Exception e) {
            setErrorMessage(MessageCli.TRANSACTION_SAVE_FAILED.getMessage(e.getMessage()));
            setSuccessMessage("");
        }
        finally {
            setIsLoading(false);
            }
    }

    public void deleteTransaction() {
        if (transactionId.getValue() == null) {
            setErrorMessage(MessageCli.TRANSACTION_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingRepository.deleteTransaction(transactionId.getValue());
            clear();
            setSuccessMessage(MessageCli.TRANSACTION_DELETED.getMessage());
        } catch (Exception e) {
            setErrorMessage(MessageCli.TRANSACTION_DELETE_FAILED.getMessage(e.getMessage()));
        } finally {
            setIsLoading(false);
        }
    }

    private void validateForm() throws IllegalArgumentException {
        if (name.getValue() == null || name.getValue().isEmpty()) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_NAME_EMPTY.getMessage());
        }
        TrackingUtlis.checkAmount(amount.getValue());
        if (categoryIds.getValue() == null || categoryIds.getValue().isEmpty()) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_CATEGORY_EMPTY.getMessage());
        }
        if (!trackingRepository.verifyExistingIdsCategories(categoryIds.getValue())) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_CATEGORIES_NOT_EXIST.getMessage());
        }

        if (Boolean.FALSE.equals(editMode.getValue())) {
            // Check for identical transaction (duplicate detection)
            Transaction dummy = new Transaction.Builder(name.getValue(), amount.getValue())
                    .description(description.getValue())
                    .type(type.getValue())
                    .date(date.getValue())
                    .build();
            if (trackingRepository.transactionHashExists(dummy.getGeneratedHash())) {
                throw new IllegalArgumentException(MessageCli.TRANSACTION_DUPLICATE.getMessage());
            }
        }

    }



    // public getters
    public LiveData<String> getName() { return name; }
    public LiveData<String> getDescription() { return description; }
    public LiveData<Set<Long>> getCategoryIds() { return categoryIds; }
    public LiveData<BigDecimal> getAmount() { return amount; }
    public LiveData<TrackingType> getType() { return type; }
    public LiveData<LocalDateTime> getDate() { return date; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getEditMode() { return editMode; }
    public LiveData<Set<CategoryWithTransactions>> getCachedCategories() { return cachedCategories; }
}
