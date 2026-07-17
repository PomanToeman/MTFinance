package com.example.mtfinance.src.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

public class CategoryFormViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private TrackingRepository trackingRepository;

    private CategoryFormViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new CategoryFormViewModel(trackingRepository);
    }

    @Test
    public void saveCategory_newCategory_insertsIntoRepository() {
        viewModel.setName("Utilities");
        viewModel.setDescription("Water and Electricity");
        viewModel.setMonthlyBudget(BigDecimal.valueOf(150.00));

        viewModel.saveCategory();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(trackingRepository).insertCategory(categoryCaptor.capture());
        
        Category capturedCategory = categoryCaptor.getValue();
        assertEquals("Utilities", capturedCategory.getName());
        assertEquals("Water and Electricity", capturedCategory.getDescription());
        assertEquals(0, BigDecimal.valueOf(150.00).compareTo(capturedCategory.getMonthlyBudget()));
        assertEquals(TrackingType.EXPENSE, capturedCategory.getType());
        assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void saveCategory_editCategory_updatesInRepository() {
        long categoryId = 1L;
        Category existingCategory = new Category("Old Name", "Old Desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        existingCategory.setCategoryId(categoryId);
        
        when(trackingRepository.categoryExists(categoryId)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(categoryId)).thenReturn(existingCategory);
        
        viewModel.setEditCategory(categoryId);
        
        viewModel.setName("New Name");
        viewModel.setMonthlyBudget(BigDecimal.valueOf(200));

        viewModel.saveCategory();

        verify(trackingRepository, atLeastOnce()).getCategoryByIdRestored(categoryId);
        verify(trackingRepository).updateCategoryTree(existingCategory);
        
        assertEquals("New Name", existingCategory.getName());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(existingCategory.getMonthlyBudget()));
        assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void saveCategory_multipleCategories_insertsAll() {
        String[] names = {"Groceries", "Entertainment", "Transport"};
        BigDecimal[] budgets = {BigDecimal.valueOf(400), BigDecimal.valueOf(200), BigDecimal.valueOf(150)};

        for (int i = 0; i < names.length; i++) {
            viewModel.clear();
            viewModel.setName(names[i]);
            viewModel.setMonthlyBudget(budgets[i]);
            viewModel.saveCategory();
        }

        verify(trackingRepository, times(3)).insertCategory(any(Category.class));
    }

    @Test
    public void saveCategory_emptyName_failsValidation() {
        viewModel.setName("");
        viewModel.saveCategory();

        assertTrue(viewModel.getErrorMessage().getValue().contains("Name cannot be empty"));
        assertEquals("", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void setMonthlyBudget_respectsMinimum() {
        Category parent = new Category("Parent", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category child = new Category("Child", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        child.setParent(parent);
        parent.setCategoryId(10L);
        
        when(trackingRepository.categoryExists(10L)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(10L)).thenReturn(parent);
        
        viewModel.setEditCategory(10L);
        
        viewModel.setMonthlyBudget(BigDecimal.valueOf(20));

        assertEquals(0, BigDecimal.valueOf(50).compareTo(viewModel.getMonthlyBudget().getValue()));
    }

    @Test
    public void saveCategory_withParent_setsParentId() {
        long parentId = 5L;
        Category parent = new Category("Parent", "", BigDecimal.valueOf(500), TrackingType.EXPENSE);
        parent.setCategoryId(parentId);
        
        when(trackingRepository.categoryExists(parentId)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(parentId)).thenReturn(parent);
        
        viewModel.setName("Child");
        viewModel.setParentId(parentId);

        viewModel.saveCategory();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(trackingRepository).insertCategory(captor.capture());
        assertEquals(Long.valueOf(parentId), captor.getValue().getParentId());
    }

    @Test
    public void hierarchyChange_siblingToChild() {
        // 1. Create two sibling categories
        Category catA = new Category("A", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        catA.setCategoryId(1L);
        Category catB = new Category("B", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        catB.setCategoryId(2L);

        when(trackingRepository.categoryExists(1L)).thenReturn(true);
        when(trackingRepository.categoryExists(2L)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(1L)).thenReturn(catA);
        when(trackingRepository.getCategoryByIdRestored(2L)).thenReturn(catB);

        // 2. Edit B to make A its parent
        viewModel.setEditCategory(2L);
        viewModel.setParentId(1L);
        viewModel.saveCategory();

        verify(trackingRepository).updateCategoryTree(catB);
        assertEquals(Long.valueOf(1L), catB.getParentId());
    }

    @Test
    public void editRoot_shouldFail() {
        Category root = new Category("Root", "", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
        root.setCategoryId(0L);
        when(trackingRepository.categoryExists(0L)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(0L)).thenReturn(root);
        when(trackingRepository.isRoot(root)).thenReturn(true);

        viewModel.setEditCategory(0L);

        assertTrue(viewModel.getErrorMessage().getValue().contains("root"));
    }

    @Test
    public void tenValidEdits_verifyEachUpdate() {
        long id = 1L;
        Category cat = new Category("Name", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        cat.setCategoryId(id);
        when(trackingRepository.categoryExists(id)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(id)).thenReturn(cat);
        
        viewModel.setEditCategory(id);

        for (int i = 1; i <= 10; i++) {
            String newName = "Edit " + i;
            BigDecimal newBudget = BigDecimal.valueOf(100 + i);
            viewModel.setName(newName);
            viewModel.setMonthlyBudget(newBudget);
            
            viewModel.saveCategory();

            verify(trackingRepository, times(i)).updateCategoryTree(cat);
            assertEquals(newName, cat.getName());
            assertEquals(0, newBudget.compareTo(cat.getMonthlyBudget()));
            assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue());
        }
    }

    @Test
    public void saveCategory_duplicateName_failsValidation() {
        // Arrange
        String duplicateName = "Groceries";
        when(trackingRepository.categoryNameExists(duplicateName)).thenReturn(true);
        
        viewModel.setName(duplicateName);
        viewModel.setMonthlyBudget(BigDecimal.valueOf(100));

        // Act
        viewModel.saveCategory();

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("already exists"));
    }

    @Test
    public void deleteCategory_validCategory_callsRepositoryDelete() {
        // Arrange
        long categoryId = 5L;
        Category category = new Category("To Delete", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        category.setCategoryId(categoryId);
        
        when(trackingRepository.categoryExists(categoryId)).thenReturn(true);
        when(trackingRepository.getCategoryByIdRestored(categoryId)).thenReturn(category);
        
        viewModel.setEditCategory(categoryId);

        // Act
        viewModel.deleteCategory(true);

        // Assert
        verify(trackingRepository).deleteCategory(categoryId, true);
        assertEquals("Category deleted successfully", viewModel.getSuccessMessage().getValue());
        assertNull(viewModel.getEditCategoryId());
        assertEquals("Name", viewModel.getName().getValue()); // Verify clear() was called
    }

    @Test
    public void deleteCategory_noCategory_setsErrorMessage() {
        // Act
        viewModel.deleteCategory(false);

        // Assert
        assertTrue(viewModel.getErrorMessage().getValue().contains("No category to delete"));
    }
}
