/**
 * @file TransactionListScreen.kt
 * @scope Main screen composable for transaction listing, searching, filtering, and dashboard navigation.
 * @purpose Renders the list of transactions with filtering options, search input, and dedicated bordered icon buttons between header and search.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.DefaultColumn
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

    DefaultColumn(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxSize()
            .let { if (selectedTransaction == null) it.verticalScroll(rememberScrollState()) else it }
    ) {
        if (selectedTransaction == null) {
            Header(ScreenMessages.TRANSACTION_LIST_HEADER.getMessage())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(onClick = { navHostController.navigate(Routes.TRANSACTION_FORM.route) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = ButtonMessages.ADD.getMessage())
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(onClick = { navHostController.navigate(Routes.TRANSACTION_IMPORT.route) }) {
                        Icon(imageVector = Icons.Default.ImportExport, contentDescription = ButtonMessages.IMPORT.getMessage())
                    }
                }
            }
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
            TransactionDashboardScreen(transactionViewModel = transactionViewModel, navHostController = navHostController)
        }
    }
}
