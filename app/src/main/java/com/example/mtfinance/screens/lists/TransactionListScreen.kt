/**
 * @file TransactionListScreen.kt
 * @scope Main screen composable for transaction listing, searching, filtering, and dashboard navigation.
 * @purpose Renders the list of transactions with filtering options, search input, and FABs for creation and import.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.DefaultColumn
import com.example.mtfinance.screens.FabRightBottomCorner
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.messages.CategoryMessages
import com.example.mtfinance.src.messages.ScreenMessages
import com.example.mtfinance.src.viewmodels.TransactionViewModel

/**
 * Main screen composable displaying transactions, search, filters, and transaction creation/import actions.
 */
@Composable
fun TransactionListScreen(
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    navHostController: NavHostController
) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()
    val searchQuery by transactionViewModel.searchQuery.observeAsState()
    val selectedTransaction by transactionViewModel.selectedTransaction.observeAsState()
    val typeFilter by transactionViewModel.typeFilter.observeAsState()

    FabRightBottomCorner(
        actionOne = { navHostController.navigate(Routes.TRANSACTION_FORM.route) },
        actionTwo = { navHostController.navigate(Routes.TRANSACTION_IMPORT.route) },
        iconImageOne = Icons.Default.Add,
        iconImageTwo = Icons.Default.ImportExport,
        content = {
            DefaultColumn(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedTransaction == null) {
                    Header(ScreenMessages.TRANSACTION_LIST_HEADER.getMessage())
                    Search(searchQuery) { transactionViewModel.setSearchQuery(it) }
                    ChooseTypeButton(typeFilter, onTypeSelected = { transactionViewModel.setTypeFilter(it) }, includeNone = true)

                    if (filteredTransactions != null && filteredTransactions!!.isNotEmpty()) {
                        TransactionList(
                            filteredTransactions!!,
                            actionOne = { transactionId: Long -> transactionViewModel.setSelectedTransaction(transactionId) },
                            actionOneLabel = ButtonMessages.SHOW_MORE.getMessage(),
                            actionTwo = { transactionId: Long -> navHostController.navigate(Routes.TRANSACTION_FORM.route + "/$transactionId") },
                            actionTwoLabel = ButtonMessages.EDIT.getMessage()
                        )
                    } else {
                        Text(CategoryMessages.NO_TRANSACTIONS.getMessage(), color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    TransactionDashboardScreen(transactionViewModel = transactionViewModel)
                }
            }
        }
    )
}
