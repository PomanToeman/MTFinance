package com.example.mtfinance.src.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * This will import Transactions from a CSV file (exported from Bank).
 */
@HiltViewModel
public class TransactionImportFormViewModel extends ViewModel {

    public enum TransactionImportErrorFields {
        FILE_SOURCE, NAME_HEADER, AMOUNT_HEADER, DATE_HEADER, TYPE_HEADER, DATE_FORMATTER
    }

    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final TransactionService transactionService;
    private final TrackingRepository trackingRepository;
    private final Executor executor;
    private final Application application;

    private final MutableLiveData<String> filePath = new MutableLiveData<>();
    private final MutableLiveData<Uri> fileUri = new MutableLiveData<>();
    private final MutableLiveData<CSVParser> csvParser = new MutableLiveData<>();

    private final MutableLiveData<String> nameHeader = new MutableLiveData<>();
    private final MutableLiveData<String> amountHeader = new MutableLiveData<>();
    private final MutableLiveData<String> dateHeader = new MutableLiveData<>();
    private final MutableLiveData<String> typeHeader = new MutableLiveData<>();
    private final MutableLiveData<DateFormat> dateFormatter = new MutableLiveData<>();

    private final MutableLiveData<Map<TransactionImportErrorFields, String>> fieldErrors = new MutableLiveData<>(new EnumMap<>(TransactionImportErrorFields.class));

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> successfulImports = new MutableLiveData<>();
    private final MutableLiveData<List<String>> failedImports = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alwaysSendToRoot = new MutableLiveData<>(Boolean.TRUE);

    private final MutableLiveData<List<String>> csvHeaders = new MutableLiveData<>();

    @Inject
    public TransactionImportFormViewModel(CategoryService categoryService,
                                          TrackingService trackingService,
                                          TransactionService transactionService,
                                          TrackingRepository trackingRepository,
                                          Executor executor,
                                          Application application) {
        this.categoryService = categoryService;
        this.trackingService = trackingService;
        this.transactionService = transactionService;
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        this.application = application;
        clear();
    }

    /**
     * Resets all field-specific import error messages in the map.
     */
    public void resetFieldErrors() {
        Map<TransactionImportErrorFields, String> errors = new EnumMap<>(TransactionImportErrorFields.class);
        for (TransactionImportErrorFields field : TransactionImportErrorFields.values()) {
            errors.put(field, "");
        }
        updateLiveData(this.fieldErrors, errors);
    }

    /**
     * Checks if any field error exists in the current LiveData error map.
     * @return true if at least one import field has a non-empty error message
     */
    public boolean hasFieldErrors() {
        return hasFieldErrors(fieldErrors.getValue());
    }

