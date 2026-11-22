package com.example.mtfinance.src.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.messages.ErrorMessages;
import com.example.mtfinance.src.messages.ImportMessages;
import com.example.mtfinance.src.messages.GeneralMessages;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;
import com.example.mtfinance.src.trackingengine.HeaderAlias;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import com.example.mtfinance.src.viewmodels.interfaces.SaveableFormViewModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * This will import Transactions from a CSV file (exported from Bank).
 * <p>
 * The CSV file is never kept open between steps: it is opened to read the headers and
 * closed again, then opened a second time (and closed) when the import actually runs.
 * Each row is saved through {@link TransactionFormViewModel#saveNewTransaction}, which returns
 * its outcome directly, so the import never polls LiveData.
 */
@HiltViewModel
public class TransactionImportFormViewModel extends ViewModel implements SaveableFormViewModel<TransactionImportFormViewModel.TransactionImportErrorFields> {

    public enum TransactionImportErrorFields {
        FILE_SOURCE, NAME_HEADER, DESCRIPTION_HEADER, AMOUNT_HEADER, DATE_HEADER, TYPE_HEADER, DATE_FORMATTER
    }

    private static final String CSV_EXTENSION = ".csv";
    private static final String MIME_CSV = "csv";
    private static final String MIME_CSV_ALT = "comma-separated-values";
    private static final String MIME_PLAIN_TEXT = "text/plain";
    private static final int PREVIEW_ROW_LIMIT = 10;
    private static final int MIN_REQUIRED_COLUMNS = 3;

    private final CategoryService categoryService;
    private final TrackingService trackingService;
    private final TransactionService transactionService;
    private final TrackingRepository trackingRepository;
    private final Executor executor;
    private final Application application;
    private final Logger logger;

    private final MutableLiveData<String> filePath = new MutableLiveData<>();
    private final MutableLiveData<Uri> fileUri = new MutableLiveData<>();

    private final MutableLiveData<String> nameHeader = new MutableLiveData<>();
    private final MutableLiveData<String> descriptionHeader = new MutableLiveData<>();
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
    private final MutableLiveData<List<Map<String, String>>> previewRows = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public TransactionImportFormViewModel(CategoryService categoryService,
                                          TrackingService trackingService,
                                          TransactionService transactionService,
                                          TrackingRepository trackingRepository,
                                          Executor executor,
                                          Application application,
                                          Logger logger) {
        this.categoryService = categoryService;
        this.trackingService = trackingService;
        this.transactionService = transactionService;
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        this.application = application;
        this.logger = logger;
        clear();
    }

    /**
     * Resets all field-specific import error messages in the map.
     */
    public void resetFieldErrors() {
        updateLiveData(this.fieldErrors, new EnumMap<>(TransactionImportErrorFields.class));
    }

    @Override
    public void resetFieldError(TransactionImportErrorFields field) {
        Map<TransactionImportErrorFields, String> currentMap = fieldErrors.getValue();
        if (currentMap != null && currentMap.containsKey(field)) {
            Map<TransactionImportErrorFields, String> newMap = new EnumMap<>(currentMap);
            newMap.remove(field);
            updateLiveData(this.fieldErrors, newMap);
        }
    }

    @Override
    public void resetMessages() {
        setErrorMessage("");
        setSuccessMessage("");
        updateLiveData(this.successfulImports, new ArrayList<>());
        updateLiveData(this.failedImports, new ArrayList<>());
    }

    @Override
    public void save() {
        importTransaction();
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
        return errors != null && !errors.isEmpty();
    }

    private void setFieldError(TransactionImportErrorFields field, String message) {
        Map<TransactionImportErrorFields, String> currentMap = fieldErrors.getValue();
        Map<TransactionImportErrorFields, String> newMap = currentMap != null ? new EnumMap<>(currentMap) : new EnumMap<>(TransactionImportErrorFields.class);
        if (message == null || message.trim().isEmpty()) {
            newMap.remove(field);
        } else {
            newMap.put(field, message);
        }
        updateLiveData(this.fieldErrors, newMap);
    }

    public void setFilePath(String filePath) {
        this.filePath.setValue(filePath);
        this.fileUri.setValue(null);
        resetMessages();
        if (filePath != null && !filePath.isEmpty() && !isCsvFile(null, filePath)) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, GeneralMessages.NON_CSV_FILE_READ.getMessage());
            setErrorMessage(GeneralMessages.NON_CSV_FILE_READ.getMessage());
        } else {
            resetFieldError(TransactionImportErrorFields.FILE_SOURCE);
        }
    }

    public void setFileUri(Uri uri) {
        this.fileUri.setValue(uri);
        this.filePath.setValue("");
        resetMessages();
        if (uri != null && !isCsvFile(uri, null)) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, GeneralMessages.NON_CSV_FILE_READ.getMessage());
            setErrorMessage(GeneralMessages.NON_CSV_FILE_READ.getMessage());
        } else {
            resetFieldError(TransactionImportErrorFields.FILE_SOURCE);
        }
    }

    public boolean isCsvFile(Uri uri, String path) {
        if (uri != null) {
            String mimeType = application.getContentResolver().getType(uri);
            if (mimeType != null && (mimeType.contains(MIME_CSV)
                    || mimeType.contains(MIME_CSV_ALT)
                    || mimeType.contains(MIME_PLAIN_TEXT))) {
                return true;
            }
            String pathStr = uri.getPath();
            if (pathStr != null && pathStr.toLowerCase().endsWith(CSV_EXTENSION)) {
                return true;
            }
            return uri.toString().toLowerCase().endsWith(CSV_EXTENSION) || mimeType == null;
        }
        if (path != null && !path.isEmpty()) {
            return path.toLowerCase().endsWith(CSV_EXTENSION);
        }
        return false;
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

    private void validateHeaderInput(String value, TransactionImportErrorFields field, String missingMessage, boolean required) {
        List<String> headers = csvHeaders.getValue();
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                setFieldError(field, missingMessage);
            } else {
                resetFieldError(field);
            }
        } else if (headers != null && !headers.isEmpty() && headers.stream().noneMatch(h -> h.equalsIgnoreCase(value.trim()))) {
            setFieldError(field, ErrorMessages.HEADER_DOES_NOT_EXIST.getMessage(value.trim()));
        } else {
            resetFieldError(field);
        }
    }

    public void setNameHeader(String nameHeader) {
        updateLiveData(this.nameHeader, nameHeader);
        resetMessages();
        validateHeaderInput(nameHeader, TransactionImportErrorFields.NAME_HEADER, ImportMessages.IMPORT_NAME_HEADER_MISSING.getMessage(), true);
    }

    public void setDescriptionHeader(String descriptionHeader) {
        updateLiveData(this.descriptionHeader, descriptionHeader);
        resetMessages();
        validateHeaderInput(descriptionHeader, TransactionImportErrorFields.DESCRIPTION_HEADER, "", false);
    }

    public void setAmountHeader(String amountHeader) {
        updateLiveData(this.amountHeader, amountHeader);
        resetMessages();
        validateHeaderInput(amountHeader, TransactionImportErrorFields.AMOUNT_HEADER, ImportMessages.IMPORT_AMOUNT_HEADER_MISSING.getMessage(), true);
    }

    public void setDateHeader(String dateHeader) {
        updateLiveData(this.dateHeader, dateHeader);
        resetMessages();
        validateHeaderInput(dateHeader, TransactionImportErrorFields.DATE_HEADER, ImportMessages.IMPORT_DATE_HEADER_MISSING.getMessage(), true);
    }

    public void setTypeHeader(String typeHeader) {
        updateLiveData(this.typeHeader, typeHeader);
        resetMessages();
        validateHeaderInput(typeHeader, TransactionImportErrorFields.TYPE_HEADER, "", false);
    }

    public void setDateFormatter(DateFormat format) {
        updateLiveData(this.dateFormatter, format);
        resetMessages();
        resetFieldError(TransactionImportErrorFields.DATE_FORMATTER);
    }

    public void setDateFormatter(String format) {
        if (format != null && !format.trim().isEmpty()) {
            DateFormat matched = DateFormat.fromStringOrDefault(format, DateFormat.DD_MM_YYYY);
            setDateFormatter(matched);
        }
    }

    public void setAlwaysSendToRoot(Boolean alwaysSendToRoot) {
        updateLiveData(this.alwaysSendToRoot, alwaysSendToRoot);
    }

    public void clear() {
        this.filePath.setValue("");
        this.fileUri.setValue(null);
        this.nameHeader.setValue("");
        this.descriptionHeader.setValue("");
        this.amountHeader.setValue("");
        this.dateHeader.setValue("");
        this.typeHeader.setValue("");
        this.errorMessage.setValue("");
        this.successMessage.setValue("");
        this.failedImports.setValue(new ArrayList<>());
        this.csvHeaders.setValue(new ArrayList<>());
        this.previewRows.setValue(new ArrayList<>());
        this.isLoading.setValue(Boolean.FALSE);
        this.successfulImports.setValue(new ArrayList<>());
        this.alwaysSendToRoot.setValue(Boolean.TRUE);
        this.dateFormatter.setValue(DateFormat.DD_MM_YYYY);
        resetFieldErrors();
    }

    /**
     * Opens the currently selected CSV (Uri first, then path) and returns a parser for it.
     * The caller owns the parser and must close it (use try-with-resources).
     */
    private CSVParser openParser() throws IOException {
        Uri uri = fileUri.getValue();
        String path = filePath.getValue();

        Reader reader;
        if (uri != null) {
            InputStream inputStream = application.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException(GeneralMessages.STREAM_OPEN_FAILED.getMessage());
            }
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } else if (path != null && !path.isEmpty()) {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
        } else {
            throw new IOException(ImportMessages.NO_FILE_FOUND.getMessage());
        }

        try {
            return CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);
        } catch (IOException | RuntimeException e) {
            reader.close();
            throw e;
        }
    }

    public void readTransactionFile() {
        executor.execute(this::readTransactionFileSync);
    }

    public void readTransactionFileSync() {
        Uri uri = fileUri.getValue();
        String path = filePath.getValue();

        if ((uri == null) && (path == null || path.isEmpty())) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, ImportMessages.NO_FILE_FOUND.getMessage());
            setErrorMessage(ImportMessages.NO_FILE_FOUND.getMessage());
            return;
        }

        if (!isCsvFile(uri, path)) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, GeneralMessages.NON_CSV_FILE_READ.getMessage());
            setErrorMessage(GeneralMessages.NON_CSV_FILE_READ.getMessage());
            return;
        }

        try (CSVParser parser = openParser()) {
            List<String> headers = parser.getHeaderNames();
            if (headers == null || headers.size() < MIN_REQUIRED_COLUMNS) {
                String errorMsg = ErrorMessages.MIN_REQUIRED_COLUMNS.getMessage(MIN_REQUIRED_COLUMNS);
                setFieldError(TransactionImportErrorFields.FILE_SOURCE, errorMsg);
                setErrorMessage(errorMsg);
                setCsvHeaders(new ArrayList<>());
                updateLiveData(this.previewRows, new ArrayList<>());
                return;
            }

            setCsvHeaders(headers);

            List<Map<String, String>> sampleRows = new ArrayList<>();
            int rowCount = 0;
            for (CSVRecord record : parser) {
                if (rowCount >= PREVIEW_ROW_LIMIT) {
                    break;
                }
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (String header : headers) {
                    if (record.isMapped(header)) {
                        rowMap.put(header, record.get(header));
                    } else {
                        rowMap.put(header, "");
                    }
                }
                sampleRows.add(rowMap);
                rowCount++;
            }
            updateLiveData(this.previewRows, sampleRows);

            autoDetectHeaders(headers, sampleRows);
            resetFieldError(TransactionImportErrorFields.FILE_SOURCE);
        } catch (IOException e) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, ImportMessages.IMPORT_FILE_INVALID.getMessage(e.getMessage()));
            setErrorMessage(ImportMessages.IMPORT_FILE_INVALID.getMessage(e.getMessage()));
        } catch (Exception e) {
            setFieldError(TransactionImportErrorFields.FILE_SOURCE, ImportMessages.IMPORT_FILE_READ_FAILED.getMessage(e.getMessage()));
            setErrorMessage(ImportMessages.IMPORT_FILE_READ_FAILED.getMessage(e.getMessage()));
        }
    }

    /**
     * Auto-detects header column mappings using {@link HeaderAlias} definitions.
     */
    private void autoDetectHeaders(List<String> headers, List<Map<String, String>> sampleRows) {
        String detectedName = "";
        String detectedDesc = "";
        String detectedAmount = "";
        String detectedDate = "";
        String detectedType = "";

        // Check for explicit name header (payee, merchant, name)
        for (String header : headers) {
            if (HeaderAlias.NAME.matches(header)) {
                String lower = header.trim().toLowerCase();
                if (!lower.contains("description")) {
                    detectedName = header;
                    break;
                }
            }
        }

        // Check for explicit description / notes header
        for (String header : headers) {
            if (HeaderAlias.DESCRIPTION.matches(header)) {
                if (!header.equalsIgnoreCase(detectedName)) {
                    detectedDesc = header;
                    break;
                }
            }
        }

        // Handle fallback where "description" is the only header for payee
        if (detectedName.isEmpty()) {
            for (String header : headers) {
                if (header.trim().equalsIgnoreCase("description")) {
                    detectedName = header;
                    if (detectedDesc.equalsIgnoreCase(header)) {
                        detectedDesc = "";
                    }
                    break;
                }
            }
        }

        for (String header : headers) {
            if (detectedAmount.isEmpty() && HeaderAlias.AMOUNT.matches(header)) {
                detectedAmount = header;
            }
            if (detectedDate.isEmpty() && HeaderAlias.DATE.matches(header)) {
                detectedDate = header;
            }
            if (detectedType.isEmpty() && HeaderAlias.TYPE.matches(header)) {
                detectedType = header;
            }
        }

        setNameHeader(detectedName);
        setDescriptionHeader(detectedDesc);
        setAmountHeader(detectedAmount);
        setDateHeader(detectedDate);
        setTypeHeader(detectedType);

        if (!detectedDate.isEmpty() && !sampleRows.isEmpty()) {
            autoDetectDateFormat(detectedDate, sampleRows);
        }
    }

    /**
     * Simple trial-parsing auto-detection for date formats across sample CSV rows.
     */
    private void autoDetectDateFormat(String dateHeaderName, List<Map<String, String>> sampleRows) {
        List<String> sampleDates = new ArrayList<>();
        for (Map<String, String> row : sampleRows) {
            String val = row.get(dateHeaderName);
            if (val != null && !val.trim().isEmpty()) {
                sampleDates.add(val.trim());
            }
        }

        if (sampleDates.isEmpty()) {
            return;
        }

        for (DateFormat fmt : DateFormat.values()) {
            try {
                DateTimeFormatter formatter = fmt.toFormatter();
                boolean allParsed = true;
                for (String dateStr : sampleDates) {
                    try {
                        parseDate(dateStr, formatter);
                    } catch (DateTimeParseException e) {
                        allParsed = false;
                        break;
                    }
                }
                if (allParsed) {
                    setDateFormatter(fmt);
                    return;
                }
            } catch (IllegalArgumentException e) {
                // Candidate format formatter creation failed; continue to next candidate
            }
        }
    }

    public void importTransaction() {
        executor.execute(this::importTransactionSync);
    }

    public void importTransactionSync() {
        try {
            setIsLoading(Boolean.TRUE);
            resetMessages();
            Map<TransactionImportErrorFields, String> errors = validateImport();

            if (hasFieldErrors(errors)) {
                setErrorMessage(GeneralMessages.FORM_INCOMPLETE.getMessage(
                        GeneralMessages.IMPORT_CONFIGURATION_REQUIRED.getMessage()));
                setSuccessMessage("");
                return;
            }

            TransactionFormViewModel transactionForm = new TransactionFormViewModel(this.trackingService, this.categoryService, this.transactionService, this.trackingRepository, null, Runnable::run, this.logger);
            String nameHeader = this.nameHeader.getValue();
            String descriptionHeader = this.descriptionHeader.getValue();
            String dateHeader = this.dateHeader.getValue();
            String amountHeader = this.amountHeader.getValue();
            DateTimeFormatter dateFormatter = this.dateFormatter.getValue().toFormatter();
            List<String> successfulImports = new ArrayList<>();
            List<String> failedImports = new ArrayList<>();
            String typeHeader = this.typeHeader.getValue();
            Map<TrackingType, Long> rootCategoryIds = new EnumMap<>(TrackingType.class);

            try (CSVParser parser = openParser()) {
                for (CSVRecord record : parser) {
                    try {
                        String name = record.get(nameHeader);
                        String description = (descriptionHeader != null && !descriptionHeader.isEmpty() && record.isMapped(descriptionHeader))
                                ? record.get(descriptionHeader) : "";
                        BigDecimal amount = new BigDecimal(record.get(amountHeader));
                        String typeValue = (typeHeader != null && !typeHeader.isEmpty()) ? record.get(typeHeader) : null;
                        TrackingType type = determineType(amount, typeValue);
                        LocalDateTime date = parseDate(record.get(dateHeader), dateFormatter);
                        Long categoryId = findCategoryIdForTransaction(type, name, description, rootCategoryIds);

                        TransactionFormViewModel.SaveResult result = transactionForm.saveNewTransaction(
                                name, description, amount.abs(), type, date, Collections.singleton(categoryId));
                        if (!result.isSuccess()) {
                            throw new IllegalStateException(result.getMessage());
                        }
                        successfulImports.add(record.toString());
                    } catch (Exception e) {
                        logger.log(Level.WARNING, ErrorMessages.IMPORT_ROW_FAILED_NOTICE.getMessage(record.toString(), e.getMessage()), e);
                        failedImports.add(record.toString());
                    }
                }
            }

            updateLiveData(this.successfulImports, successfulImports);
            updateLiveData(this.failedImports, failedImports);

            setSuccessMessage(ImportMessages.IMPORT_SUCCESS.getMessage(successfulImports.size()));
            if (!failedImports.isEmpty()) {
                setErrorMessage(ImportMessages.IMPORT_FAILED.getMessage(failedImports.size()));
            }
        } catch (Exception e) {
            setErrorMessage(ImportMessages.IMPORT_STOPPED.getMessage());
        } finally {
            setIsLoading(Boolean.FALSE);
        }
    }

    /**
     * Parses a CSV date cell. Falls back to a date-only parse (start of day) when the
     * selected format has no time part.
     */
    private LocalDateTime parseDate(String text, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(text, formatter);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(text, formatter).atStartOfDay();
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

        List<String> headers = csvHeaders.getValue();
        if (headers == null || headers.isEmpty()) {
            errors.put(TransactionImportErrorFields.FILE_SOURCE, ImportMessages.IMPORT_PARSER_MISSING.getMessage());
        }

        validateHeaderInputForMap(nameHeader.getValue(), headers, TransactionImportErrorFields.NAME_HEADER, ImportMessages.IMPORT_NAME_HEADER_MISSING.getMessage(), true, errors);
        validateHeaderInputForMap(descriptionHeader.getValue(), headers, TransactionImportErrorFields.DESCRIPTION_HEADER, "", false, errors);
        validateHeaderInputForMap(amountHeader.getValue(), headers, TransactionImportErrorFields.AMOUNT_HEADER, ImportMessages.IMPORT_AMOUNT_HEADER_MISSING.getMessage(), true, errors);
        validateHeaderInputForMap(dateHeader.getValue(), headers, TransactionImportErrorFields.DATE_HEADER, ImportMessages.IMPORT_DATE_HEADER_MISSING.getMessage(), true, errors);
        validateHeaderInputForMap(typeHeader.getValue(), headers, TransactionImportErrorFields.TYPE_HEADER, "", false, errors);

        updateLiveData(this.fieldErrors, errors);
        return errors;
    }

    private void validateHeaderInputForMap(String value, List<String> availableHeaders, TransactionImportErrorFields field, String missingMessage, boolean required, Map<TransactionImportErrorFields, String> errors) {
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                errors.put(field, missingMessage);
            }
        } else if (availableHeaders != null && !availableHeaders.isEmpty() && availableHeaders.stream().noneMatch(h -> h.equalsIgnoreCase(value.trim()))) {
            errors.put(field, ErrorMessages.HEADER_DOES_NOT_EXIST.getMessage(value.trim()));
        }
    }

    private Long findCategoryIdForTransaction(TrackingType type, String name, String description, Map<TrackingType, Long> rootCategoryIds) {
        if (Boolean.FALSE.equals(alwaysSendToRoot.getValue())) {
            List<Long> matchedCategoryIds = categoryService.autoSearchCategoryIds(name, description, type);
            if (!matchedCategoryIds.isEmpty()) {
                return matchedCategoryIds.get(0);
            }
        }
        return rootCategoryIds.computeIfAbsent(type, t -> categoryService.getRootCategoryByType(t).getCategoryId());
    }

    public LiveData<String> getFilePath() { return filePath; }
    public LiveData<Uri> getFileUri() { return fileUri; }
    public LiveData<List<String>> getCsvHeaders() { return csvHeaders; }
    public LiveData<List<Map<String, String>>> getPreviewRows() { return previewRows; }
    public LiveData<String> getNameHeader() { return nameHeader; }
    public LiveData<String> getDescriptionHeader() { return descriptionHeader; }
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
