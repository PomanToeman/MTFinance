package com.example.mtfinance.src.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.messages.ErrorMessages;
import com.example.mtfinance.src.messages.TransactionMessages;
import com.example.mtfinance.src.messages.GeneralMessages;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import com.example.mtfinance.src.viewmodels.interfaces.DeletableFormViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Modernized, robust ViewModel managing create, edit, delete, and validation lifecycles for Transactions.
 */
@HiltViewModel
public class TransactionFormViewModel extends ViewModel implements DeletableFormViewModel<TransactionFormViewModel.TransactionErrorFields> {

    public enum TransactionErrorFields {
        NAME, DESCRIPTION, AMOUNT, DATE, TYPE, CATEGORY_IDS
    }

    public static class SaveResult {
        private final boolean success;
        private final String message;

        private SaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static SaveResult success(String message) {
            return new SaveResult(true, message);
        }

        public static SaveResult failure(String message) {
            return new SaveResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class TransactionFormFields {
        public Long transactionId = null;
        public String name = "";
        public String description = "";
        public BigDecimal amount = null;
        public TrackingType type = TrackingType.EXPENSE;
        public LocalDateTime date = LocalDateTime.now();
        public Set<Long> categoryIds = new HashSet<>();
        public Boolean editMode = false;
    }

    private final TrackingService trackingService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final TrackingRepository trackingRepository;
    private final SystemService systemService;
    private final Executor executor;
    private final Logger logger;

    private final MutableLiveData<TransactionFormFields> formFields = new MutableLiveData<>(new TransactionFormFields());
    private final MutableLiveData<Map<TransactionErrorFields, String>> fieldErrors = new MutableLiveData<>(new EnumMap<>(TransactionErrorFields.class));
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> successMessage = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final LiveData<String> name = Transformations.map(formFields, fields -> fields.name);
    private final LiveData<String> description = Transformations.map(formFields, fields -> fields.description);
    private final LiveData<BigDecimal> amount = Transformations.map(formFields, fields -> fields.amount);
    private final LiveData<TrackingType> type = Transformations.map(formFields, fields -> fields.type);
    private final LiveData<LocalDateTime> date = Transformations.map(formFields, fields -> fields.date);
    private final LiveData<Set<Long>> categoryIds = Transformations.map(formFields, fields -> fields.categoryIds);
    private final LiveData<Boolean> editMode = Transformations.map(formFields, fields -> fields.editMode);

    private final MutableLiveData<Set<CategoryWithTransactions>> cachedCategories = new MutableLiveData<>(new HashSet<>());
    private final LiveData<List<CategoryWithTransactions>> categorySelection;
    private final LiveData<DateFormat> systemDateFormat;
    private final LiveData<CurrencyFormat> currencyFormat;

    @Inject
    public TransactionFormViewModel(TrackingService trackingService,
                                    CategoryService categoryService,
                                    TransactionService transactionService,
                                    TrackingRepository trackingRepository,
                                    SystemService systemService,
                                    Executor executor,
                                    Logger logger) {
        this.trackingService = trackingService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.trackingRepository = trackingRepository;
        this.systemService = systemService;
        this.executor = executor;
        this.logger = logger;

        if (systemService != null) {
            this.systemDateFormat = Transformations.map(systemService.getPreferencesLive(), prefs -> {
                if (prefs == null || prefs.getDateFormat() == null) {
                    return DateFormat.DD_MM_YYYY;
                }
                return DateFormat.fromStringOrDefault(prefs.getDateFormat(), DateFormat.DD_MM_YYYY);
            });
            this.currencyFormat = Transformations.map(systemService.getPreferencesLive(), prefs -> {
                if (prefs == null || prefs.getCurrencyFormat() == null) {
                    return CurrencyFormat.NZD;
                }
                return CurrencyFormat.fromString(prefs.getCurrencyFormat());
            });
        } else {
            this.systemDateFormat = new MutableLiveData<>(DateFormat.DD_MM_YYYY);
            this.currencyFormat = new MutableLiveData<>(CurrencyFormat.NZD);
        }

        categorySelection = Transformations.switchMap(type, t -> {
            if (t == null) {
                return new MutableLiveData<>(java.util.Collections.emptyList());
            }
            return trackingService.searchCategoriesWithType("", t);
        });

        resetFieldErrors();
        clearSync();
    }

    public void resetFieldErrors() {
        updateLiveData(fieldErrors, new EnumMap<>(TransactionErrorFields.class));
    }

    @Override
    public void resetFieldError(TransactionErrorFields field) {
        Map<TransactionErrorFields, String> currentMap = fieldErrors.getValue();
        if (currentMap != null && currentMap.containsKey(field)) {
            Map<TransactionErrorFields, String> newMap = new EnumMap<>(currentMap);
            newMap.remove(field);
            updateLiveData(fieldErrors, newMap);
        }
    }

    @Override
    public void resetMessages() {
        setErrorMessage("");
        setSuccessMessage("");
    }

    @Override
    public void save() {
        saveTransaction();
    }

    @Override
    public void delete() {
        deleteTransaction();
    }

    private void updateFields(TransactionFormFields fields) {
        formFields.postValue(fields);
    }

    private void setErrorMessage(String errorMessage) {
        updateLiveData(this.errorMessage, errorMessage);
    }

    private void setSuccessMessage(String successMessage) {
        updateLiveData(this.successMessage, successMessage);
    }

    private void setIsLoading(Boolean isLoading) {
        updateLiveData(this.isLoading, isLoading);
    }

    public boolean hasFieldErrors() {
        return hasFieldErrors(fieldErrors.getValue());
    }

    public boolean hasFieldErrors(Map<TransactionErrorFields, String> errors) {
        return errors != null && !errors.isEmpty();
    }

    private void setFieldError(TransactionErrorFields field, String message) {
        Map<TransactionErrorFields, String> currentMap = fieldErrors.getValue();
        Map<TransactionErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(TransactionErrorFields.class);
        if (message == null || message.trim().isEmpty()) {
            newMap.remove(field);
        } else {
            newMap.put(field, message);
        }
        updateLiveData(fieldErrors, newMap);
    }

    public void setName(String name) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.name = name != null ? name : "";
            updateFields(fields);
            if (fields.name.trim().isEmpty()) {
                setFieldError(TransactionErrorFields.NAME, TransactionMessages.TRANSACTION_NAME_EMPTY.getMessage());
            } else {
                resetFieldError(TransactionErrorFields.NAME);
            }
        }
    }

