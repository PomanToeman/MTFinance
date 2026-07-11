package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class CategoryViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private TrackingRepository trackingRepository;

    private CategoryViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void constructor_loadsCategoriesFromRepository() {
        // Arrange
        List<CategoryWithTransactions> expectedCategories = new ArrayList<>();
        MutableLiveData<List<CategoryWithTransactions>> liveData = new MutableLiveData<>();
        liveData.setValue(expectedCategories);
        when(trackingRepository.getAllCategoriesWithTransactions()).thenReturn(liveData);

        // Act
        viewModel = new CategoryViewModel(trackingRepository);
        
        // Assert
        verify(trackingRepository).getAllCategoriesWithTransactions();
        assertEquals(expectedCategories, viewModel.getAllCategories().getValue());
    }
}
