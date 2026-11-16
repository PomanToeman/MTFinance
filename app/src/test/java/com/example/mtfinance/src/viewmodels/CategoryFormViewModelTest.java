package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.CategoryService;
import com.example.mtfinance.src.repositories.TrackingService;
import com.example.mtfinance.src.repositories.TrackingRepository;
import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
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
    public void saveCategory_multipleCategories_insertsAll() {
        String[] names = {"Groceries", "Entertainment", "Transport"};
        BigDecimal[] budgets = {BigDecimal.valueOf(400), BigDecimal.valueOf(200), BigDecimal.valueOf(150)};

        for (int i = 0; i < names.length; i++) {
            viewModel.clear();
            viewModel.setName(names[i]);
            viewModel.setMonthlyBudget(budgets[i]);
            viewModel.saveCategory();
        }

        verify(trackingWriteRepository, times(3)).insertCategory(any(Category.class));
    }

    @Test
    public void saveCategory_emptyName_failsValidation() {
        viewModel.setName("");
        viewModel.saveCategory();

        assertAll(
            () -> assertTrue(viewModel.getErrorMessage().getValue().contains("Name cannot be empty")),
            () -> assertEquals("", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void setMonthlyBudget_atExactMinimum_succeeds() {
        Category parent = new Category("Parent", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        Category child = new Category("Child", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        child.setParent(parent);
        parent.setCategoryId(10L);

        when(categoryService.categoryExists(10L)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(10L)).thenReturn(parent);

        viewModel.setEditCategory(10L);

        viewModel.setMonthlyBudget(BigDecimal.valueOf(50));

        assertAll(
            () -> assertEquals(0, BigDecimal.valueOf(50).compareTo(viewModel.getMonthlyBudget().getValue())),
            () -> assertEquals("", viewModel.getErrorMessage().getValue())
        );
    }

    @Test
    public void saveCategory_childBudgetIncrease_propagatesToParent() {
        long parentId = 10L;
        long childId = 20L;
        Category parent = new Category("Parent", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        parent.setCategoryId(parentId);

        Category child = new Category("Child", "", BigDecimal.valueOf(50), TrackingType.EXPENSE);
        child.setCategoryId(childId);
        child.setParent(parent);

        when(categoryService.categoryExists(parentId)).thenReturn(true);
        when(categoryService.categoryExists(childId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(parentId)).thenReturn(parent);
        when(categoryService.getCategoryByIdRestored(childId)).thenReturn(child);
        when(categoryService.isRoot(child)).thenReturn(false);

        viewModel.setEditCategory(childId);

        viewModel.setMonthlyBudget(BigDecimal.valueOf(150));
        viewModel.saveCategory();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(trackingWriteRepository).updateCategoryTree(captor.capture());

        Category savedChild = captor.getValue();
        assertAll(
            () -> assertEquals(0, BigDecimal.valueOf(150).compareTo(savedChild.getMonthlyBudget())),
            () -> assertEquals(0, BigDecimal.valueOf(150).compareTo(savedChild.getParent().getMonthlyBudget()))
        );
    }

    @Test
    public void saveCategory_withParent_setsParentId() {
        long parentId = 5L;
        Category parent = new Category("Parent", "", BigDecimal.valueOf(500), TrackingType.EXPENSE);
        parent.setCategoryId(parentId);

        when(categoryService.categoryExists(parentId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(parentId)).thenReturn(parent);

        viewModel.setName("Child");
        viewModel.setParentId(parentId);
        viewModel.setMonthlyBudget(BigDecimal.TEN);

        viewModel.saveCategory();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(trackingWriteRepository).insertCategory(captor.capture());
        assertEquals(Long.valueOf(parentId), captor.getValue().getParentId());
    }

    @Test
    public void hierarchyChange_siblingToChild() {
        Category catA = new Category("A", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        catA.setCategoryId(1L);
        Category catB = new Category("B", "", BigDecimal.valueOf(100), TrackingType.EXPENSE);
        catB.setCategoryId(2L);

        when(categoryService.categoryExists(1L)).thenReturn(true);
        when(categoryService.categoryExists(2L)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(1L)).thenReturn(catA);
        when(categoryService.getCategoryByIdRestored(2L)).thenReturn(catB);
        when(categoryService.isRoot(catB)).thenReturn(false);

        viewModel.setEditCategory(2L);
        viewModel.setParentId(1L);
        viewModel.saveCategory();

        verify(trackingWriteRepository).updateCategoryTree(catB);
        assertEquals(Long.valueOf(1L), catB.getParentId());
    }

    @Test
    public void editRoot_allowsBudgetUpdate() {
        long rootId = 0L;
        Category root = new Category("Root", "", BigDecimal.valueOf(1000), TrackingType.EXPENSE);
        root.setCategoryId(rootId);
        when(categoryService.categoryExists(rootId)).thenReturn(true);
        when(categoryService.getCategoryByIdRestored(rootId)).thenReturn(root);
        when(categoryService.isRoot(root)).thenReturn(true);

        viewModel.setEditCategory(rootId);
        viewModel.setMonthlyBudget(BigDecimal.valueOf(1200));
        viewModel.saveCategory();

        verify(trackingWriteRepository).updateRootCategoryBudget(rootId, BigDecimal.valueOf(1200));
        assertAll(
            () -> assertEquals("Category saved successfully", viewModel.getSuccessMessage().getValue())
        );
    }

    @Test
    public void saveCategory_duplicateName_failsValidation() {
        String duplicateName = "Groceries";
        when(categoryService.categoryNameExists(duplicateName)).thenReturn(true);

        viewModel.setName(duplicateName);
        viewModel.setMonthlyBudget(BigDecimal.valueOf(100));

        viewModel.saveCategory();

        assertTrue(viewModel.getErrorMessage().getValue().contains("already exists"));
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
