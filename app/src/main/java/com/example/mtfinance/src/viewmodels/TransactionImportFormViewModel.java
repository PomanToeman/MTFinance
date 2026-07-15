package com.example.mtfinance.src.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mtfinance.src.repositories.TrackingRepository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    private final MutableLiveData<String> nameHeader = new MutableLiveData<>();
    private final MutableLiveData<String> amountHeader = new MutableLiveData<>();
    private final MutableLiveData<String> dateHeader = new MutableLiveData<>();


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
            setCsvHeaders(csvParser.getHeaderNames());


        }
        catch (IOException e) {
            setErrorMessage("Invalid Transaction File: " + e.getMessage());
        }
        catch (Exception e) {
            setErrorMessage("Couldn't read transaction File: " + e.getMessage());
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
    public LiveData<Boolean> getAlwaysSendToRoot() {
        return alwaysSendToRoot;
    }

}
