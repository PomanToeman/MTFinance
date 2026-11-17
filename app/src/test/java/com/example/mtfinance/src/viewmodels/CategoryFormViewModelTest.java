package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Executor;

@ExtendWith(InstantExecutorExtension.class)
public class CategoryFormViewModelTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private TrackingRepository trackingWriteRepository;

    private CategoryFormViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new CategoryFormViewModel(categoryService, trackingService, trackingWriteRepository, synchronousExecutor);

        viewModel.getName().observeForever(s -> {});
        viewModel.getDescription().observeForever(s -> {});
        viewModel.getParentId().observeForever(l -> {});
        viewModel.getMonthlyBudget().observeForever(b -> {});
        viewModel.getMinimumBudget().observeForever(b -> {});
        viewModel.getType().observeForever(t -> {});
        viewModel.getIsRoot().observeForever(b -> {});
        viewModel.IsEditMode().observeForever(b -> {});
        viewModel.getFieldErrors().observeForever(m -> {});
    }

    @Test
    public void saveCategory_newCategory_insertsIntoRepository() {
        viewModel.setName("Utilities");
        viewModel.setDescription("Water and Electricity");
        viewModel.setMonthlyBudget(BigDecimal.valueOf(150.00));

        viewModel.saveCategory();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(trackingWriteRepository).insertCategory(categoryCaptor.capture());

        Category capturedCategory = categoryCaptor.getValue();
        assertAll(
            () -> assertEquals("Utilities", capturedCategory.getName()),
            () -> assertEquals("Water and Electricity", capturedCategory.getDescription()),
            () -> assertEquals(0, BigDecimal.valueOf(150.00).compareTo(capturedCategory.getMonthlyBudget())),
            () -> assertEquals(TrackingType.EXPENSE, capturedCategory.getType()),
            () -> assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void saveCategory_editCategory_updatesInRepository() {
        long categoryId = 1L;
        Category existingCategory = new Category("Old Name", "Old Desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        existingCategory.setCategoryId(categoryId);

        when(categoryService.categoryExists(categoryId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(categoryId)).thenReturn(existingCategory);
        when(categoryService.isRoot(existingCategory)).thenReturn(false);

        viewModel.setEditCategory(categoryId);

        viewModel.setName("New Name");
        viewModel.setMonthlyBudget(BigDecimal.valueOf(200));

        viewModel.saveCategory();

        verify(categoryService, atLeastOnce()).getCategoryByIdRestored(categoryId);
        verify(trackingWriteRepository).updateCategoryTree(existingCategory);

        assertAll(
            () -> assertEquals("New Name", existingCategory.getName()),
            () -> assertEquals(0, BigDecimal.valueOf(200).compareTo(existingCategory.getMonthlyBudget())),
            () -> assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void saveCategory_editCategory_keepingSameName_succeeds() {
        long categoryId = 1L;
        Category existingCategory = new Category("MyCategory", "Desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        existingCategory.setCategoryId(categoryId);

        when(categoryService.categoryExists(categoryId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(categoryId)).thenReturn(existingCategory);
        when(categoryService.categoryNameExists("MyCategory")).thenReturn(true);
        when(categoryService.isRoot(existingCategory)).thenReturn(false);

        viewModel.setEditCategory(categoryId);
        viewModel.setName("MyCategory");
        viewModel.setMonthlyBudget(BigDecimal.valueOf(150));

        viewModel.saveCategory();

        verify(trackingWriteRepository).updateCategoryTree(existingCategory);
        assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void saveCategory_editCategory_changingToExistingOtherName_failsValidation() {
        long categoryId = 1L;
        Category existingCategory = new Category("OriginalName", "Desc", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        existingCategory.setCategoryId(categoryId);

        when(categoryService.categoryExists(categoryId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(categoryId)).thenReturn(existingCategory);
        when(categoryService.categoryNameExists("OtherCategory")).thenReturn(true);
        when(categoryService.isRoot(existingCategory)).thenReturn(false);

        viewModel.setEditCategory(categoryId);
        viewModel.setName("OtherCategory");
        viewModel.setMonthlyBudget(BigDecimal.valueOf(150));

        viewModel.saveCategory();

        verify(trackingWriteRepository, never()).updateCategoryTree(any());
        Map<CategoryFormViewModel.CategoryErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertFalse(errors.get(CategoryFormViewModel.CategoryErrorFields.NAME).isEmpty())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "0", "0.00"})
    public void saveCategory_bvaInvalidBudget_failsValidation(String invalidAmount) {
        viewModel.setName("Valid Name");
        viewModel.setMonthlyBudget(new BigDecimal(invalidAmount));

        viewModel.saveCategory();

        verify(trackingWriteRepository, never()).insertCategory(any());
        Map<CategoryFormViewModel.CategoryErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertFalse(errors.get(CategoryFormViewModel.CategoryErrorFields.MONTHLY_BUDGET).isEmpty())
        );
    }

    @Test
    public void saveCategory_bvaValidBudget_succeeds() {
        viewModel.setName("Valid Name");
        viewModel.setMonthlyBudget(new BigDecimal("0.01"));

        viewModel.saveCategory();

        verify(trackingWriteRepository).insertCategory(any());
        assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue());
    }

    @Test
    public void saveCategory_emptyName_populatesFieldErrorAndFailsValidation() {
        viewModel.setName("");
        viewModel.setMonthlyBudget(BigDecimal.TEN);
        viewModel.saveCategory();

        verify(trackingWriteRepository, never()).insertCategory(any());
        Map<CategoryFormViewModel.CategoryErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertFalse(errors.get(CategoryFormViewModel.CategoryErrorFields.NAME).isEmpty()),
            () -> assertEquals("", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void saveCategory_duplicateName_populatesFieldError() {
        String duplicateName = "Groceries";
        when(categoryService.categoryNameExists(duplicateName)).thenReturn(true);

        viewModel.setName(duplicateName);
        viewModel.setMonthlyBudget(BigDecimal.valueOf(100));

        viewModel.saveCategory();

        verify(trackingWriteRepository, never()).insertCategory(any());
        Map<CategoryFormViewModel.CategoryErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertFalse(errors.get(CategoryFormViewModel.CategoryErrorFields.NAME).isEmpty())
        );
    }

    @Test
    public void validateForm_invalidParentId_populatesParentIdFieldError() {
        long invalidParentId = 99L;
        when(categoryService.categoryExists(invalidParentId)).thenReturn(false);

        viewModel.setName("Valid Name");
        viewModel.setMonthlyBudget(BigDecimal.TEN);
        viewModel.setParentId(invalidParentId);

        viewModel.saveCategory();

        verify(trackingWriteRepository, never()).insertCategory(any());
        Map<CategoryFormViewModel.CategoryErrorFields, String> errors = viewModel.getFieldErrors().getValue();
        assertAll(
            () -> assertNotNull(errors),
            () -> assertFalse(errors.get(CategoryFormViewModel.CategoryErrorFields.PARENT_ID).isEmpty())
        );
    }

    @Test
    public void clear_clearsAllFieldErrors() {
        viewModel.setName("");
        viewModel.saveCategory();
        assertTrue(viewModel.hasFieldErrors());

        viewModel.clear();
        assertFalse(viewModel.hasFieldErrors());
    }

    @Test
    public void deleteCategory_validCategory_callsRepositoryDelete() {
        long categoryId = 5L;
        Category category = new Category("To Delete", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        category.setCategoryId(categoryId);

        when(categoryService.categoryExists(categoryId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(categoryId)).thenReturn(category);

        viewModel.setEditCategory(categoryId);

        viewModel.deleteCategory(true);

        verify(trackingWriteRepository).deleteCategory(categoryId, true);
        assertAll(
            () -> assertEquals("Category deleted successfully", viewModel.getSuccessMessage().getValue()),
            () -> assertNull(viewModel.getEditCategoryId())
        );
    }

    @Test
    public void deleteCategory_noCategory_setsErrorMessage() {
        viewModel.deleteCategory(false);

        assertTrue(viewModel.getErrorMessage().getValue().contains("No category to delete"));
    }
}
