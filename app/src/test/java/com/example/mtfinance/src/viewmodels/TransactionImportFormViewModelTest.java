package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ContentResolver;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingWriteRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.Executor;

public class TransactionImportFormViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private TrackingWriteRepository trackingWriteRepository;

    @Mock
    private Application application;

    @Mock
    private ContentResolver contentResolver;

    private TransactionImportFormViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(application.getContentResolver()).thenReturn(contentResolver);
        viewModel = new TransactionImportFormViewModel(categoryService, trackingService, trackingWriteRepository, synchronousExecutor, application);
    }

    @Test
    public void clear_resetsAllFields() {
        viewModel.setFilePath("some/path.csv");
        viewModel.setNameHeader("Name");

        viewModel.clear();

        assertEquals("", viewModel.getFilePath().getValue());
        assertEquals("", viewModel.getNameHeader().getValue());
        assertTrue(viewModel.getCsvHeaders().getValue().isEmpty());
    }
}
