package com.example.mtfinance.screens


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.trackingengine.TrackingType
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.viewmodels.TransactionFormViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun TransactionFormScreen(transactionFormViewModel: TransactionFormViewModel = hiltViewModel(), navHostController: NavHostController, transactionId: Long? = null) {
    val transactionName by transactionFormViewModel.name.observeAsState()
    val transactionAmount by transactionFormViewModel.amount.observeAsState()
    val transactionDate by transactionFormViewModel.date.observeAsState()
    val transactionType by transactionFormViewModel.type.observeAsState()
    val categoryIds by transactionFormViewModel.categoryIds.observeAsState()

    val transactionNotes by transactionFormViewModel.description.observeAsState()
    val editMode by transactionFormViewModel.editMode.observeAsState()
    val successMessage by transactionFormViewModel.successMessage.observeAsState()
    val errorMessage by transactionFormViewModel.errorMessage.observeAsState()
    val isLoading by transactionFormViewModel.isLoading.observeAsState()
    val cachedCategories by transactionFormViewModel.cachedCategories.observeAsState()
    var expanded: Boolean by remember { mutableStateOf(false) }

    // edit mode if transactionId is not null
    transactionFormViewModel.setTransactionId(transactionId)

    DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Transaction Form")
        TextFieldForm("Name", transactionName, onValueChange = {transactionFormViewModel.setName(it)}, minLines = 1, maxLines = 1, enabled = editMode == false)
        TextFieldForm("Description", transactionNotes, onValueChange = {transactionFormViewModel.setDescription(it)}, minLines = 3, maxLines = 3, placeholder = TrackingUtlis.EMPTY_DESCRIPTION)
        NumberFieldForm("Amount", transactionAmount, setter = {transactionFormViewModel.setAmount(it)}, enabled = editMode == false)

        Text(transactionDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString())
        DatePickerField(value = transactionDate?.toString(), valuelong = transactionDate?.toLocalDate(),  onValueChange = {transactionFormViewModel.setDate(
            LocalDate.ofEpochDay(it!! / (1000 * 60 * 60 * 24)))}, enabled = editMode == false)

        CategoryList(categories = cachedCategories?.toList() ?: emptyList(), actionOne = {transactionFormViewModel.removeCategoryId(it)}, actionOneLabel = "Remove")
        ChooseCategoryForm(add = {transactionFormViewModel.addCategoryId(it)}, remove = {transactionFormViewModel.removeCategoryId(it)})

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





        Button(onClick = { transactionFormViewModel.saveTransaction() }) {
            Text("Save")
        }
        if (isLoading == true) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .padding(16.dp)
            ) {
            }
        }

        if (successMessage != null) {
            Text(successMessage!!, color = Color.Green)
        }
        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red)
        }

        if (editMode == true) {
            Button(onClick = { transactionFormViewModel.deleteTransaction() }) {
                Text("Delete")
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

/**
 * Allows you to view and choose a category from a list via a dialogue. Can input actions as composables for specific selection actions.
 */
@Composable
fun ChooseCategoryForm(transactionFormViewModel: TransactionFormViewModel = hiltViewModel(), add: (Long) -> Unit = {}, remove: (Long) -> Unit = {}) {
    var showDialog by remember { mutableStateOf(false) }
    val categorySelection by transactionFormViewModel.categorySelection.observeAsState()
    Button(onClick = { showDialog = !showDialog }) {
        Text("Choose Category")
    }

    if (showDialog) {
        Dialog( onDismissRequest = { showDialog = false }) {
            LazyColumn() {
                item {
                    CategoryList(categories = categorySelection?.toList() ?: emptyList(), actionOne = {add(it)}, actionOneLabel = "Add", actionTwo = {remove(it)}, actionTwoLabel = "Remove", backgroundColor = Color.Black)
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
    Button(onClick = { showDialog = true }, enabled = enabled) {
        Text(text = "Pick a Date")
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


