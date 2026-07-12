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
        // Default mock for the repository call in the constructor
        MutableLiveData<List<CategoryWithTransactions>> liveData = new MutableLiveData<>();
        liveData.setValue(new ArrayList<>());
        when(trackingRepository.getAllCategoriesWithTransactions()).thenReturn(liveData);
    }

    @Test
    public void constructor_loadsCategoriesFromRepository() {
        // Act
        viewModel = new CategoryViewModel(trackingRepository);
        
        // Assert
        verify(trackingRepository).getAllCategoriesWithTransactions();
        assertEquals(new ArrayList<>(), viewModel.getAllCategories().getValue());
    }

    @Test
    public void setSearchQuery_updatesSearchQueryLiveData() {
        // Arrange
        viewModel = new CategoryViewModel(trackingRepository);
        String query = "  Groceries  ";

        // Act
        viewModel.setSearchQuery(query);

        // Assert
        assertEquals("Groceries", viewModel.getSearchQuery().getValue());
    }

    @Test
    public void filterCategories_withValidQuery_callsRepositorySearch() {
        // Arrange
        String query = "Food";
        List<CategoryWithTransactions> searchResults = new ArrayList<>();
        MutableLiveData<List<CategoryWithTransactions>> repoLiveData = new MutableLiveData<>();
        repoLiveData.setValue(searchResults);

        // Mock repository search
        when(trackingRepository.searchCategories(query)).thenReturn(repoLiveData);

        viewModel = new CategoryViewModel(trackingRepository);
        viewModel.setSearchQuery(query);



        // Assert
        verify(trackingRepository).searchCategories(query);
        assertEquals(searchResults, viewModel.getFilteredCategories().getValue());
    }

    @Test
    public void filterCategories_withEmptyQuery_returnsAllCategories() {
        // Arrange
        List<CategoryWithTransactions> allCats = new ArrayList<>();
        // Mocking the initial load in setUp handles the constructor.
        // But we want to ensure allCategories is populated.

        viewModel = new CategoryViewModel(trackingRepository);
        viewModel.setSearchQuery("");



        // Assert
        assertEquals(allCats, viewModel.getFilteredCategories().getValue());
    }
}
