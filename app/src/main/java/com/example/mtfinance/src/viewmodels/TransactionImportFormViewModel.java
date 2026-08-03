package com.example.mtfinance.src.viewmodels;


import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.DateFormat;
import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.TrackingRepository;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * This will import Transactions from a CSV file (exported from Bank).
 *
 */
@HiltViewModel
public class TransactionImportFormViewModel extends ViewModel {
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

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> successfulImports = new MutableLiveData<>();
    private final MutableLiveData<List<String>> failedImports = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alwaysSendToRoot = new MutableLiveData<>(Boolean.TRUE);

    private final MutableLiveData<List<String>> csvHeaders = new MutableLiveData<>();


    @Inject
    public TransactionImportFormViewModel(TrackingRepository trackingRepository, Executor executor, Application application) {
        this.trackingRepository = trackingRepository;
        this.executor = executor;
        this.application = application;
        clear();
    }

    // Setters

    public void setFilePath(String filePath) {
        this.filePath.setValue(filePath);
        this.fileUri.setValue(null);
    }

    public void setFileUri(Uri uri) {
        this.fileUri.setValue(uri);
        this.filePath.setValue("");
    }

    private void setCsvHeaders(List<String> headers) {
        updateLiveData(this.csvHeaders, headers);
    }

    private void setErrorMessage(String errorMessage)  {
        updateLiveData(this.errorMessage, errorMessage);
    }

    private void setSuccessMessage(String successMessage)  {
        updateLiveData(this.successMessage, successMessage);
    }

    private void setIsLoading(Boolean booleanValue) {
        updateLiveData(this.isLoading, booleanValue);
    }

    /**
     * Required header
     * @param nameHeader - header name (must be in csvHeaders).
     */
    public void setNameHeader(String nameHeader) {
        if (this.csvHeaders.getValue() != null && this.csvHeaders.getValue().contains(nameHeader)) {
            this.nameHeader.setValue(nameHeader);
        }



    }

    /**
     * Required header
     * @param amountHeader - header name (must be in csvHeaders).
     */
    public void setAmountHeader(String amountHeader) {
        if (this.csvHeaders.getValue() != null && this.csvHeaders.getValue().contains(amountHeader)) {
            this.amountHeader.setValue(amountHeader);
        }


    }

    /**
     * optional Header. Only set when there is an explicit type header.
     * @param typeHeader - header name (must be in csvHeaders).
     */
    public void setTypeHeader(String typeHeader) {
        if (this.csvHeaders.getValue() != null && this.csvHeaders.getValue().contains(typeHeader)) {
            this.typeHeader.setValue(typeHeader);

        }

    }

    /**
     * Required header. Please format to what the actual records are formatted in the file.
     * @param dateHeader - header name (must be in csvHeaders).
     */
    public void setDateHeader(String dateHeader) {
        if (this.csvHeaders.getValue() != null && this.csvHeaders.getValue().contains(dateHeader)) {
            this.dateHeader.setValue(dateHeader);
        }


    }

    /**
     * Set to the specific format of the date (and time if possible). Will ignore if invalid format.
     *
     * @param dateFormatter - What the format looks like.
     */
    public void setDateFormatter(String dateFormatter) {
        try {
            this.dateFormatter.setValue(DateFormat.fromString(dateFormatter));
            setErrorMessage("");
        }
        catch (IllegalArgumentException e) {
            setErrorMessage(MessageCli.IMPORT_DATE_FORMAT_INVALID.getMessage(e.getMessage()));
            this.dateFormatter.setValue(DateFormat.DD_MM_YYYY);
        }
    }

    public void setDateFormatter(DateFormat dateFormatter)  {
        this.dateFormatter.setValue(dateFormatter);

    }

    /**
     * When false, The import method will try to auto find a category for it (with the root as a fallback).
     * @param alwaysSendToRoot - The truth value.
     */
    public void setAlwaysSendToRoot(Boolean alwaysSendToRoot) {
        this.alwaysSendToRoot.setValue(alwaysSendToRoot);
    }



