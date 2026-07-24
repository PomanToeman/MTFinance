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
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Meant to manually enter transactions, Will also be used for the import feature.
 */
@HiltViewModel
public class TransactionFormViewModel extends ViewModel {
    // instance fields
    private final TrackingRepository trackingRepository;
    private final Executor executor;

    private final MutableLiveData<TransactionFormFields> formFields = new MutableLiveData<>(new TransactionFormFields());
    
    private final LiveData<String> name = Transformations.map(formFields, fields -> fields.name);
    private final LiveData<String> description = Transformations.map(formFields, fields -> fields.description);
    private final LiveData<Set<Long>> categoryIds = Transformations.map(formFields, fields -> fields.categoryIds);
    private final LiveData<BigDecimal> amount = Transformations.map(formFields, fields -> fields.amount);
    private final LiveData<TrackingType> type = Transformations.map(formFields, fields -> fields.type);
    private final LiveData<LocalDateTime> date = Transformations.map(formFields, fields -> fields.date);
    private final LiveData<Long> transactionId = Transformations.map(formFields, fields -> fields.transactionId);
    private final LiveData<Boolean> editMode = Transformations.map(formFields, fields -> fields.editMode);
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");
    
    private final LiveData<Set<CategoryWithTransactions>> cachedCategories;
    private final LiveData<List<CategoryWithTransactions>> categorySelection;


    @Inject
    public TransactionFormViewModel(TrackingRepository trackingRepository, Executor executor) {
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        
        cachedCategories = Transformations.switchMap(categoryIds, ids -> {
            if (ids == null || ids.isEmpty()) {
                return new MutableLiveData<>(new HashSet<>());
            }
            return new MutableLiveData<>(new HashSet<>(trackingRepository.getCategoriesWithTransactionsByIds(ids)));
        });
        categorySelection = Transformations.switchMap(type , type -> {
            if (type == null) {
                return trackingRepository.getAllCategoriesWithTransactions();
            }
            return trackingRepository.searchCategoriesWithType("", type);
        });

        clearSync(); // set default values

    }

