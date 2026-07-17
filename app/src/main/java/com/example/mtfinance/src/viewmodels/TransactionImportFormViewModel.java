package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.MessageCli;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import jakarta.inject.Inject;

/**
 * This will import Transactions from a CSV file (exported from Bank).
 *
 */
@HiltViewModel
public class TransactionImportFormViewModel extends ViewModel {
    private final TrackingRepository trackingRepository;
    private final MutableLiveData<String> filePath = new MutableLiveData<>();
    private final MutableLiveData<CSVParser> csvParser = new MutableLiveData<>();


    private final MutableLiveData<String> nameHeader = new MutableLiveData<>();
    private final MutableLiveData<String> amountHeader = new MutableLiveData<>();
    private final MutableLiveData<String> dateHeader = new MutableLiveData<>();
    private final MutableLiveData<String> typeHeader = new MutableLiveData<>();
    private final MutableLiveData<DateTimeFormatter> dateFormatter = new MutableLiveData<>();

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> successfulImports = new MutableLiveData<>();
    private final MutableLiveData<List<String>> failedImports = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alwaysSendToRoot = new MutableLiveData<>(Boolean.TRUE);

    private final MutableLiveData<List<String>> csvHeaders = new MutableLiveData<>();


    @Inject
    public TransactionImportFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        clear();
    }

    // Setters

    public void setFilePath(String filePath) {
        this.filePath.setValue(filePath);
    }

    private void setCsvHeaders(List<String> headers) {
        this.csvHeaders.setValue(headers);

    }

    private void setErrorMessage(String errorMessage)  {
        this.errorMessage.setValue(errorMessage);
    }

    private void setSuccessMessage(String successMessage)  {
        this.successMessage.setValue(successMessage);
    }

    private void setIsLoading(Boolean booleanValue) {
        this.isLoading.setValue(booleanValue);
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
            this.dateFormatter.setValue(DateTimeFormatter.ofPattern(dateFormatter));
        }
        catch (IllegalArgumentException e) {
            setErrorMessage(MessageCli.IMPORT_DATE_FORMAT_INVALID.getMessage(e.getMessage()));
            this.dateFormatter.setValue(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
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
        this.dateFormatter.setValue(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    }


    /**
     * Reads the CSV file and sets the csv headers and phases to prepare for importing.
     * User needs to manually set each field header before importing.
     * You must do this before importing!
     *
     */

    public void readTransactionFile() {
        if (filePath.getValue() == null || filePath.getValue().isEmpty()) {
            setErrorMessage(MessageCli.NO_FILE_FOUND.getMessage());
            return;
        }


        try {
            Reader reader = new FileReader(this.filePath.getValue());
            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()                    // Use first record as header
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);

            this.csvParser.setValue(csvParser);
            dateHeader.setValue("");
            nameHeader.setValue("");
            amountHeader.setValue("");
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
        try {
            setIsLoading(Boolean.TRUE);
            validateImport();
            TransactionFormViewModel transactionForm = new TransactionFormViewModel(this.trackingRepository);
            CSVParser csvParser = this.csvParser.getValue();
            String nameHeader = this.nameHeader.getValue();
            String dateHeader = this.dateHeader.getValue();
            String amountHeader = this.amountHeader.getValue();
            DateTimeFormatter dateFormatter = this.dateFormatter.getValue();
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
                    transactionForm.saveTransaction();

                    // check for success
                    if (!transactionForm.getSuccessMessage().getValue().isEmpty()) {
                        successfulImports.add(record.toString());

                    }


                } catch (Exception e) {
                    // record and skip transactions
                    failedImports.add(record.toString());

                }
                finally {
                    transactionForm.clear();

                }
            }

            this.successfulImports.setValue(successfulImports);



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
            return TrackingType.fromString(typeValue);
        } else {
            return TrackingUtlis.determineTypeByAmount(amount);
        }
    }

    /**
     * This ensures the required form fields are validated before the import.
     * csvParser and essential headers (name, date, amount) must be put in.
     *
     * @throws IllegalArgumentException - If there is an illegal field.
     */

    private void validateImport() throws IllegalArgumentException {
        if (csvParser.getValue() == null) {
            throw new IllegalArgumentException(MessageCli.IMPORT_PARSER_MISSING.getMessage());
        }
        if (nameHeader.getValue() == null || nameHeader.getValue().isEmpty()) {
            throw new IllegalArgumentException(MessageCli.IMPORT_NAME_HEADER_MISSING.getMessage());
        }
        if (dateHeader.getValue() == null || dateHeader.getValue().isEmpty()) {
            throw new IllegalArgumentException(MessageCli.IMPORT_DATE_HEADER_MISSING.getMessage());
        }
        if (amountHeader.getValue() == null || amountHeader.getValue().isEmpty()) {
            throw new IllegalArgumentException(MessageCli.IMPORT_AMOUNT_HEADER_MISSING.getMessage());
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


    // public getters


    public LiveData<CSVParser> getCsvParser() {
        return csvParser;
    }
    public LiveData<String> getFilePath() {
        return filePath;
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

    public LiveData<DateTimeFormatter> getDateFormatter() {
        return dateFormatter;
    }
}
