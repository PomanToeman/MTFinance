/**
 * @file TransactionDashboard.kt
 * @scope Transaction dashboard views and details summary.
 * @purpose Displays all necessary information of a selected transaction, including associated categories.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.screens.Header
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.messages.CategoryMessages
import com.example.mtfinance.src.messages.ScreenMessages
import com.example.mtfinance.src.viewmodels.TransactionViewModel

/**
 * Displays all necessary information of a selected transaction within the viewModel.
 * This includes categories the transaction is under.
 */
@Composable
fun TransactionDashboardScreen(transactionViewModel: TransactionViewModel = hiltViewModel()) {
    val selectedTransaction by transactionViewModel.selectedTransaction.observeAsState()
    val categoriesUnderSelectedTransaction by transactionViewModel.categoriesUnderSelectedTransaction.observeAsState()
    Column(modifier = Modifier.fillMaxSize(), content =  {
        if (selectedTransaction != null) {
            Header(ScreenMessages.TRANSACTION_DASHBOARD_HEADER.getMessage())
        } else {
            Header(CategoryMessages.NO_TRANSACTIONS.getMessage())
        }
        if (selectedTransaction != null) {
            TransactionDetails(selectedTransaction!!)
        }

        if (categoriesUnderSelectedTransaction != null) {
            Text("Categories")
            CategoryRawList(categoriesUnderSelectedTransaction!!)
        }

        Button(onClick = { transactionViewModel.resetSelectedTransaction() }) {
            Text(ButtonMessages.BACK.getMessage())
        }
    })
}
