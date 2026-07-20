package com.example.mtfinance.screens

import android.view.SurfaceControl
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.TransactionViewModel


@Composable
fun TransactionListScreen(transactionViewModel: TransactionViewModel = hiltViewModel()) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()

   DefaultColumn(horizontalAlignment = Alignment.Start) {
       Text("Transaction List")
       if (filteredTransactions != null && filteredTransactions!!.isNotEmpty()) {
           TransactionList(filteredTransactions!!)
       } else {
           Text("No transactions found")
       }

   }

}

@Composable
fun TransactionList(transactions: Collection<Transaction>) {
    LazyRow(modifier = Modifier.fillMaxSize()) {
        items(transactions.size) { index ->
            TransactionListItem(transactions.elementAt(index))
        }
    }

}

@Composable
fun TransactionListItem(transaction: Transaction) {
    Text(transaction.name + " ")
}