package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ContentResolver;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.Executor;

@ExtendWith(InstantExecutorExtension.class)
public class TransactionImportFormViewModelTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TrackingRepository trackingWriteRepository;

    @Mock
    private Application application;

    @Mock
    private ContentResolver contentResolver;

    private TransactionImportFormViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(application.getContentResolver()).thenReturn(contentResolver);
        viewModel = new TransactionImportFormViewModel(categoryService, trackingService, transactionService, trackingWriteRepository, synchronousExecutor, application);
        viewModel.getFieldErrors().observeForever(m -> {});
    }

    @Test
    public void clear_resetsAllFields() {
        viewModel.setFilePath("some/path.csv");
        viewModel.setNameHeader("Name");

        viewModel.clear();

        assertAll(
            () -> assertEquals("", viewModel.getFilePath().getValue()),
            () -> assertEquals("", viewModel.getNameHeader().getValue()),
            () -> assertTrue(viewModel.getCsvHeaders().getValue().isEmpty())
        );
    }

    @Test
    public void importTransaction_missingHeadersAndFile_populatesFieldErrors() {
        viewModel.importTransaction();

        Map<TransactionImportFormViewModel.TransactionImportErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertTrue(errors.get(TransactionImportFormViewModel.TransactionImportErrorFields.FILE_SOURCE).contains("CSV Parser is not initialized")),
            () -> assertTrue(errors.get(TransactionImportFormViewModel.TransactionImportErrorFields.NAME_HEADER).contains("Name header is missing")),
            () -> assertTrue(errors.get(TransactionImportFormViewModel.TransactionImportErrorFields.AMOUNT_HEADER).contains("Amount header is missing")),
            () -> assertTrue(errors.get(TransactionImportFormViewModel.TransactionImportErrorFields.DATE_HEADER).contains("Date header is missing"))
        );
    }

    @Test
    public void clear_clearsAllImportFieldErrors() {
        viewModel.importTransaction();
        assertTrue(viewModel.hasFieldErrors());

        viewModel.clear();
        assertFalse(viewModel.hasFieldErrors());
    }

    @Test
    public void setFilePath_nonCsvFile_populatesFieldError() {
        viewModel.setFilePath("some/path/document.pdf");

        Map<TransactionImportFormViewModel.TransactionImportErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertTrue(errors.get(TransactionImportFormViewModel.TransactionImportErrorFields.FILE_SOURCE).contains("Only CSV files"))
        );
    }
}
