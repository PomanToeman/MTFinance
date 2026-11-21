package com.example.mtfinance.screens.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.DeleteConfirmationDialog
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.LoadingDialog
import com.example.mtfinance.screens.theme.SaveStatusColor
import com.example.mtfinance.screens.theme.getSaveStatusThemeColor
import com.example.mtfinance.src.CurrencyFormat
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.messages.GeneralMessages
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.viewmodels.CategoryFormViewModel

/**
 * The UI screen that allows the user to create and edit categories in the database.
 */
@Composable
fun CategoryFormScreen(
    categoryFormViewModel: CategoryFormViewModel = hiltViewModel(),
    navHostController: NavHostController,
    categoryId: Long? = null
) {
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
    val currencyFormat by categoryFormViewModel.currencyFormat.observeAsState(CurrencyFormat.NZD)
    val currencySymbol = currencyFormat?.symbol ?: "$"

    var deleteTransactions by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId) {
        categoryFormViewModel.setEditCategory(categoryId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FORM_SPACING_UNIT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FORM_SPACING_UNIT)
    ) {
        Header(if (editMode == true) "Edit Category" else "New Category")

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
            currencySymbol = currencySymbol,
            isError = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.MONTHLY_BUDGET)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(CategoryFormViewModel.CategoryErrorFields.MONTHLY_BUDGET)
        )

        OutlinedTextField(
            value = categoryType?.toString()?.lowercase() ?: "expense",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Type") },
            supportingText = { Text("Category type is read-only", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth()
        )

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
            Text("Parent: None (Root Category)", style = MaterialTheme.typography.bodyMedium)
        }

        if (isLoading == true) {
            LoadingDialog()
        }

        if (!successMessage.isNullOrBlank()) {
            Text(successMessage!!, color = getSaveStatusThemeColor(SaveStatusColor.GREEN), style = MaterialTheme.typography.bodyMedium)
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(FORM_SPACING_UNIT))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FORM_SPACING_UNIT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { categoryFormViewModel.saveCategory() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(ButtonMessages.SAVE.getMessage())
            }

            if (editMode == true && isRoot == false) {
                Button(
                    onClick = { deleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(ButtonMessages.DELETE.getMessage())
                }
            } else if (isRoot == false) {
                Button(
                    onClick = { categoryFormViewModel.clear() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(ButtonMessages.CLEAR.getMessage())
                }
            }
        }

        if (deleteConfirmation) {
            DeleteConfirmationDialog(
                onDismiss = { deleteConfirmation = false },
                onConfirm = { categoryFormViewModel.deleteCategory(deleteTransactions) },
                onCancel = { deleteConfirmation = false },
                message = GeneralMessages.DELETE_BUTTON_CONFIRMATION.getMessage(categoryName),
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(onClick = { navHostController.navigate("category") }) {
                Text(ButtonMessages.BACK.getMessage())
            }
        }
    }
}
