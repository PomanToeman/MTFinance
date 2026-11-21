package com.example.mtfinance.screens.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.DefaultColumn
import com.example.mtfinance.screens.DeleteConfirmationDialog
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.LoadingDialog
import com.example.mtfinance.screens.lists.*
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.viewmodels.CategoryFormViewModel

/**
 * The UI screen that allows the user to create and edit categories in the database.
 */
@Composable
fun CategoryFormScreen(categoryFormViewModel: CategoryFormViewModel = hiltViewModel(), navHostController: NavHostController, categoryId: Long? = null) {
    val categoryName by categoryFormViewModel.name.observeAsState()
    val categoryDescription by categoryFormViewModel.description.observeAsState()
    val categoryMonthlyBudget by categoryFormViewModel.monthlyBudget.observeAsState()
    val categoryType by categoryFormViewModel.type.observeAsState()
    val categoryParent by categoryFormViewModel.cachedParentCategory.observeAsState()
    val errorMap by categoryFormViewModel.fieldErrors.observeAsState()
    val editMode by categoryFormViewModel.IsEditMode().observeAsState()
    val successMessage by categoryFormViewModel.successMessage.observeAsState()
    val errorMessage by categoryFormViewModel.errorMessage.observeAsState()
    val isLoading by categoryFormViewModel.isLoading.observeAsState()
    val categorySelection by categoryFormViewModel.categorySelection.observeAsState()
    val isRoot by categoryFormViewModel.isRoot.observeAsState()
    var deleteTransactions by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }


    // edit mode if categoryId is not null
    LaunchedEffect(categoryId) {
        categoryFormViewModel.setEditCategory(categoryId)
    }

    DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Header(MessageCli.CATEGORY_FORM_HEADER.getMessage())
        Text("Edit Mode: $editMode")
        TextFieldForm(
            "Name", categoryName,
            onValueChange = { categoryFormViewModel.setName(it) },
            minLines = 1, maxLines = 1,
            enabled = isRoot == false,
            isError = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.NAME)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.NAME)
        )
        TextFieldForm(
            "Description", categoryDescription,
            onValueChange = { categoryFormViewModel.setDescription(it) },
            minLines = 3, maxLines = 3,
            placeholder = TrackingUtlis.EMPTY_DESCRIPTION,
            enabled = isRoot == false,
            isError = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.DESCRIPTION)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.DESCRIPTION)
        )
        NumberFieldForm(
            "Monthly Budget", categoryMonthlyBudget,
            setter = { categoryFormViewModel.setMonthlyBudget(it) },
            isError = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.MONTHLY_BUDGET)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.MONTHLY_BUDGET)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRoot == false) {
                val rootCategory = remember(categorySelection) {
                    categorySelection?.firstOrNull { it?.category?.parentId == null }?.category
                }
                val effectiveParent = categoryParent ?: rootCategory
                val chosenParentList = if (effectiveParent != null) listOf(effectiveParent) else emptyList()

                CategorySelectionBox(
                    label = "Parent Category",
                    chosenCategories = chosenParentList,
                    categorySelection = categorySelection,
                    buttonLabel = "Set Parent",
                    emptyMessage = "No parent category selected",
                    isError = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.PARENT_ID)?.isNotEmpty() == true,
                    errorMessage = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.PARENT_ID),
                    onSelect = { categoryFormViewModel.setParentId(it) },
                    selectLabel = "Set Parent",
                    onRemove = if (categoryParent != null) { { categoryFormViewModel.setParentId(null) } } else null,
                    removeLabel = "Reset to Root",
                    dismissOnSelection = true,
                    enabled = true
                )
            } else {
                Text("Parent: None (Root Category)")
            }

            Text("Type: " + categoryType.toString().lowercase())
            Button(onClick = { categoryFormViewModel.saveCategory() }) {
                Text("Save")
            }
            if (isLoading == true) {
                LoadingDialog()
            }

            if (successMessage != null) {
                Text(successMessage!!, color = Color.Green)
            }
            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red)
            }

            if (editMode == true && isRoot == false) {

                if (deleteConfirmation) {
                    DeleteConfirmationDialog(
                        onDismiss = { deleteConfirmation = false },
                        onConfirm = { categoryFormViewModel.deleteCategory(deleteTransactions) },
                        onCancel = { deleteConfirmation = false },
                        message = MessageCli.DELETE_BUTTON_CONFIRMATION.getMessage(categoryName),
                        content = {
                            Row() {
                                Text(
                                    text = "delete transactions",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Checkbox(
                                    checked = deleteTransactions,
                                    onCheckedChange = { deleteTransactions = it }
                                )
                            }
                        }
                    )
                }

                Button(onClick = { deleteConfirmation = true }) {
                    Text("Delete")
                }
            }
            else if (isRoot == false) {
                Button(onClick = { categoryFormViewModel.clear() }) {
                    Text("Clear")
                }
            }

            Button(onClick = { navHostController.navigate("category") }) {
                Text("Back")
            }
        }
    }
}