    // setters
    public void setName(String name) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.name = name;
            formFields.setValue(fields);
        }

    }
    public void setDescription(String description) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.description = TrackingUtlis.determineDescription(description);
            formFields.setValue(fields);
        }
    }
    public void addCategoryId(Long categoryId) {
       TransactionFormFields fields = formFields.getValue();
       if (fields != null) {
           fields.categoryIds.add(categoryId);
           formFields.setValue(fields);
       }
    }

    public void removeCategoryId(Long categoryId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.categoryIds.remove(categoryId);
            formFields.setValue(fields);
        }
    }
    public void setAmount(BigDecimal amount) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.amount = amount;
            formFields.setValue(fields);
        }
    }

    /**
     * Sets the type of the transaction.
     * Please set your type before setting category IDs, as the categories and transactions must be the same type.
     * Will clear category IDs if type is changed.
     * @param type
     */
    public void setType(TrackingType type) {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || type == null || Boolean.TRUE.equals(fields.editMode) || fields.type == type) {
            return;
        }

        fields.type = type;
        fields.categoryIds = new HashSet<>();
        formFields.setValue(fields);
    }
    public void setDate(LocalDateTime date) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.date = date;
            formFields.setValue(fields);
        }
    }
    public void setDate(LocalDate date) {
        setDate(date.atStartOfDay());
    }
    public void setTransactionId(Long transactionId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.transactionId = transactionId;
            formFields.setValue(fields);
            if (transactionId != null) {
                loadTransactionForEditing();
            }
        }
    }


    // private setters
    private void setIsLoading(boolean isLoading) {
        this.isLoading.postValue(isLoading);
    }
    private void setErrorMessage(String errorMessage) {
        this.errorMessage.postValue(errorMessage);
    }
    private void setSuccessMessage(String successMessage) {
        this.successMessage.postValue(successMessage);
    }

    public void loadTransactionForEditing() {
        executor.execute(this::loadTransactionForEditingSync);
    }

    public void loadTransactionForEditingSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || fields.transactionId == null) return;

        if (trackingRepository.transactionExists(fields.transactionId)) {
            Transaction transaction = trackingRepository.getTransactionById(fields.transactionId);
            fields.name = transaction.getName();
            fields.description = transaction.getDescription();
            fields.categoryIds = new HashSet<>(trackingRepository.getCategoryIdsByTransactionId(fields.transactionId));
            fields.amount = transaction.getAmount();
            fields.type = transaction.getType();
            fields.date = transaction.getDate();
            fields.editMode = true;
            formFields.postValue(fields);
        }
        else {
            setErrorMessage(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
            fields.transactionId = null;
            fields.editMode = false;
            formFields.postValue(fields);
        }
    }


    /**
     * resets to default values.
     */
    public void clear() {
        executor.execute(this::clearSync);
    }

    public void clearSync() {
        TransactionFormFields fields = new TransactionFormFields();
        formFields.postValue(fields);
        setErrorMessage("");
        setSuccessMessage("");
        setIsLoading(false);
    }

    /**
     * Forms must be valid before creating a transaction.
     */
    public void saveTransaction() {
        executor.execute(this::saveTransactionSync);
    }

    public void saveTransactionSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null) return;

        try {
            setIsLoading(true);

            validateFormSync(fields);

            if (Boolean.TRUE.equals(fields.editMode)) {
                Transaction transaction = trackingRepository.getTransactionById(fields.transactionId);
                transaction.setDescription(fields.description);
                trackingRepository.updateTransaction(transaction);

                List<Long> oldCategoryIds = trackingRepository.getCategoryIdsByTransactionId(fields.transactionId);
                Set<Long> newCategoryIds = fields.categoryIds;

                // Add new relationships
                for (Long catId : newCategoryIds) {
                    if (!oldCategoryIds.contains(catId)) {
                        trackingRepository.insertRelationship(fields.transactionId, catId);
                    }
                }

                // Remove old relationships
                for (Long catId : oldCategoryIds) {
                    if (!newCategoryIds.contains(catId)) {
                        trackingRepository.deleteRelationship(fields.transactionId, catId);
                    }
                }

                setErrorMessage("");
                setSuccessMessage(MessageCli.TRANSACTION_UPDATED.getMessage());

            }
            else {
                Transaction newTransaction = new Transaction.Builder(fields.name, fields.amount)
                        .description(fields.description)
                        .type(fields.type)
                        .date(fields.date)
                        .build();


                boolean first = true;
                for (Long catId : fields.categoryIds) {
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
        executor.execute(this::deleteTransactionSync);
    }

    public void deleteTransactionSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || fields.transactionId == null) {
            setErrorMessage(MessageCli.TRANSACTION_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingRepository.deleteTransaction(fields.transactionId);
            clearSync();
            setSuccessMessage(MessageCli.TRANSACTION_DELETED.getMessage());
        } catch (Exception e) {
            setErrorMessage(MessageCli.TRANSACTION_DELETE_FAILED.getMessage(e.getMessage()));
        } finally {
            setIsLoading(false);
        }
    }

    private void validateFormSync(TransactionFormFields fields) throws IllegalArgumentException {
        if (fields.name == null || fields.name.isEmpty()) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_NAME_EMPTY.getMessage());
        }
        TrackingUtlis.checkAmount(fields.amount);
        if (fields.categoryIds == null || fields.categoryIds.isEmpty()) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_CATEGORY_EMPTY.getMessage());
        }
        if (!trackingRepository.verifyExistingIdsCategories(fields.categoryIds)) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_CATEGORIES_NOT_EXIST.getMessage());
        }

        if (Boolean.FALSE.equals(fields.editMode)) {
            // Check for identical transaction (duplicate detection)
            Transaction dummy = new Transaction.Builder(fields.name, fields.amount)
                    .description(fields.description)
                    .type(fields.type)
                    .date(fields.date)
                    .build();
            if (trackingRepository.transactionHashExists(dummy.getGeneratedHash())) {
                throw new IllegalArgumentException(MessageCli.TRANSACTION_DUPLICATE.getMessage());
            }
        }

    }

    private static class TransactionFormFields {
        String name = "";
        String description = "";
        Set<Long> categoryIds = new HashSet<>();
        BigDecimal amount = BigDecimal.ZERO;
        TrackingType type = TrackingType.EXPENSE;
        LocalDateTime date = LocalDateTime.now();
        Long transactionId = null;
        Boolean editMode = false;
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
    public LiveData<List<CategoryWithTransactions>> getCategorySelection() { return categorySelection; }
}