    public void setDescription(String description) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.description = description;
            updateFields(fields);
            resetFieldError(TransactionErrorFields.DESCRIPTION);
        }
    }

    public void addCategoryId(Long categoryId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.categoryIds.add(categoryId);
            updateFields(fields);
            resetFieldError(TransactionErrorFields.CATEGORY_IDS);
            updateCachedCategories(fields.categoryIds);
        }
    }

    public void removeCategoryId(Long categoryId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null) {
            fields.categoryIds.remove(categoryId);
            updateFields(fields);
            updateCachedCategories(fields.categoryIds);
            if (fields.categoryIds.isEmpty()) {
                setFieldError(TransactionErrorFields.CATEGORY_IDS, TransactionMessages.TRANSACTION_CATEGORY_EMPTY.getMessage());
            }
        }
    }

    private void updateCachedCategories(Set<Long> categoryIds) {
        Runnable task = () -> {
            Set<CategoryWithTransactions> cats = new HashSet<>(trackingService.getCategoriesWithTransactionsByIds(categoryIds));
            updateLiveData(cachedCategories, cats);
        };
        if (executor != null) {
            executor.execute(task);
        } else {
            task.run();
        }
    }

    public void setAmount(BigDecimal amount) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.amount = amount;
            updateFields(fields);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                setFieldError(TransactionErrorFields.AMOUNT, GeneralMessages.INVALID_AMOUNT.getMessage(amount));
            } else {
                resetFieldError(TransactionErrorFields.AMOUNT);
            }
        }
    }

    public void setTypeSync(TrackingType type) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            TrackingType oldType = fields.type;
            fields.type = type;
            if (oldType != type) {
                fields.categoryIds.clear();
                if (type != null) {
                    try {
                        Category rootCat = categoryService.getRootCategoryByType(type);
                        if (rootCat != null) {
                            fields.categoryIds.add(rootCat.getCategoryId());
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, ErrorMessages.ROOT_CATEGORY_RESOLUTION_NOTICE.getMessage(e.getMessage()), e);
                    }
                }
                updateLiveData(cachedCategories, new HashSet<>(trackingService.getCategoriesWithTransactionsByIds(fields.categoryIds)));
            }
            updateFields(fields);
            if (type == null) {
                setFieldError(TransactionErrorFields.TYPE, GeneralMessages.INVALID_TYPE_ERROR_MESSAGE.getMessage());
            } else {
                resetFieldError(TransactionErrorFields.TYPE);
            }
        }
    }

    public void setType(TrackingType type) {
        if (executor != null) {
            executor.execute(() -> setTypeSync(type));
        } else {
            setTypeSync(type);
        }
    }

    public void setDateText(String dateStr) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                fields.date = null;
                updateFields(fields);
                setFieldError(TransactionErrorFields.DATE, TransactionMessages.TRANSACTION_DATE_EMPTY.getMessage());
                return;
            }
            LocalDateTime parsed = DateFormat.parseSimple(dateStr);
            if (parsed != null) {
                fields.date = parsed;
                updateFields(fields);
                resetFieldError(TransactionErrorFields.DATE);
            } else {
                fields.date = null;
                updateFields(fields);
                setFieldError(TransactionErrorFields.DATE, TransactionMessages.TRANSACTION_DATE_INVALID.getMessage());
            }
        }
    }

    public void setDate(LocalDateTime date) {
        TransactionFormFields fields = formFields.getValue();
        if (fields != null && Boolean.FALSE.equals(fields.editMode)) {
            fields.date = date;
            updateFields(fields);
            if (date == null) {
                setFieldError(TransactionErrorFields.DATE, TransactionMessages.TRANSACTION_DATE_EMPTY.getMessage());
            } else {
                resetFieldError(TransactionErrorFields.DATE);
            }
        }
    }

    public void setDate(LocalDate date) {
        if (date != null) {
            setDate(date.atStartOfDay());
        }
    }

    public void setTransactionId(Long transactionId) {
        executor.execute(() -> setTransactionIdSync(transactionId));
    }

    public void setTransactionIdSync(Long transactionId) {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null) fields = new TransactionFormFields();

        if (transactionId == null) {
            clearSync();
            return;
        }

        fields.transactionId = transactionId;
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
            updateFields(fields);
        } else {
            setErrorMessage(TransactionMessages.TRANSACTION_NOT_FOUND.getMessage());
            fields.transactionId = null;
            fields.editMode = false;
            formFields.postValue(fields);
            updateFields(fields);
        }
    }

    public void clear() {
        executor.execute(this::clearSync);
    }

    public void clearSync() {
        TransactionFormFields fields = new TransactionFormFields();
        formFields.postValue(fields);
        updateLiveData(cachedCategories, new HashSet<>());
        resetFieldErrors();
        setErrorMessage("");
        setSuccessMessage("");
        setIsLoading(false);
    }

    public void saveTransaction() {
        executor.execute(this::saveTransactionSync);
    }

    public void saveTransactionSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null) return;

        try {
            Map<TransactionErrorFields, String> errors = validateFormSync(fields);

            if (hasFieldErrors(errors)) {
                setErrorMessage(GeneralMessages.FORM_INCOMPLETE.getMessage("Please fix transaction form field errors"));
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

                setSuccessMessage(TransactionMessages.TRANSACTION_UPDATED.getMessage());
            } else {
                persistNewTransaction(fields.name, fields.description, fields.amount,
                        fields.type, fields.date, fields.categoryIds);
                clearSync();
                setSuccessMessage(TransactionMessages.TRANSACTION_SAVED.getMessage());
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, TransactionMessages.TRANSACTION_SAVE_FAILED.getMessage(e.getMessage()), e);
            }
            setErrorMessage(TransactionMessages.TRANSACTION_SAVE_FAILED.getMessage(e.getMessage()));
            setSuccessMessage("");
        } finally {
            setIsLoading(false);
        }
    }

    public SaveResult saveNewTransaction(String name,
                                         BigDecimal amount,
                                         TrackingType type,
                                         LocalDateTime date,
                                         Set<Long> categoryIds) {
        return saveNewTransaction(name, "", amount, type, date, categoryIds);
    }

    public SaveResult saveNewTransaction(String name,
                                         String description,
                                         BigDecimal amount,
                                         TrackingType type,
                                         LocalDateTime date,
                                         Set<Long> categoryIds) {
        Map<TransactionErrorFields, String> errors = collectErrors(name, amount, categoryIds, date, type);
        if (hasFieldErrors(errors)) {
            return SaveResult.failure(GeneralMessages.FORM_INCOMPLETE.getMessage(String.join("; ", errors.values())));
        }

        try {
            persistNewTransaction(name, description != null ? description : "", amount, type, date, categoryIds);
            return SaveResult.success(TransactionMessages.TRANSACTION_SAVED.getMessage());
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, TransactionMessages.TRANSACTION_SAVE_FAILED.getMessage(e.getMessage()), e);
            }
            return SaveResult.failure(TransactionMessages.TRANSACTION_SAVE_FAILED.getMessage(e.getMessage()));
        }
    }

    public void deleteTransaction() {
        executor.execute(this::deleteTransactionSync);
    }

    public void deleteTransactionSync() {
        TransactionFormFields fields = formFields.getValue();
        if (fields == null || fields.transactionId == null) {
            setErrorMessage(TransactionMessages.TRANSACTION_DELETE_NONE.getMessage());
            return;
        }
        try {
            setIsLoading(true);
            trackingRepository.deleteTransaction(fields.transactionId);
            clearSync();
            setSuccessMessage(TransactionMessages.TRANSACTION_DELETED.getMessage());
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, TransactionMessages.TRANSACTION_DELETE_FAILED.getMessage(e.getMessage()), e);
            }
            setErrorMessage(TransactionMessages.TRANSACTION_DELETE_FAILED.getMessage(e.getMessage()));
        } finally {
            setIsLoading(false);
        }
    }

    private Map<TransactionErrorFields, String> validateFormSync(TransactionFormFields fields) {
        Map<TransactionErrorFields, String> errors = collectErrors(fields.name, fields.amount, fields.categoryIds, fields.date, fields.type);
        updateLiveData(fieldErrors, errors);
        return errors;
    }

    private Map<TransactionErrorFields, String> collectErrors(String name,
                                                              BigDecimal amount,
                                                              Set<Long> categoryIds,
                                                              LocalDateTime date,
                                                              TrackingType type) {
        Map<TransactionErrorFields, String> errors = new EnumMap<>(TransactionErrorFields.class);

        if (name == null || name.trim().isEmpty()) {
            errors.put(TransactionErrorFields.NAME, TransactionMessages.TRANSACTION_NAME_EMPTY.getMessage());
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.put(TransactionErrorFields.AMOUNT, GeneralMessages.INVALID_AMOUNT.getMessage(amount));
        }
        if (categoryIds == null || categoryIds.isEmpty()) {
            errors.put(TransactionErrorFields.CATEGORY_IDS, TransactionMessages.TRANSACTION_CATEGORY_EMPTY.getMessage());
        } else if (!categoryService.verifyExistingIds(categoryIds)) {
            errors.put(TransactionErrorFields.CATEGORY_IDS, TransactionMessages.TRANSACTION_CATEGORIES_NOT_EXIST.getMessage());
        }
        if (date == null) {
            errors.put(TransactionErrorFields.DATE, TransactionMessages.TRANSACTION_DATE_EMPTY.getMessage());
        }
        if (type == null) {
            errors.put(TransactionErrorFields.TYPE, GeneralMessages.INVALID_TYPE_ERROR_MESSAGE.getMessage());
        }

        return errors;
    }

    private void persistNewTransaction(String name,
                                       String description,
                                       BigDecimal amount,
                                       TrackingType type,
                                       LocalDateTime date,
                                       Set<Long> categoryIds) {
        Transaction newTransaction = new Transaction.Builder(name, amount)
                .description(description)
                .type(type)
                .date(date)
                .build();

        boolean first = true;
        for (Long catId : categoryIds) {
            if (first) {
                trackingRepository.insertTransaction(newTransaction, catId);
                first = false;
            } else {
                trackingRepository.insertRelationship(newTransaction.getTransactionId(), catId);
            }
        }
    }

    public LiveData<String> getName() { return name; }
    public LiveData<String> getDescription() { return description; }
    public LiveData<BigDecimal> getAmount() { return amount; }
    public LiveData<TrackingType> getType() { return type; }
    public LiveData<LocalDateTime> getDate() { return date; }
    public LiveData<Set<Long>> getCategoryIds() { return categoryIds; }
    public LiveData<Boolean> getEditMode() { return editMode; }
    public LiveData<Map<TransactionErrorFields, String>> getFieldErrors() { return fieldErrors; }
    public LiveData<Set<CategoryWithTransactions>> getCachedCategories() { return cachedCategories; }
    public LiveData<List<CategoryWithTransactions>> getCategorySelection() { return categorySelection; }
    public LiveData<DateFormat> getSystemDateFormat() { return systemDateFormat; }
    public LiveData<CurrencyFormat> getCurrencyFormat() { return currencyFormat; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
}
