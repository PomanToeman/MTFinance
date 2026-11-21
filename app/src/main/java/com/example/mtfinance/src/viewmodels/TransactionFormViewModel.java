package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Meant to manually enter transactions, Will also be used for the import feature.
 */
@HiltViewModel
public class TransactionFormViewModel extends ViewModel {

    public enum TransactionErrorFields {
        NAME, DESCRIPTION, CATEGORY_IDS, AMOUNT, TYPE, DATE
    }

    private final TrackingService trackingService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final TrackingRepository trackingRepository;
    private final SystemService systemService;
    private final Executor executor;

    private final MutableLiveData<TransactionFormFields> formFields = new MutableLiveData<>(new TransactionFormFields());
    private final MutableLiveData<Map<TransactionErrorFields, String>> fieldErrors = new MutableLiveData<>(new EnumMap<>(TransactionErrorFields.class));

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

    private final MutableLiveData<Set<CategoryWithTransactions>> cachedCategories = new MutableLiveData<>(new HashSet<>());
    private final LiveData<List<CategoryWithTransactions>> categorySelection;
    private final LiveData<DateFormat> systemDateFormat;

    @Inject
    public TransactionFormViewModel(TrackingService trackingService,
                                    CategoryService categoryService,
                                    TransactionService transactionService,
                                    TrackingRepository trackingRepository,
                                    SystemService systemService,
                                    Executor executor) {
        this.trackingService = trackingService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.trackingRepository = trackingRepository;
        this.systemService = systemService;
        this.executor = executor;

        if (systemService != null) {
            this.systemDateFormat = Transformations.map(systemService.getPreferencesLive(), prefs -> {
                if (prefs == null || prefs.getDateFormat() == null) {
                    return DateFormat.DD_MM_YYYY;
                }
                try {
                    return DateFormat.valueOf(prefs.getDateFormat());
                } catch (Exception e) {
                    return DateFormat.DD_MM_YYYY;
                }
            });
        } else {
            this.systemDateFormat = new MutableLiveData<>(DateFormat.DD_MM_YYYY);
        }

        categorySelection = Transformations.switchMap(type, t -> {
            if (t == null) {
                return trackingService.getAllCategoriesWithTransactions();
            }
            return trackingService.searchCategoriesWithType("", t);
        });

        clearSync();
    }

    /**
     * Overloaded constructor for callers and tests without SystemService.
     */
    public TransactionFormViewModel(TrackingService trackingService,
                                    CategoryService categoryService,
                                    TransactionService transactionService,
                                    TrackingRepository trackingRepository,
                                    Executor executor) {
        this(trackingService, categoryService, transactionService, trackingRepository, null, executor);
    }

    /**
     * Resets all field-specific error messages in the map.
     */
    public void resetFieldErrors() {
        Map<TransactionErrorFields, String> errors = new EnumMap<>(TransactionErrorFields.class);
        for (TransactionErrorFields field : TransactionErrorFields.values()) {
            errors.put(field, "");
        }
        updateLiveData(this.fieldErrors, errors);
    }

    /**
     * Checks if any field error exists in the current LiveData error map.
     * @return true if at least one field has a non-empty error message
     */
    public boolean hasFieldErrors() {
        return hasFieldErrors(fieldErrors.getValue());
    }

