package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ContentResolver;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingWriteRepository;
import com.example.mtfinance.src.repositories.TransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    private TrackingWriteRepository trackingWriteRepository;

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
}
