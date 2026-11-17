package com.example.mtfinance.screens.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.mtfinance.screens.ChooseTypeForm
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.viewmodels.TransactionFormViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The UI screen that allows the user to create and edit transactions in the database.
 */
@Composable
fun TransactionFormScreen(transactionFormViewModel: TransactionFormViewModel = hiltViewModel(), navHostController: NavHostController, transactionId: Long? = null) {
    val transactionName by transactionFormViewModel.name.observeAsState()
    val transactionAmount by transactionFormViewModel.amount.observeAsState()
    val transactionDate by transactionFormViewModel.date.observeAsState()
    val transactionType by transactionFormViewModel.type.observeAsState()

    val transactionNotes by transactionFormViewModel.description.observeAsState()
    val errorMap by transactionFormViewModel.fieldErrors.observeAsState()
    val editMode by transactionFormViewModel.editMode.observeAsState()
    val successMessage by transactionFormViewModel.successMessage.observeAsState()
    val errorMessage by transactionFormViewModel.errorMessage.observeAsState()
    val isLoading by transactionFormViewModel.isLoading.observeAsState()
    val cachedCategories by transactionFormViewModel.cachedCategories.observeAsState()
    var expanded: Boolean by remember { mutableStateOf(false) }
    val categorySelection by transactionFormViewModel.categorySelection.observeAsState()
    var deleteConfirmation by remember { mutableStateOf(false) }


    // edit mode if transactionId is not null
    LaunchedEffect(transactionId) {
        transactionFormViewModel.setTransactionId(transactionId)
    }

    DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Header(MessageCli.TRANSACTION_FORM_HEADER.getMessage())
        Text("Edit Mode: $editMode")
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
            enabled = editMode == false,
            isError = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.AMOUNT)?.isNotEmpty() == true,
            errorMessage = errorMap?.get(TransactionFormViewModel.TransactionErrorFields.AMOUNT)
        )


        DatePickerField(value = transactionDate?.format(DateTimeFormatter.ISO_LOCAL_DATE).toString(), valuelong = transactionDate?.toLocalDate(),  onValueChange = {transactionFormViewModel.setDate(
            LocalDate.ofEpochDay(it!! / (1000 * 60 * 60 * 24)))}, enabled = editMode == false)
        ChooseTypeForm(transactionType, onTypeSelected = {transactionFormViewModel.setType(it)}, enabled = editMode == false)
        ChooseCategoryForm(categorySelection, cachedCategories, select = {transactionFormViewModel.addCategoryId(it)}, selectLabel = "Add", remove = {transactionFormViewModel.removeCategoryId(it)}, removeLabel = "Remove")


        Button(onClick = { transactionFormViewModel.saveTransaction() }) {
            Text(MessageCli.SAVE_BUTTON.getMessage())
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

        if (editMode == true) {
            Button(onClick = { deleteConfirmation = true }) {
                Text("Delete")
            }
            if (deleteConfirmation) {
                DeleteConfirmationDialog(
                    onDismiss = { deleteConfirmation = false },
                    onConfirm = { transactionFormViewModel.deleteTransaction() },
                    onCancel = { deleteConfirmation = false },
                    message = MessageCli.DELETE_BUTTON_CONFIRMATION.getMessage(transactionName)
                )
            }
        }
        else {
            Button(onClick = { transactionFormViewModel.clear() }) {
                Text("Clear")
            }
        }

        Button(onClick = { navHostController.navigate("transaction") }) {
            Text("Back")
        }
    }
}