    /**
     * Checks if any field error exists in the given error map.
     * @param errors the error map to check
     * @return true if at least one field has a non-empty error message
     */
    public boolean hasFieldErrors(Map<TransactionErrorFields, String> errors) {
        if (errors == null || errors.isEmpty()) return false;
        for (String error : errors.values()) {
            if (error != null && !error.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setFieldError(TransactionErrorFields field, String message) {
        Map<TransactionErrorFields, String> currentMap = fieldErrors.getValue();
        Map<TransactionErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(TransactionErrorFields.class);
        newMap.put(field, message != null ? message : "");
        updateLiveData(this.fieldErrors, newMap);
    }

    /**
     * Sets transaction name field.
     * @param name - transaction name
     */
    public void setName(String name) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.name = name != null ? name : "";
            updateFields(fields);
            if (fields.name.trim().isEmpty()) {
                setFieldError(TransactionErrorFields.NAME, MessageCli.TRANSACTION_NAME_EMPTY.getMessage());
            } else {
                setFieldError(TransactionErrorFields.NAME, "");
            }
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
            setFieldError(TransactionErrorFields.DESCRIPTION, "");
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
            setFieldError(TransactionErrorFields.CATEGORY_IDS, "");
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
            if (fields.categoryIds.isEmpty()) {
                setFieldError(TransactionErrorFields.CATEGORY_IDS, MessageCli.TRANSACTION_CATEGORY_EMPTY.getMessage());
            }
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
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                setFieldError(TransactionErrorFields.AMOUNT, MessageCli.INVALID_AMOUNT.getMessage(amount));
            } else {
                setFieldError(TransactionErrorFields.AMOUNT, "");
            }
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
        setFieldError(TransactionErrorFields.TYPE, "");
    }

    /**
     * Sets transaction date from text input, checking strictly against ISO (yyyy-MM-dd) and dd/MM/yyyy formats.
     * @param dateStr raw date string input
     */
    public void setDateText(String dateStr) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                fields.date = null;
                updateFields(fields);
                setFieldError(TransactionErrorFields.DATE, "Date cannot be empty.");
                return;
            }
            LocalDateTime parsed = DateFormat.parseSimple(dateStr);
            if (parsed != null) {
                fields.date = parsed;
                updateFields(fields);
                setFieldError(TransactionErrorFields.DATE, "");
            } else {
                fields.date = null;
                updateFields(fields);
                setFieldError(TransactionErrorFields.DATE, "Invalid date format. Accepted formats: yyyy-MM-dd or dd/MM/yyyy");
            }
        }
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
            if (date == null) {
                setFieldError(TransactionErrorFields.DATE, "Invalid date format. Accepted formats: yyyy-MM-dd or dd/MM/yyyy");
            } else {
                setFieldError(TransactionErrorFields.DATE, "");
            }
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
        resetFieldErrors();
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
            Map<TransactionErrorFields, String> errors = validateFormSync(fields);

            if (hasFieldErrors(errors)) {
                setErrorMessage(MessageCli.FORM_INCOMPLETE.getMessage("Please fix transaction form field errors"));
                setSuccessMessage("");
                return;
            }

            setErrorMessage("");
            setSuccessMessage("");
            setIsLoading(true);

            if (Boolean.TRUE.equals(fields.editMode)) {
                Transaction transaction = transactionService.getById(fields.transactionId);
                transaction.setDescription(fields.description);
                trackingRepository.updateTransaction(transaction);

                List<Long> oldCategoryIds = trackingService.getCategoryIdsByTransactionId(fields.transactionId);
                Set<Long> newCategoryIds = fields.categoryIds;

                for (Long catId : newCategoryIds) {
                    if (!oldCategoryIds.contains(catId)) {
                        trackingRepository.insertRelationship(fields.transactionId, catId);
                    }
                }

                for (Long catId : oldCategoryIds) {
                    if (!newCategoryIds.contains(catId)) {
                        trackingRepository.deleteRelationship(fields.transactionId, catId);
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
                        trackingRepository.insertTransaction(newTransaction, catId);
                        first = false;
                    } else {
                        trackingRepository.insertRelationship(newTransaction.getTransactionId(), catId);
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
            trackingRepository.deleteTransaction(fields.transactionId);
            clearSync();
            setSuccessMessage(MessageCli.TRANSACTION_DELETED.getMessage());
        } catch (Exception e) {
            setErrorMessage(MessageCli.TRANSACTION_DELETE_FAILED.getMessage(e.getMessage()));
        } finally {
            setIsLoading(false);
        }
    }

    private Map<TransactionErrorFields, String> validateFormSync(TransactionFormFields fields) {
        Map<TransactionErrorFields, String> errors = new EnumMap<>(TransactionErrorFields.class);
        for (TransactionErrorFields field : TransactionErrorFields.values()) {
            errors.put(field, "");
        }

        if (fields.name == null || fields.name.trim().isEmpty()) {
            errors.put(TransactionErrorFields.NAME, MessageCli.TRANSACTION_NAME_EMPTY.getMessage());
        }

        if (fields.amount == null || fields.amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.put(TransactionErrorFields.AMOUNT, MessageCli.INVALID_AMOUNT.getMessage(fields.amount));
        }

        if (fields.categoryIds == null || fields.categoryIds.isEmpty()) {
            errors.put(TransactionErrorFields.CATEGORY_IDS, MessageCli.TRANSACTION_CATEGORY_EMPTY.getMessage());
        } else if (!categoryService.verifyExistingIds(fields.categoryIds)) {
            errors.put(TransactionErrorFields.CATEGORY_IDS, MessageCli.TRANSACTION_CATEGORIES_NOT_EXIST.getMessage());
        }

        if (fields.date == null) {
            errors.put(TransactionErrorFields.DATE, "Date cannot be empty or invalid.");
        }

        updateLiveData(this.fieldErrors, errors);
        return errors;
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
        updateLiveData(formFields, fields);
        if (executor != null) {
            executor.execute(() -> {
                Set<Long> ids = fields.categoryIds;
                Set<CategoryWithTransactions> categories;
                if (ids == null || ids.isEmpty()) {
                    categories = new HashSet<>();
                } else {
                    categories = new HashSet<>(trackingService.getCategoriesWithTransactionsByIds(ids));
                }
                updateLiveData(cachedCategories, categories);
            });
        } else {
            Set<Long> ids = fields.categoryIds;
            Set<CategoryWithTransactions> categories;
            if (ids == null || ids.isEmpty()) {
                categories = new HashSet<>();
            } else {
                categories = new HashSet<>(trackingService.getCategoriesWithTransactionsByIds(ids));
            }
            updateLiveData(cachedCategories, categories);
        }
    }

    private <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            liveData.setValue(value);
        } catch (Exception e) {
            liveData.postValue(value);
        }
    }

    public LiveData<String> getName() { return name; }
    public LiveData<String> getDescription() { return description; }
    public LiveData<Set<Long>> getCategoryIds() { return categoryIds; }
    public LiveData<BigDecimal> getAmount() { return amount; }
    public LiveData<TrackingType> getType() { return type; }
    public LiveData<LocalDateTime> getDate() { return date; }
    public LiveData<DateFormat> getSystemDateFormat() { return systemDateFormat; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getEditMode() { return editMode; }
    public LiveData<Set<CategoryWithTransactions>> getCachedCategories() { return cachedCategories; }
    public LiveData<List<CategoryWithTransactions>> getCategorySelection() { return categorySelection; }
    public LiveData<Map<TransactionErrorFields, String>> getFieldErrors() { return fieldErrors; }
}
