package com.example.mtfinance.screens


import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.viewmodels.TransactionFormViewModel
import java.math.BigDecimal


@Composable
fun TransactionFormScreen(transactionFormViewModel: TransactionFormViewModel = hiltViewModel()) {
    val transactionName by transactionFormViewModel.name.observeAsState()
    val transactionAmount by transactionFormViewModel.amount.observeAsState()
    val transactionDate by transactionFormViewModel.date.observeAsState()
    val transactionType by transactionFormViewModel.type.observeAsState()
    val categoryIds by transactionFormViewModel.categoryIds.observeAsState()
    val transactionNotes by transactionFormViewModel.description.observeAsState()
    val editMode by transactionFormViewModel.editMode.observeAsState()
    val successMessage by transactionFormViewModel.successMessage.observeAsState()
    val errorMessage by transactionFormViewModel.errorMessage.observeAsState()


    DefaultColumn {
        Text("Transaction Form")
        TextFieldForm("Name", transactionName, onValueChange = {transactionFormViewModel.setName(it)})
        NumbereFieldForm("Amount", transactionAmount, setter = {transactionFormViewModel.setAmount(it)})

        DialogDatePickerExample()

        Button(onClick = { transactionFormViewModel.saveTransaction() }) {
            Text("Save")
        }





        if (successMessage != null) {
            Text(successMessage!!, color = androidx.compose.ui.graphics.Color.Green)
        }
        if (errorMessage != null) {
            Text(errorMessage!!, color = androidx.compose.ui.graphics.Color.Red)
        }



    }

}

@Composable
fun DatePickerForm(label: String, value: String?, onValueChange: (String) -> Unit) {
    // TODO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogDatePickerExample() {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Button to trigger the dialog
    Button(onClick = { showDialog = true }) {
        Text(text = "Pick a Date")
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
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
            DatePicker(state = datePickerState)
        }
    }
}


@Composable
fun TextFieldForm(label: String, value: String?, onValueChange: (String) -> Unit) {
    TextField(label = { Text(label) }, value = value ?: "", onValueChange = onValueChange)
}

@Composable
fun NumbereFieldForm(label: String, value: BigDecimal?, setter: (BigDecimal) -> Unit) {
    OutlinedTextField(
        label = { Text(label) },
        value =  if (value != null && value != BigDecimal.ZERO) value.toString() else "",
        prefix = { Text("$ ") },
        placeholder = { Text("0.00") },
        onValueChange = {input ->
            val filteredInput = input.filter { it.isDigit() || it == '.' }

            if (filteredInput.isNotEmpty() ) {

                if (filteredInput.last() == '.') {
                    setter(BigDecimal(filteredInput + "00"))
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
                    setter(BigDecimal.ZERO)
                }
            }
            else {
                setter(BigDecimal.ZERO)
            }


        }
    )


}