    /**
     * Checks if any field error exists in the given error map.
     * @param errors the error map to check
     * @return true if at least one import field has a non-empty error message
     */
    public boolean hasFieldErrors(Map<TransactionImportErrorFields, String> errors) {
        if (errors == null || errors.isEmpty()) return false;
        for (String error : errors.values()) {
            if (error != null && !error.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setFieldError(TransactionImportErrorFields field, String message) {
        Map<TransactionImportErrorFields, String> currentMap = fieldErrors.getValue();
        Map<TransactionImportErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(TransactionImportErrorFields.class);
        newMap.put(field, message != null ? message : "");
        updateLiveData(this.fieldErrors, newMap);
    }

    public void setFilePath(String filePath) {
        this.filePath.setValue(filePath);
        this.fileUri.setValue(null);
        setFieldError(TransactionImportErrorFields.FILE_SOURCE, "");
    }

    public void setFileUri(Uri uri) {
        this.fileUri.setValue(uri);
        this.filePath.setValue("");
        setFieldError(TransactionImportErrorFields.FILE_SOURCE, "");
    }

    private void setCsvHeaders(List<String> headers) {
        updateLiveData(this.csvHeaders, headers);
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

    public void setNameHeader(String nameHeader) {
        updateLiveData(this.nameHeader, nameHeader);
        if (nameHeader == null || nameHeader.trim().isEmpty()) {
            setFieldError(TransactionImportErrorFields.NAME_HEADER, MessageCli.IMPORT_NAME_HEADER_MISSING.getMessage());
        } else {
            setFieldError(TransactionImportErrorFields.NAME_HEADER, "");
        }
    }

    public void setAmountHeader(String amountHeader) {
        updateLiveData(this.amountHeader, amountHeader);
        if (amountHeader == null || amountHeader.trim().isEmpty()) {
            setFieldError(TransactionImportErrorFields.AMOUNT_HEADER, MessageCli.IMPORT_AMOUNT_HEADER_MISSING.getMessage());
        } else {
            setFieldError(TransactionImportErrorFields.AMOUNT_HEADER, "");
        }
    }

    public void setDateHeader(String dateHeader) {
        updateLiveData(this.dateHeader, dateHeader);
        if (dateHeader == null || dateHeader.trim().isEmpty()) {
            setFieldError(TransactionImportErrorFields.DATE_HEADER, MessageCli.IMPORT_DATE_HEADER_MISSING.getMessage());
        } else {
            setFieldError(TransactionImportErrorFields.DATE_HEADER, "");
        }
    }

    public void setTypeHeader(String typeHeader) {
        updateLiveData(this.typeHeader, typeHeader);
        setFieldError(TransactionImportErrorFields.TYPE_HEADER, "");
    }

    public void setDateFormatter(DateFormat format) {
        updateLiveData(this.dateFormatter, format);
        setFieldError(TransactionImportErrorFields.DATE_FORMATTER, "");
    }

    public void setDateFormatter(String format) {
        try {
            setDateFormatter(DateFormat.valueOf(format));
        } catch (Exception ignored) {
        }
    }

    public void setAlwaysSendToRoot(Boolean alwaysSendToRoot) {
        updateLiveData(this.alwaysSendToRoot, alwaysSendToRoot);
    }

    public void clear() {
        this.filePath.setValue("");
        this.fileUri.setValue(null);
        this.csvParser.setValue(null);
        this.nameHeader.setValue("");
        this.amountHeader.setValue("");
        this.dateHeader.setValue("");
        this.typeHeader.setValue("");
        this.errorMessage.setValue("");
        this.successMessage.setValue("");
        this.failedImports.setValue(new ArrayList<>());
        this.csvHeaders.setValue(new ArrayList<>());
        this.isLoading.setValue(Boolean.FALSE);
        this.successfulImports.setValue(new ArrayList<>());
        this.alwaysSendToRoot.setValue(Boolean.TRUE);
        this.dateFormatter.setValue(DateFormat.DD_MM_YYYY);
        resetFieldErrors();
    }

    public void readTransactionFile() {
        executor.execute(this::readTransactionFileSync);
    }

    public void readTransactionFileSync() {
        Uri uri = fileUri.getValue();
        String path = filePath.getValue();

        if ((uri == null) && (path == null || path.isEmpty())) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, MessageCli.NO_FILE_FOUND.getMessage());
            setErrorMessage(MessageCli.NO_FILE_FOUND.getMessage());
            return;
        }

        try {
            Reader reader;
            if (uri != null) {
                InputStream inputStream = application.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    setFieldError(TransactionImportErrorFields.FILE_SOURCE, MessageCli.IMPORT_FILE_INVALID.getMessage("Could not open stream from Uri"));
                    setErrorMessage(MessageCli.IMPORT_FILE_INVALID.getMessage("Could not open stream from Uri"));
                    return;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } else {
                reader = new FileReader(path);
            }

            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);

            updateLiveData(this.csvParser, csvParser);
            updateLiveData(this.dateHeader, "");
            updateLiveData(this.nameHeader, "");
            updateLiveData(this.amountHeader, "");
            setCsvHeaders(csvParser.getHeaderNames());
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, "");
        } catch (IOException e) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, MessageCli.IMPORT_FILE_INVALID.getMessage(e.getMessage()));
            setErrorMessage(MessageCli.IMPORT_FILE_INVALID.getMessage(e.getMessage()));
        } catch (Exception e) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, MessageCli.IMPORT_FILE_READ_FAILED.getMessage(e.getMessage()));
            setErrorMessage(MessageCli.IMPORT_FILE_READ_FAILED.getMessage(e.getMessage()));
        }
    }

    public void importTransaction() {
        executor.execute(this::importTransactionSync);
    }

    public void importTransactionSync() {
        try {
            setIsLoading(Boolean.TRUE);
            Map<TransactionImportErrorFields, String> errors = validateImport();

            if (hasFieldErrors(errors)) {
                setErrorMessage(MessageCli.FORM_INCOMPLETE.getMessage("Please configure required import headers and CSV file"));
                setSuccessMessage("");
                return;
            }

            TransactionFormViewModel transactionForm = new TransactionFormViewModel(this.trackingService, this.categoryService, this.transactionService, this.trackingRepository, Runnable::run);
            CSVParser csvParser = this.csvParser.getValue();
            String nameHeader = this.nameHeader.getValue();
            String dateHeader = this.dateHeader.getValue();
            String amountHeader = this.amountHeader.getValue();
            DateTimeFormatter dateFormatter = this.dateFormatter.getValue().toFormatter();
            List<String> successfulImports = new ArrayList<>();
            List<String> failedImports = new ArrayList<>();
            String typeHeader = this.typeHeader.getValue();

            for (CSVRecord record : csvParser) {
                try {
                    transactionForm.setName(record.get(nameHeader));
                    BigDecimal amount = new BigDecimal(record.get(amountHeader));
                    String typeValue = (typeHeader != null && !typeHeader.isEmpty()) ? record.get(typeHeader) : null;
                    TrackingType type = determineType(amount, typeValue);
                    transactionForm.setAmount(amount.abs());
                    transactionForm.setType(type);

                    try {
                        transactionForm.setDate(LocalDateTime.parse(record.get(dateHeader), dateFormatter));
                    } catch (Exception e) {
                        transactionForm.setDate(LocalDate.parse(record.get(dateHeader), dateFormatter));
                    }

                    transactionForm.addCategoryId(findCategoryIdForTransaction(type, record.get(nameHeader)));

                    transactionForm.saveTransactionSync();
                    Thread.sleep(10);
                    do {
                        Thread.sleep(1);
                    } while (Boolean.TRUE.equals(transactionForm.getIsLoading().getValue()));

                    if (!transactionForm.getSuccessMessage().getValue().isEmpty()) {
                        successfulImports.add(record.toString());
                    } else {
                        throw new Exception(transactionForm.getErrorMessage().getValue());
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage() + " " + record.toString());
                    failedImports.add(record.toString());
                } finally {
                    transactionForm.clearSync();
                }
            }

            updateLiveData(this.successfulImports, successfulImports);

            setSuccessMessage(MessageCli.IMPORT_SUCCESS.getMessage(successfulImports.size()));
            if (!failedImports.isEmpty()) {
                setErrorMessage(MessageCli.IMPORT_FAILED.getMessage(failedImports.size()));
            }
        } catch (Exception e) {
            setErrorMessage(MessageCli.IMPORT_STOPPED.getMessage());
        } finally {
            setIsLoading(Boolean.FALSE);
        }
    }

    private TrackingType determineType(BigDecimal amount, String typeValue) {
        if (typeValue != null && !typeValue.isEmpty()) {
            TrackingType type = TrackingType.fromString(typeValue);
            if (type != TrackingType.OTHER) {
                return type;
            }
        }
        return TrackingUtlis.determineTypeByAmount(amount);
    }

    private Map<TransactionImportErrorFields, String> validateImport() {
        Map<TransactionImportErrorFields, String> errors = new EnumMap<>(TransactionImportErrorFields.class);
        for (TransactionImportErrorFields field : TransactionImportErrorFields.values()) {
            errors.put(field, "");
        }

        if (csvParser.getValue() == null) {
            errors.put(TransactionImportErrorFields.FILE_SOURCE, MessageCli.IMPORT_PARSER_MISSING.getMessage());
        }
        if (nameHeader.getValue() == null || nameHeader.getValue().trim().isEmpty()) {
            errors.put(TransactionImportErrorFields.NAME_HEADER, MessageCli.IMPORT_NAME_HEADER_MISSING.getMessage());
        }
        if (amountHeader.getValue() == null || amountHeader.getValue().trim().isEmpty()) {
            errors.put(TransactionImportErrorFields.AMOUNT_HEADER, MessageCli.IMPORT_AMOUNT_HEADER_MISSING.getMessage());
        }
        if (dateHeader.getValue() == null || dateHeader.getValue().trim().isEmpty()) {
            errors.put(TransactionImportErrorFields.DATE_HEADER, MessageCli.IMPORT_DATE_HEADER_MISSING.getMessage());
        }

        updateLiveData(this.fieldErrors, errors);
        return errors;
    }

    private Long findCategoryIdForTransaction(TrackingType type, String query) {
        if (Boolean.FALSE.equals(alwaysSendToRoot.getValue())) {
            List<Long> matchedCategoryIds = categoryService.autoSearchCategoryIds(query, type);
            if (!matchedCategoryIds.isEmpty()) {
                return matchedCategoryIds.get(0);
            }
        }
        return categoryService.getRootCategoryByType(type).getCategoryId();
    }

    private <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            liveData.setValue(value);
        } catch (Exception e) {
            liveData.postValue(value);
        }
    }

    public LiveData<String> getFilePath() { return filePath; }
    public LiveData<Uri> getFileUri() { return fileUri; }
    public LiveData<CSVParser> getCsvParser() { return csvParser; }
    public LiveData<List<String>> getCsvHeaders() { return csvHeaders; }
    public LiveData<String> getNameHeader() { return nameHeader; }
    public LiveData<String> getAmountHeader() { return amountHeader; }
    public LiveData<String> getDateHeader() { return dateHeader; }
    public LiveData<String> getTypeHeader() { return typeHeader; }
    public LiveData<DateFormat> getDateFormatter() { return dateFormatter; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<String>> getSuccessfulImports() { return successfulImports; }
    public LiveData<List<String>> getFailedImports() { return failedImports; }
    public LiveData<Boolean> getAlwaysSendToRoot() { return alwaysSendToRoot; }
    public LiveData<Map<TransactionImportErrorFields, String>> getFieldErrors() { return fieldErrors; }
}
