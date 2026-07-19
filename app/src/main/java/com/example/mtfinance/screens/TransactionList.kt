package com.example.mtfinance.screens

import android.view.SurfaceControl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.TransactionViewModel


@Composable
fun TransactionListScreen(transactionViewModel: TransactionViewModel = hiltViewModel()) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()

   DefaultColumn {
       Text("Transaction List")
       if (filteredTransactions != null && filteredTransactions!!.isNotEmpty()) {
           TransactionList(filteredTransactions!!)
       } else {
           Text("No transactions found")
       }

   }

}

@Composable
fun TransactionList(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions.size) { index ->
            TransactionListItem(transactions[index])
        }
    }

}

@Composable
fun TransactionListItem(transaction: Transaction) {
    Text(transaction.name)
}