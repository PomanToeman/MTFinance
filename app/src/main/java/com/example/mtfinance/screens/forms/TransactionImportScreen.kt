package com.example.mtfinance.screens.forms

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import com.example.mtfinance.screens.LoadingDialog
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.viewmodels.TransactionImportFormViewModel

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

        if (successfulImports != null && successfulImports!!.isNotEmpty()) {
            Text(successfulImports.toString())
        }
        if (failedImports != null && failedImports!!.isNotEmpty()) {
            Text(failedImports.toString())
        }
    }
}