    /**
     * Clears the form to their default values
     */
    public void clear() {
        this.filePath.setValue("");
        this.fileUri.setValue(null);
        this.csvParser.setValue(null);
        this.errorMessage.setValue("");
        this.successMessage.setValue("");
        this.nameHeader.setValue("");
        this.amountHeader.setValue("");
        this.dateHeader.setValue("");
        this.typeHeader.setValue("");
        this.failedImports.setValue(new ArrayList<>());
        this.csvHeaders.setValue(new ArrayList<>());
        this.isLoading.setValue(Boolean.FALSE);
        this.successfulImports.setValue(new ArrayList<>());
        this.alwaysSendToRoot.setValue(Boolean.TRUE);
        this.dateFormatter.setValue(DateFormat.DD_MM_YYYY);

    }


    /**
     * Reads the CSV file and sets the csv headers and phases to prepare for importing.
     * User needs to manually set each field header before importing.
     * You must do this before importing!
     *
     */

    public void readTransactionFile() {
        executor.execute(this::readTransactionFileSync);
    }

    public void readTransactionFileSync() {
        Uri uri = fileUri.getValue();
        String path = filePath.getValue();

        if ((uri == null) && (path == null || path.isEmpty())) {
            setErrorMessage(MessageCli.NO_FILE_FOUND.getMessage());
            return;
        }

        try {
            Reader reader;
            if (uri != null) {
                InputStream inputStream = application.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    setErrorMessage(MessageCli.IMPORT_FILE_INVALID.getMessage("Could not open stream from Uri"));
                    return;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } else {
                reader = new FileReader(path);
            }

            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()                    // Use first record as header
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);

            updateLiveData(this.csvParser, csvParser);
            updateLiveData(this.dateHeader, "");
            updateLiveData(this.nameHeader, "");
            updateLiveData(this.amountHeader, "");
            setCsvHeaders(csvParser.getHeaderNames());


        }
        catch (IOException e) {
            setErrorMessage(MessageCli.IMPORT_FILE_INVALID.getMessage(e.getMessage()));
        }
        catch (Exception e) {
            setErrorMessage(MessageCli.IMPORT_FILE_READ_FAILED.getMessage(e.getMessage()));
        }


    }

    /**
     * This will import a csv transaction file for automatic insertion in database.
     * All required fields must be set before importing.
     *
     */
    public void importTransaction() {
        executor.execute(this::importTransactionSync);
    }

    public void importTransactionSync() {
        try {
            setIsLoading(Boolean.TRUE);
            validateImport();
            TransactionFormViewModel transactionForm = new TransactionFormViewModel(this.trackingRepository, Runnable::run);
            CSVParser csvParser = this.csvParser.getValue();
            String nameHeader = this.nameHeader.getValue();
            String dateHeader = this.dateHeader.getValue();
            String amountHeader = this.amountHeader.getValue();
            DateTimeFormatter dateFormatter = this.dateFormatter.getValue().toFormatter();
            List<String> successfulImports = new ArrayList<>();
            List<String> failedImports = new ArrayList<>();
            String typeHeader = this.typeHeader.getValue();



            // imports each record (if possible)

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
                        // try as LocalDate if LocalDateTime fails (in case no time in pattern/record)
                        transactionForm.setDate(LocalDate.parse(record.get(dateHeader), dateFormatter));
                    }

                    transactionForm.addCategoryId(findCategoryIdForTransaction(type, record.get(nameHeader)));

                    // create and insert instance.
                    transactionForm.saveTransactionSync();
                    Thread.sleep(10);
                    do {
                        Thread.sleep(1);
                    } while (Boolean.TRUE.equals(transactionForm.getIsLoading().getValue()));

                    // check for success
                    if (!transactionForm.getSuccessMessage().getValue().isEmpty()) {
                        successfulImports.add(record.toString());

                    }
                    else {
                        throw new Exception(transactionForm.getErrorMessage().getValue());
                    }



                } catch (Exception e) {
                    // record and skip transactions
                    System.err.println(e.getMessage() + " " + record.toString());
                    failedImports.add(record.toString());

                }
                finally {
                    transactionForm.clearSync();


                }

            }

            updateLiveData(this.successfulImports, successfulImports);



            setSuccessMessage(MessageCli.IMPORT_SUCCESS.getMessage(successfulImports.size()));
            if (!failedImports.isEmpty()) {
                setErrorMessage(MessageCli.IMPORT_FAILED.getMessage(failedImports.size()));
            }

        }
        catch (IllegalArgumentException e) {
            setErrorMessage(MessageCli.FORM_INCOMPLETE.getMessage(e.getMessage()));
        } catch (Exception e) {
            setErrorMessage(MessageCli.IMPORT_STOPPED.getMessage());
        }
        finally {
            setIsLoading(Boolean.FALSE);
        }

    }

    /**
     * Determines the type of transaction based type record if there is a type header.
     * Else it will determine the type based on the amount.
     * @param amount - amount of transaction.
     * @param typeValue - value of the type record.
     * @return - type of transaction.
     */
    private TrackingType determineType(BigDecimal amount, String typeValue) {
        if (typeValue != null && !typeValue.isEmpty()) {
            TrackingType type = TrackingType.fromString(typeValue);
            if (type != TrackingType.OTHER) {
                return type;
            }
        }

        return TrackingUtlis.determineTypeByAmount(amount);

    }

    /**
     * This ensures the required form fields are validated before the import.
     * csvParser and essential headers (name, date, amount) must be put in.
     *
     * @throws IllegalArgumentException - If there is an illegal field.
     */

    private void validateImport() throws IllegalArgumentException {
        if (csvParser.getValue() == null || csvHeaders.getValue() == null) {
            throw new IllegalArgumentException(MessageCli.IMPORT_PARSER_MISSING.getMessage());
        }
        if (nameHeader.getValue() == null || nameHeader.getValue().isEmpty() ) {
            throw new IllegalArgumentException(MessageCli.IMPORT_NAME_HEADER_MISSING.getMessage());
        }
        if (dateHeader.getValue() == null || dateHeader.getValue().isEmpty() || !csvHeaders.getValue().contains(dateHeader.getValue())) {
            throw new IllegalArgumentException(MessageCli.IMPORT_DATE_HEADER_MISSING.getMessage());
        }
        if (amountHeader.getValue() == null || amountHeader.getValue().isEmpty() || !csvHeaders.getValue().contains(amountHeader.getValue())) {
            throw new IllegalArgumentException(MessageCli.IMPORT_AMOUNT_HEADER_MISSING.getMessage());
        }
        if (dateFormatter.getValue() == null) {
            throw new IllegalArgumentException(MessageCli.IMPORT_DATE_FORMAT_MISSING.getMessage());
        }
        if (typeHeader != null && !typeHeader.getValue().isEmpty() && !csvHeaders.getValue().contains(typeHeader.getValue())) {
            throw new IllegalArgumentException(MessageCli.IMPORT_TYPE_HEADER_MISSING.getMessage());
        }
    }

    /**
     * Tries to find the best category (via id) for the transaction.
     * Will fallback to root if none is found or alwaysSendToRoot is true.
     * @param type - type of transaction
     * @param name - name of transaction
     * @return - id of category
     */
    private Long findCategoryIdForTransaction(TrackingType type, String name) {
        if (Boolean.TRUE.equals(alwaysSendToRoot.getValue())) {
            return trackingRepository.getRootCategoryByType(type).getCategoryId();

        }
        List<Long> foundCategories = trackingRepository.autoSearchCategoryIds(name, type);
        if (!foundCategories.isEmpty()) {
            return foundCategories.get(0);
        }

        // fallback to root if no category found.
       return trackingRepository.getRootCategoryByType(type).getCategoryId();

    }

    private <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            liveData.setValue(value);
        } catch (IllegalStateException e) {
            liveData.postValue(value);
        }
    }


    // public getters


    public LiveData<CSVParser> getCsvParser() {
        return csvParser;
    }
    public LiveData<String> getFilePath() {
        return filePath;
    }
    public LiveData<Uri> getFileUri() {
        return fileUri;
    }

    public LiveData<List<String>> getCsvHeaders() {
        return csvHeaders;
    }
    public LiveData<String> getNameHeader() {
        return nameHeader;
    }
    public LiveData<String> getAmountHeader() {
        return amountHeader;
    }
    public LiveData<String> getDateHeader() {
        return dateHeader;
    }

    public LiveData<String> getTypeHeader() {
        return typeHeader;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<Boolean> getAlwaysSendToRoot() {
        return alwaysSendToRoot;
    }

    public LiveData<List<String>> getSuccessfulImports() {
        return successfulImports;
    }

    public LiveData<List<String>> getFailedImports() {
        return failedImports;
    }

    public LiveData<DateFormat> getDateFormatter() {
        return dateFormatter;
    }

}
