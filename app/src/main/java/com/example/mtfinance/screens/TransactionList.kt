package com.example.mtfinance.screens

import android.view.SurfaceControl
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.TransactionViewModel


@Composable
fun TransactionListScreen(transactionViewModel: TransactionViewModel = hiltViewModel(), navHostController: NavHostController) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()
    FabRightBottomCorner(onClick = { navHostController.navigate("transactionForm") }, content = {
        DefaultColumn(horizontalAlignment = Alignment.Start) {
            Text("Transaction List")
            if (filteredTransactions != null && filteredTransactions!!.isNotEmpty()) {
                TransactionList(filteredTransactions!!)
            } else {
                Text("No transactions found")
            }

        }
    }, iconImage = androidx.compose.material.icons.Icons.Default.Add)


}

@Composable
fun TransactionList(transactions: Collection<Transaction>) {
    LazyColumn(modifier = Modifier.fillMaxSize().height(100.dp)) {
        items(transactions.size) { index ->
            TransactionListItem(transactions.elementAt(index))
        }
    }

}

@Composable
fun TransactionListItem(transaction: Transaction) {
    TextButton(
        content = { Text(text = transaction.name) },
        onClick = { }
    )
}