package com.example.mtfinance.screens.forms

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.LoadingDialog
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.screens.theme.getSaveButtonColors
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.viewmodels.TransactionImportFormViewModel

/**
 * Allows you to import transactions from a csv file with CSV data preview and column mapping.
 * @param transactionImportViewModel The viewModel to use.
 * @param navHostController The navigation controller to use.
 */
@Composable
fun TransactionImportScreen(
    transactionImportViewModel: TransactionImportFormViewModel = hiltViewModel(),
    navHostController: NavHostController
) {
    val csvHeaders by transactionImportViewModel.csvHeaders.observeAsState()
    val previewRows by transactionImportViewModel.previewRows.observeAsState()
    val nameHeader by transactionImportViewModel.nameHeader.observeAsState()
    val descriptionHeader by transactionImportViewModel.descriptionHeader.observeAsState()
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

    // Keep UI dateFormatterString in sync with ViewModel state changes
    if (dateFormatter != null && dateFormatterString != dateFormatter?.toString()) {
        dateFormatterString = dateFormatter?.toString()
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        transactionImportViewModel.setFileUri(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FORM_SPACING_UNIT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FORM_SPACING_UNIT)
    ) {
        Header("Transaction Import")

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FORM_SPACING_UNIT),
                verticalArrangement = Arrangement.spacedBy(FORM_SPACING_UNIT)
            ) {
                Text("CSV File Selection", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { csvLauncher.launch("text/comma-separated-values") }) {
                    Text("Select CSV File")
                }
                fileUri?.let {
                    Text(text = "Target Selected: ${it.path}", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { transactionImportViewModel.readTransactionFile() }) {
                    Text("Read File")
                }
            }
        }

        if (csvHeaders != null && csvHeaders!!.isNotEmpty()) {
            CsvViewPreviewTable(
                headers = csvHeaders!!,
                previewRows = previewRows ?: emptyList(),
                nameHeader = nameHeader,
                descriptionHeader = descriptionHeader,
                amountHeader = amountHeader,
                dateHeader = dateHeader,
                typeHeader = typeHeader
            )

            SelectForm(
                label = "Name Header",
                options = csvHeaders,
                selectedOption = nameHeader,
                onOptionSelected = { transactionImportViewModel.setNameHeader(it) }
            )
            SelectForm(
                label = "Description Header",
                options = csvHeaders,
                selectedOption = descriptionHeader,
                onOptionSelected = { transactionImportViewModel.setDescriptionHeader(it) }
            )
            SelectForm(
                label = "Amount Header",
                options = csvHeaders,
                selectedOption = amountHeader,
                onOptionSelected = { transactionImportViewModel.setAmountHeader(it) }
            )
            SelectForm(
                label = "Date Header",
                options = csvHeaders,
                selectedOption = dateHeader,
                onOptionSelected = { transactionImportViewModel.setDateHeader(it) }
            )
            SelectForm(
                label = "Type Header",
                options = csvHeaders,
                selectedOption = typeHeader,
                onOptionSelected = { transactionImportViewModel.setTypeHeader(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Always Send To Root", style = MaterialTheme.typography.bodyMedium)
                Checkbox(
                    checked = alwaysSendToRoot == true,
                    onCheckedChange = { transactionImportViewModel.setAlwaysSendToRoot(it) }
                )
            }

            SelectForm(
                label = "Date Formatter",
                options = DateFormat.values().map { it.toString() },
                selectedOption = dateFormatterString,
                onOptionSelected = { selectedFmt ->
                    dateFormatterString = selectedFmt
                    val matched = DateFormat.values().firstOrNull { it.toString() == selectedFmt }
                    if (matched != null) {
                        transactionImportViewModel.setDateFormatter(matched)
                    }
                }
            )

            Button(
                onClick = { transactionImportViewModel.importTransaction() },
                colors = getSaveButtonColors()
            ) {
                Text("Import")
            }
        }

        if (isLoading == true) {
            LoadingDialog()
        }

        if (!successMessage.isNullOrBlank()) {
            Text(successMessage!!, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyMedium)
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (successfulImports != null && successfulImports!!.isNotEmpty()) {
            Text("Successful imports: ${successfulImports?.size}", style = MaterialTheme.typography.bodyMedium)
        }
        if (failedImports != null && failedImports!!.isNotEmpty()) {
            Text("Failed imports: ${failedImports?.size}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(FORM_SPACING_UNIT))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(onClick = { navHostController.navigate(Routes.TRANSACTION.route) }) {
                Text("Back")
            }
        }
    }
}

/**
 * Interactive CSV View table rendering column headers and preview data rows.
 */
@Composable
fun CsvViewPreviewTable(
    headers: List<String>,
    previewRows: List<Map<String, String>>,
    nameHeader: String?,
    descriptionHeader: String?,
    amountHeader: String?,
    dateHeader: String?,
    typeHeader: String?
) {
    if (headers.isEmpty()) return

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FORM_SPACING_UNIT),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("CSV Data Preview", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                headers.forEach { header ->
                    val tag = when (header) {
                        nameHeader -> "[Name]"
                        descriptionHeader -> "[Desc]"
                        amountHeader -> "[Amount]"
                        dateHeader -> "[Date]"
                        typeHeader -> "[Type]"
                        else -> ""
                    }

                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .padding(4.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (tag.isNotEmpty()) "$header $tag" else header,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (tag.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (previewRows.isNotEmpty()) {
                            previewRows.forEach { rowMap ->
                                val cellVal = rowMap[header] ?: ""
                                Text(
                                    text = cellVal.ifEmpty { "-" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Text("-", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
