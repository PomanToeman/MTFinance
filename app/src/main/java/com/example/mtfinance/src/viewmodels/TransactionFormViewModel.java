package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Meant to manually enter transactions, Will also be used for the import feature.
 */
@HiltViewModel
public class TransactionFormViewModel extends ViewModel {
    private final TrackingService trackingService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final TrackingRepository trackingWriteRepository;
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
    public TransactionFormViewModel(TrackingService trackingService,
                                    CategoryService categoryService,
                                    TransactionService transactionService,
                                    TrackingRepository trackingWriteRepository,
                                    Executor executor) {
        this.trackingService = trackingService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.trackingWriteRepository = trackingWriteRepository;
        this.executor = executor;

        cachedCategories = Transformations.switchMap(categoryIds, ids -> {
            if (ids == null || ids.isEmpty()) {
                return new MutableLiveData<>(new HashSet<>());
            }
            return new MutableLiveData<>(new HashSet<>(trackingService.getCategoriesWithTransactionsByIds(ids)));
        });
        categorySelection = Transformations.switchMap(type, t -> {
            if (t == null) {
                return trackingService.getAllCategoriesWithTransactions();
            }
            return trackingService.searchCategoriesWithType("", t);
        });

        clearSync();
    }

    /**
     * Sets transaction name field.
     * @param name - transaction name
     */
    public void setName(String name) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.name = name;
            updateFields(fields);
        }
    }

    /**
     * Sets transaction description field.
     * @param description - transaction description
     */
    public void setDescription(String description) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.description = description;
            updateFields(fields);
        }
    }

    /**
     * Adds a category association to the transaction.
     * @param categoryId - category ID
     */
    public void addCategoryId(Long categoryId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.categoryIds.add(categoryId);
            updateFields(fields);
        }
    }

    /**
     * Removes a category association from the transaction.
     * @param categoryId - category ID
     */
    public void removeCategoryId(Long categoryId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.categoryIds.remove(categoryId);
            updateFields(fields);
        }
    }

    /**
     * Sets the transaction monetary amount.
     * @param amount - amount
     */
    public void setAmount(BigDecimal amount) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.amount = amount;
            updateFields(fields);
        }
    }

    /**
     * Sets the tracking type filter.
     * @param type - tracking type
     */
    public void setType(TrackingType type) {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || type == null || Boolean.TRUE.equals(fields.editMode) || fields.type == type) {
            return;
        }

        fields.type = type;
        fields.categoryIds = new HashSet<>();
        updateFields(fields);
    }

    /**
     * Sets transaction date and time.
     * @param date - date and time
     */
    public void setDate(LocalDateTime date) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.date = date;
            updateFields(fields);
        }
    }

    /**
     * Sets transaction date (start of day).
     * @param date - date
     */
    public void setDate(LocalDate date) {
        setDate(date.atStartOfDay());
    }

    /**
     * Sets the transaction ID for editing mode.
     * @param transactionId - transaction ID
     */
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

    private void setIsLoading(boolean isLoading) {
        this.isLoading.postValue(isLoading);
    }

    private void setErrorMessage(String errorMessage) {
        this.errorMessage.postValue(errorMessage);
    }

    private void setSuccessMessage(String successMessage) {
        this.successMessage.postValue(successMessage);
    }

    /**
     * Loads transaction details for editing asynchronously.
     */
    public void loadTransactionForEditing() {
        executor.execute(this::loadTransactionForEditingSync);
    }

    /**
     * Loads transaction details for editing synchronously.
     */
    public void loadTransactionForEditingSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || fields.transactionId == null) return;

        if (transactionService.transactionExists(fields.transactionId)) {
            Transaction transaction = transactionService.getById(fields.transactionId);
            fields.name = transaction.getName();
            fields.description = transaction.getDescription();
            fields.categoryIds = new HashSet<>(trackingService.getCategoryIdsByTransactionId(fields.transactionId));
            fields.amount = transaction.getAmount();
            fields.type = transaction.getType();
            fields.date = transaction.getDate();
            fields.editMode = true;
            formFields.postValue(fields);
        } else {
            setErrorMessage(MessageCli.TRANSACTION_NOT_FOUND.getMessage());
            fields.transactionId = null;
            fields.editMode = false;
            formFields.postValue(fields);
        }
    }

    /**
     * Resets form fields asynchronously.
     */
    public void clear() {
        executor.execute(this::clearSync);
    }

    /**
     * Resets form fields synchronously.
     */
    public void clearSync() {
        TransactionFormFields fields = new TransactionFormFields();
        formFields.postValue(fields);
        setErrorMessage("");
        setSuccessMessage("");
        setIsLoading(false);
    }

    /**
     * Saves transaction form asynchronously.
     */
    public void saveTransaction() {
        executor.execute(this::saveTransactionSync);
    }

    /**
     * Saves transaction form synchronously.
     */
    public void saveTransactionSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null) return;

        try {
            setIsLoading(true);
            validateFormSync(fields);

            if (Boolean.TRUE.equals(fields.editMode)) {
                Transaction transaction = transactionService.getById(fields.transactionId);
                transaction.setDescription(fields.description);
                trackingWriteRepository.updateTransaction(transaction);

                List<Long> oldCategoryIds = trackingService.getCategoryIdsByTransactionId(fields.transactionId);
                Set<Long> newCategoryIds = fields.categoryIds;

                for (Long catId : newCategoryIds) {
                    if (!oldCategoryIds.contains(catId)) {
                        trackingWriteRepository.insertRelationship(fields.transactionId, catId);
                    }
                }

                for (Long catId : oldCategoryIds) {
                    if (!newCategoryIds.contains(catId)) {
                        trackingWriteRepository.deleteRelationship(fields.transactionId, catId);
                    }
                }

                setErrorMessage("");
                setSuccessMessage(MessageCli.TRANSACTION_UPDATED.getMessage());
            } else {
                Transaction newTransaction = new Transaction.Builder(fields.name, fields.amount)
                        .description(fields.description)
                        .type(fields.type)
                        .date(fields.date)
                        .build();

                boolean first = true;
                for (Long catId : fields.categoryIds) {
                    if (first) {
                        trackingWriteRepository.insertTransaction(newTransaction, catId);
                        first = false;
                    } else {
                        trackingWriteRepository.insertRelationship(newTransaction.getTransactionId(), catId);
                    }
                }

                setErrorMessage("");
                setSuccessMessage(MessageCli.TRANSACTION_SAVED.getMessage());
            }
        } catch (Exception e) {
            setErrorMessage(MessageCli.TRANSACTION_SAVE_FAILED.getMessage(e.getMessage()));
            setSuccessMessage("");
        } finally {
            setIsLoading(false);
        }
    }

    /**
     * Deletes selected transaction asynchronously.
     */
    public void deleteTransaction() {
        executor.execute(this::deleteTransactionSync);
    }

    /**
     * Deletes selected transaction synchronously.
     */
    public void deleteTransactionSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || fields.transactionId == null) {
            setErrorMessage(MessageCli.TRANSACTION_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingWriteRepository.deleteTransaction(fields.transactionId);
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
        if (!categoryService.verifyExistingIds(fields.categoryIds)) {
            throw new IllegalArgumentException(MessageCli.TRANSACTION_CATEGORIES_NOT_EXIST.getMessage());
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

    private void updateFields(TransactionFormFields fields) {
        try {
            formFields.setValue(fields);
        } catch (Exception e) {
            formFields.postValue(fields);
        }
    }

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
