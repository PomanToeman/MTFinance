package com.example.mtfinance.screens


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List

import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox

import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.DateFormat

import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.example.mtfinance.src.trackingengine.TrackingType
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.viewmodels.CategoryFormViewModel
import com.example.mtfinance.src.viewmodels.TransactionFormViewModel
import com.example.mtfinance.src.viewmodels.TransactionImportFormViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun TransactionFormScreen(transactionFormViewModel: TransactionFormViewModel = hiltViewModel(), navHostController: NavHostController, transactionId: Long? = null) {
    val transactionName by transactionFormViewModel.name.observeAsState()
    val transactionAmount by transactionFormViewModel.amount.observeAsState()
    val transactionDate by transactionFormViewModel.date.observeAsState()
    val transactionType by transactionFormViewModel.type.observeAsState()

    val transactionNotes by transactionFormViewModel.description.observeAsState()
    val editMode by transactionFormViewModel.editMode.observeAsState()
    val successMessage by transactionFormViewModel.successMessage.observeAsState()
    val errorMessage by transactionFormViewModel.errorMessage.observeAsState()
    val isLoading by transactionFormViewModel.isLoading.observeAsState()
    val cachedCategories by transactionFormViewModel.cachedCategories.observeAsState()
    var expanded: Boolean by remember { mutableStateOf(false) }
    val categorySelection by transactionFormViewModel.categorySelection.observeAsState()
    var deleteConfirmation by remember { mutableStateOf(false) }

    // edit mode if transactionId is not null
    transactionFormViewModel.setTransactionId(transactionId)

    DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Transaction Form")
        TextFieldForm("Name", transactionName, onValueChange = {transactionFormViewModel.setName(it)}, minLines = 1, maxLines = 1, enabled = editMode == false)
        TextFieldForm("Description", transactionNotes, onValueChange = {transactionFormViewModel.setDescription(it)}, minLines = 3, maxLines = 3, placeholder = TrackingUtlis.EMPTY_DESCRIPTION)
        NumberFieldForm("Amount", transactionAmount, setter = {transactionFormViewModel.setAmount(it)}, enabled = editMode == false)


        DatePickerField(value = transactionDate?.format(DateTimeFormatter.ISO_LOCAL_DATE).toString(), valuelong = transactionDate?.toLocalDate(),  onValueChange = {transactionFormViewModel.setDate(
            LocalDate.ofEpochDay(it!! / (1000 * 60 * 60 * 24)))}, enabled = editMode == false)
        Box(
            modifier = Modifier
                .padding(16.dp),

            ) {
            Button(onClick = { expanded = !expanded }, enabled = editMode == false) {
                Text("Type: " + transactionType.toString().lowercase())
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                for (type in TrackingType.entries) {
                    DropdownMenuItem(
                        text = { Text(type.toString().lowercase()) },
                        onClick = {
                            transactionFormViewModel.setType(type)
                            expanded = false
                        }
                    )
                }
            }}
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

@Composable
fun CategoryFormScreen(categoryFormViewModel: CategoryFormViewModel = hiltViewModel(), navHostController: NavHostController, categoryId: Long? = null) {
    val categoryName by categoryFormViewModel.name.observeAsState()
    val categoryDescription by categoryFormViewModel.description.observeAsState()
    val categoryMonthlyBudget by categoryFormViewModel.monthlyBudget.observeAsState()
    val categoryType by categoryFormViewModel.type.observeAsState()
    val categoryParent by categoryFormViewModel.cachedParentCategory.observeAsState()
    val editMode by categoryFormViewModel.IsEditMode().observeAsState()
    val successMessage by categoryFormViewModel.successMessage.observeAsState()
    val errorMessage by categoryFormViewModel.errorMessage.observeAsState()
    val isLoading by categoryFormViewModel.isLoading.observeAsState()
    val categorySelection by categoryFormViewModel.categorySelection.observeAsState()
    val isRoot by categoryFormViewModel.isRoot.observeAsState()
    var deleteTransactions by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }

    // edit mode if categoryId is not null
    categoryFormViewModel.setEditCategory(categoryId)

    DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Header("Category Form")
        Text("Edit Mode: $editMode")
        TextFieldForm("Name", categoryName, onValueChange = {categoryFormViewModel.setName(it)}, minLines = 1, maxLines = 1, enabled = isRoot == false)
        TextFieldForm("Description", categoryDescription, onValueChange = {categoryFormViewModel.setDescription(it)}, minLines = 3, maxLines = 3, placeholder = TrackingUtlis.EMPTY_DESCRIPTION, enabled = isRoot == false)
        NumberFieldForm("Monthly Budget", categoryMonthlyBudget, setter = {categoryFormViewModel.setMonthlyBudget(it)})
        Column(modifier = Modifier.padding(16.dp)) {
            if (categoryParent != null && isRoot == false) {
                CategoryListItem(categoryParent!!, backgroundColor = Color.LightGray)
            }
            else if (isRoot == false)  {
                Text("Parent: The root of the category")
            }
            else {
                Text("Parent: None (Root Category)")
            }
            if (isRoot == false) {
                ChooseCategoryForm( categorySelection, select = {categoryFormViewModel.setParentId(it)},  selectLabel = "Set Parent", dismissOnSelection = true)
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

/**
 * Allows you to import transactions from a csv file.
 * @param transactionImportViewModel The viewModel to use.
 * @param navHostController The navigation controller to use.
 */
@Composable
fun TransactionImportScreen(transactionImportViewModel: TransactionImportFormViewModel = hiltViewModel(), navHostController: NavHostController) {

    val csvParser by transactionImportViewModel.csvParser.observeAsState()
    val csvHeaders by transactionImportViewModel.csvHeaders.observeAsState()
    val nameHeader by transactionImportViewModel.nameHeader.observeAsState()
    val amountHeader by transactionImportViewModel.amountHeader.observeAsState()
    val dateHeader by transactionImportViewModel.dateHeader.observeAsState()
    val typeHeader by transactionImportViewModel.typeHeader.observeAsState()
    val errorMessage by transactionImportViewModel.errorMessage.observeAsState()
    val successMessage by transactionImportViewModel.successMessage.observeAsState()
    val isLoading by transactionImportViewModel.isLoading.observeAsState()
    val fileUri by transactionImportViewModel.fileUri.observeAsState()
    val successfulImports by transactionImportViewModel.successfulImports.observeAsState()
    val failedImports by transactionImportViewModel.failedImports.observeAsState()
    val alwaysSendToRoot by transactionImportViewModel.alwaysSendToRoot.observeAsState()
    val dateFormatter by transactionImportViewModel.dateFormatter.observeAsState()
    var dateFormatterString by remember { mutableStateOf(dateFormatter?.toString()) }
    var dateFormatterMenu by remember { mutableStateOf(false) }


    DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {

        Text("Transaction Import")


        val csvLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            transactionImportViewModel.setFileUri(uri)
        }


        Button(onClick = {
            csvLauncher.launch("text/comma-separated-values")
        }) {
            Text("Select CSV File")
        }

        fileUri?.let {
            Text(text = "Target Selected: ${it.path}", modifier = Modifier.padding(top = 10.dp))
        }

        Button(onClick = { transactionImportViewModel.readTransactionFile() }) {
            Text("Read File")
        }
        if (csvParser != null) {
            Text(csvHeaders.toString())
            SelectForm( label = "Name Header", options = csvHeaders, selectedOption = nameHeader, onOptionSelected = { transactionImportViewModel.setNameHeader(it)})
            SelectForm( label = "Amount Header", options = csvHeaders, selectedOption = amountHeader, onOptionSelected = { transactionImportViewModel.setAmountHeader(it)})
            SelectForm( label = "Date Header", options = csvHeaders, selectedOption = dateHeader, onOptionSelected = { transactionImportViewModel.setDateHeader(it)})
            SelectForm( label = "Type Header", options = csvHeaders, selectedOption = typeHeader, onOptionSelected = { transactionImportViewModel.setTypeHeader(it)})
            Row() {
                Text("Always Send To Root")
                Checkbox(
                    checked = alwaysSendToRoot == true,
                    onCheckedChange = { transactionImportViewModel.setAlwaysSendToRoot(it) }
                )

            }
            TextField(label = { Text("Date Formatter")}, value = dateFormatterString ?: "", onValueChange = {transactionImportViewModel.setDateFormatter(it)}, trailingIcon = {
                IconButton(onClick = { dateFormatterMenu = !dateFormatterMenu }) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "Date Formatter List")
                }
            })
            if (dateFormatterMenu) {
                DateFormat.values().forEach {
                    DropdownMenuItem(
                        text = { Text(it.toString()) },
                        onClick = {
                            transactionImportViewModel.setDateFormatter(it)
                            dateFormatterString = it.toString()
                            dateFormatterMenu = false
                        }
                    )
                }
            }

            Button(onClick = { transactionImportViewModel.importTransaction() }) {
                Text("Import")
            }



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


        Button(onClick = {navHostController.navigate(Routes.TRANSACTION.route)}) {
            Text("Back")
        }

        if (successfulImports != null) {
            Text(successfulImports.toString())
        }
        if (failedImports != null) {
            Text(failedImports.toString())
        }
    }

}

/**
 * Allows you to view and choose a category from a list via a dialogue. Can input actions as composables for specific selection actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseCategoryForm(categorySelection: List<CategoryWithTransactions?>?, chosenCategories: Set<CategoryWithTransactions?>? = null, select: ((Long) -> Unit)? = null, selectLabel: String? = null, remove: ((Long) -> Unit)? = null, removeLabel: String? = null, dismissOnSelection: Boolean = false) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = !showDialog }) {
        Text("Choose Category")
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }) {

        LazyColumn() {
            item {
                Text("Select Category")
            }
            if (chosenCategories == null) {
                item {
                    CategoryList(categories = categorySelection as Collection<CategoryWithTransactions>, actionOne = {if (select != null) {select(it); if (dismissOnSelection) showDialog = false}}, actionOneLabel = selectLabel, actionTwo = remove, actionTwoLabel = removeLabel, backgroundColor = Color.LightGray)
                }
            }
            else {
                items(categorySelection!!.size) {
                    if (categorySelection[it] != null && chosenCategories.contains(categorySelection[it])) {
                        CategoryListItem(categorySelection[it]!!, backgroundColor = Color.Green, actionOne = {if (remove != null) {remove(it); if (dismissOnSelection) showDialog = false}}, actionOneLabel = removeLabel)
                    }
                    else {
                        CategoryListItem(categorySelection[it]!!, backgroundColor = Color.LightGray, actionOne = {if (select != null) {select(it); if (dismissOnSelection) showDialog = false}}, actionOneLabel = selectLabel)
                    }

                }
            }

            item {
                Button(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        }


    }
    }




}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(value: String? = null, valuelong: LocalDate?, onValueChange: (Long?) -> Unit, enabled: Boolean = true) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = valuelong?.toEpochDay()?.times(1000 * 60 * 60 * 24))

    // Button to trigger the dialog
    TextButton(onClick = { showDialog = true }, enabled = enabled) {
        Text(text = value ?: "Select Date")
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false
                onValueChange(datePickerState.selectedDateMillis)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }


        ) {
            // The calendar UI goes inside the dialog content slot
            return@DatePickerDialog DatePicker(state = datePickerState)
        }
    }
}


@Composable
fun TextFieldForm(label: String, value: String?, onValueChange: (String) -> Unit, minLines: Int = 1, maxLines: Int = 1, placeholder: String = "", enabled: Boolean = true) {
    TextField(label = { Text(label) }, value = value ?: "", onValueChange = onValueChange, minLines = minLines, maxLines = maxLines, placeholder = { Text(placeholder)}, enabled = enabled)
}

@Composable
fun NumberFieldForm(label: String, value: BigDecimal?, setter: (BigDecimal?) -> Unit, enabled: Boolean = true) {
    OutlinedTextField(
        label = { Text(label) },
        value = value?.toString() ?: "",
        prefix = { Text("$ ") },
        placeholder = { Text("0.00") },
        onValueChange = {input ->
            val filteredInput = input.filter { it.isDigit() || it == '.' }

            if (filteredInput.isNotEmpty() ) {

                if (filteredInput.last() == '.') {
                    setter(BigDecimal(filteredInput + "0"))
                    return@OutlinedTextField
                }
                if (filteredInput.first() == '.') {
                    setter(BigDecimal("0" + filteredInput))
                    return@OutlinedTextField
                }
                if (filteredInput.contains(".")) {
                    val parts = filteredInput.split(".")
                    if (parts[1].length > 2) {
                        setter(BigDecimal(parts[0] + "." + parts[1].substring(0, 2)))
                        return@OutlinedTextField
                    }
                }

                try {
                    setter(BigDecimal(filteredInput))
                } catch (e: NumberFormatException) {
                    setter(null)
                }
            }
            else {
                setter(null)
            }


        },
        enabled = enabled

    )


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectForm(label: String, options: List<String?>?, selectedOption: String?, onOptionSelected: (String) -> Unit) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var textFieldValue: String? by remember { mutableStateOf(selectedOption ?: "") }
    val filteredOptions = options?.filter { textFieldValue?.let { other -> it?.contains(other, ignoreCase = true) }
        ?: false }

    ExposedDropdownMenuBox(
        expanded = isMenuExpanded,
        onExpandedChange = { isMenuExpanded = !isMenuExpanded },
    ) {
        TextField(
            value = textFieldValue ?: "",
            onValueChange = {value -> textFieldValue = value; onOptionSelected(value)},
            label = { Text(label) },
            trailingIcon = { IconButton(onClick = { isMenuExpanded = !isMenuExpanded }, ) {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded)
            }})

    }

    if (filteredOptions?.isNotEmpty() ?: false && isMenuExpanded) {
        filteredOptions.forEach { selectionOption ->
            DropdownMenuItem(
                text = {
                    if (selectionOption != null) {
                        Text(selectionOption)
                    }
                },
                onClick = {
                    textFieldValue = selectionOption
                    onOptionSelected(selectionOption ?: "")
                    isMenuExpanded = false
                }
            )
        }
    }
}


