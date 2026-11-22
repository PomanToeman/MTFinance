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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.mtfinance.screens.ChooseTypeForm
import com.example.mtfinance.screens.DeleteConfirmationDialog
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.LoadingDialog
import com.example.mtfinance.screens.theme.SaveStatusColor
import com.example.mtfinance.screens.theme.getSaveButtonColors
import com.example.mtfinance.screens.theme.getSaveStatusThemeColor
import com.example.mtfinance.src.CurrencyFormat
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.messages.GeneralMessages
import com.example.mtfinance.src.trackingengine.TrackingType
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.viewmodels.TransactionFormViewModel

/**
 * The UI screen that allows the user to create and edit transactions in the database.
 */
@Composable
fun TransactionFormScreen(
    transactionFormViewModel: TransactionFormViewModel = hiltViewModel(),
    navHostController: NavHostController,
    transactionId: Long? = null
) {
    val transactionName by transactionFormViewModel.name.observeAsState()
    val transactionAmount by transactionFormViewModel.amount.observeAsState()
    val transactionDate by transactionFormViewModel.date.observeAsState()
    val transactionType by transactionFormViewModel.type.observeAsState()
    val systemDateFormat by transactionFormViewModel.systemDateFormat.observeAsState(DateFormat.DD_MM_YYYY)
    val currencyFormat by transactionFormViewModel.currencyFormat.observeAsState(CurrencyFormat.NZD)
    val currencySymbol = currencyFormat?.symbol ?: "$"

    val transactionNotes by transactionFormViewModel.description.observeAsState()
    val errorMap by transactionFormViewModel.fieldErrors.observeAsState()
    val editMode by transactionFormViewModel.editMode.observeAsState()
    val successMessage by transactionFormViewModel.successMessage.observeAsState()
    val errorMessage by transactionFormViewModel.errorMessage.observeAsState()
    val isLoading by transactionFormViewModel.isLoading.observeAsState()
    val cachedCategories by transactionFormViewModel.cachedCategories.observeAsState()
    val categorySelection by transactionFormViewModel.categorySelection.observeAsState()
    var deleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        transactionFormViewModel.setTransactionId(transactionId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FORM_SPACING_UNIT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FORM_SPACING_UNIT)
    ) {
        Header(if (editMode == true) "Edit Transaction" else "New Transaction")

        TextFieldForm(
            "Name", transactionName,
            onValueChange = { transactionFormViewModel.setName(it) },
            minLines = 1, maxLines = 1,
            enabled = editMode == false,
            isError = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.NAME)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.NAME)
        )
        TextFieldForm(
            "Description", transactionNotes,
            onValueChange = { transactionFormViewModel.setDescription(it) },
            minLines = 3, maxLines = 3,
            placeholder = TrackingUtlis.EMPTY_DESCRIPTION,
            isError = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.DESCRIPTION)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.DESCRIPTION)
        )
        NumberFieldForm(
            "Amount", transactionAmount,
            setter = { transactionFormViewModel.setAmount(it) },
            currencySymbol = currencySymbol,
            enabled = editMode == false,
            isError = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.AMOUNT)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.AMOUNT)
        )
        DatePickerField(
            label = "Date",
            value = transactionDate,
            onValueChange = { transactionFormViewModel.setDate(it) },
            onTextChange = { transactionFormViewModel.setDateText(it) },
            systemDateFormat = systemDateFormat ?: DateFormat.DD_MM_YYYY,
            enabled = editMode == false,
            isError = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.DATE)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.DATE)
        )
        SelectFormNonEditable(
            label = "Type",
            options = TrackingType.entries.map { it.name },
            selectedOption = transactionType?.name,
            onOptionSelected = { selectedName ->
                val matched = TrackingType.entries.firstOrNull { it.name == selectedName } ?: TrackingType.EXPENSE
                transactionFormViewModel.setType(matched)
            },
            enabled = editMode == false
        )
        val typeErr = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.TYPE)
        if (!typeErr.isNullOrBlank()) {
            Text(typeErr, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        CategorySelectionBox(
            label = "Categories",
            chosenCategories = cachedCategories?.map { it.category } ?: emptyList(),
            categorySelection = categorySelection,
            buttonLabel = "Choose Category",
            emptyMessage = "No categories selected",
            isError = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.CATEGORY_IDS)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.CATEGORY_IDS),
            onSelect = { transactionFormViewModel.addCategoryId(it) },
            selectLabel = "Add",
            onRemove = { transactionFormViewModel.removeCategoryId(it) },
            removeLabel = "Remove",
            dismissOnSelection = false,
            enabled = true
        )

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
                onClick = { transactionFormViewModel.saveTransaction() },
                modifier = Modifier.weight(1f),
                colors = getSaveButtonColors()
            ) {
                Text(ButtonMessages.SAVE.getMessage())
            }

            if (editMode == true) {
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
            } else {
                Button(
                    onClick = { transactionFormViewModel.clear() },
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
                onConfirm = { transactionFormViewModel.deleteTransaction() },
                onCancel = { deleteConfirmation = false },
                message = GeneralMessages.DELETE_BUTTON_CONFIRMATION.getMessage(transactionName)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(onClick = { navHostController.navigate("transaction") }) {
                Text(ButtonMessages.BACK.getMessage())
            }
        }
    }
}
