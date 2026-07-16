package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
    private final MutableLiveData<DateTimeFormatter> dateFormatter = new MutableLiveData<>();

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> successfulImports = new MutableLiveData<>();


    // TODO - add auto-sorting Categories later.
    private final MutableLiveData<Boolean> alwaysSendToRoot = new MutableLiveData<>(Boolean.TRUE);

    private final MutableLiveData<List<String>> csvHeaders = new MutableLiveData<>();


    @Inject
    public TransactionImportFormViewModel(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
        clear();
    }



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


    public void setNameHeader(String nameHeader) {
        if (this.csvHeaders.getValue().contains(nameHeader)) {
            this.nameHeader.setValue(nameHeader);
        }
    }

    public void setAmountHeader(String amountHeader) {
        if (this.csvHeaders.getValue().contains(amountHeader)) {
            this.amountHeader.setValue(amountHeader);
        }
    }

    public void setDateHeader(String dateHeader) {
        if (this.csvHeaders.getValue().contains(dateHeader)) {
            this.dateHeader.setValue(dateHeader);
        }
    }

    public void setDateFormatter(String dateFormatter) {
        try {
            this.dateFormatter.setValue(DateTimeFormatter.ofPattern(dateFormatter));
        }
        catch (Exception e) {
            setErrorMessage("Invalid Date Formatter: " + e.getMessage());
            this.dateFormatter.setValue(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
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
        this.csvHeaders.setValue(new ArrayList<>());
        this.isLoading.setValue(Boolean.FALSE);
        this.successfulImports.setValue(new ArrayList<>());
        this.alwaysSendToRoot.setValue(Boolean.TRUE);
        this.dateFormatter.setValue(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    }


    /**
     * Reads the CSV file and headers and phases to prepare for importing
     * You must do this before importing!
     *
     */

    public void readTransactionFile() {

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
            setErrorMessage("Invalid Transaction File: " + e.getMessage());
        }
        catch (Exception e) {
            setErrorMessage("Couldn't read transaction File: " + e.getMessage());
        }


    }

    /**
     * This will import a csv transaction file for automatic insertion in database.
     *
     *
     */
    public void importTransaction() {
        try {
            validateImport();
            setIsLoading(Boolean.TRUE);
            TransactionFormViewModel transactionForm = new TransactionFormViewModel(this.trackingRepository);
            CSVParser csvParser = this.csvParser.getValue();
            String nameHeader = this.nameHeader.getValue();
            String dateHeader = this.dateHeader.getValue();
            String amountHeader = this.amountHeader.getValue();
            DateTimeFormatter dateFormatter = this.dateFormatter.getValue();
            List<String> successfulImports = new ArrayList<>();


            // imports each record (if possible)

            for (CSVRecord record : csvParser) {
                try {
                    transactionForm.setName(record.get(nameHeader));
                    BigDecimal amount = new BigDecimal(record.get(amountHeader));
                    TrackingType type = TrackingUtlis.determineTypeByAmount(amount);
                    transactionForm.setAmount(amount.abs());
                    transactionForm.setType(type);
                    transactionForm.setDate(LocalDate.parse(record.get(dateHeader), dateFormatter));

                    if (Boolean.TRUE.equals(alwaysSendToRoot.getValue())) {
                        Long categoryId = trackingRepository.getRootCategoryByType(type).getCategoryId();
                        transactionForm.addCategoryId(categoryId);
                    }
                    else {
                        transactionForm.addCategoryId(1L);
                    }

                    // create and insert instance.
                    transactionForm.saveTransaction();

                    // check for success
                    if (!transactionForm.getSuccessMessage().getValue().isEmpty()) {
                        successfulImports.add(record.toString());

                    }


                } catch (Exception e) {
                    // just skip transaction

                }
                finally {
                    transactionForm.clear();

                }
            }

            this.successfulImports.setValue(successfulImports);



            setSuccessMessage(successfulImports.size() + " Transaction/s successfully imported!");
        }
        catch (IllegalArgumentException e) {
            setErrorMessage("Form is not complete:" + e.getMessage());
        } catch (Exception e) {
            setErrorMessage("Seems import stopped working.");
        }
        finally {
            setIsLoading(Boolean.FALSE);
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
            throw new IllegalArgumentException("");
        }
        if (nameHeader.getValue() == null || nameHeader.getValue().isEmpty()) {
            throw new IllegalArgumentException("");
        }
        if (dateHeader.getValue() == null || dateHeader.getValue().isEmpty()) {
            throw new IllegalArgumentException("");
        }
        if (amountHeader.getValue() == null || amountHeader.getValue().isEmpty()) {
            throw new IllegalArgumentException("");
        }
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

    public LiveData<DateTimeFormatter> getDateFormatter() {
        return dateFormatter;
    }
}
