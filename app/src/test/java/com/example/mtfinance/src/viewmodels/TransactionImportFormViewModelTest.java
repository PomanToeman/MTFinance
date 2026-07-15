package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.mtfinance.src.repositories.TrackingRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TransactionImportFormViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private TrackingRepository trackingRepository;

    private TransactionImportFormViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new TransactionImportFormViewModel(trackingRepository);
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

    @Test
    public void readTransactionFile_validFile_loadsHeaders() throws IOException {
        // Arrange
        File tempFile = temporaryFolder.newFile("transactions.csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Date,Name,Amount,Description\n");
            writer.write("2023-01-01,Milk,3.50,Grocery\n");
        }
        
        viewModel.setFilePath(tempFile.getAbsolutePath());

        // Act
        viewModel.readTransactionFile();

        // Assert
        assertNotNull(viewModel.getCsvParser().getValue());
        assertNotNull(viewModel.getCsvHeaders().getValue());
        assertEquals(4, viewModel.getCsvHeaders().getValue().size());
        assertTrue(viewModel.getCsvHeaders().getValue().contains("Name"));
        assertTrue(viewModel.getCsvHeaders().getValue().contains("Amount"));
        assertEquals("", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void readTransactionFile_nonExistentFile_setsErrorMessage() {
        // Arrange
        viewModel.setFilePath("non_existent_file.csv");

        // Act
        viewModel.readTransactionFile();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("Invalid Transaction File"));
    }

    @Test
    public void setHeaders_onlyValidHeadersAccepted() throws IOException {
        // Arrange
        File tempFile = temporaryFolder.newFile("test.csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Header1,Header2\n");
        }
        viewModel.setFilePath(tempFile.getAbsolutePath());
        viewModel.readTransactionFile();

        // Act
        viewModel.setNameHeader("Header1");
        viewModel.setNameHeader("InvalidHeader");

        // Assert
        assertEquals("Header1", viewModel.getNameHeader().getValue());
    }
}
