package com.example.mtfinance.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.TransactionViewModel
import java.math.RoundingMode


@Composable
fun TransactionListScreen(transactionViewModel: TransactionViewModel = hiltViewModel(), navHostController: NavHostController) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()
    FabRightBottomCorner(onClick = { navHostController.navigate("transactionForm") }, content = {
        DefaultColumn(horizontalAlignment = Alignment.Start) {

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
    LazyColumn(modifier = Modifier.fillMaxSize().height(100.dp).border(
        width = 2.dp,
        color = Color.Black,
        shape = RectangleShape
    )) {
        items(transactions.size) { index ->
            TransactionListItem(transactions.elementAt(index))
        }
    }

}

@Composable
fun TransactionListItem(transaction: Transaction, action: ((Long) -> Unit)? = null, actionLabel: String = "Select", expanded: Boolean = false, backgroundColor: Color = Color.Gray) {
    val expanded = remember { mutableStateOf(expanded) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 3. Toggle state on click
            .clickable { expanded.value = !expanded.value }.background(color = backgroundColor, shape = RectangleShape), content = {
            Column() {
                Row() {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(transaction.name)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("$" + transaction.amount.setScale(2, RoundingMode.HALF_UP).toString())
                    Spacer(modifier = Modifier.weight(1f))
                    Text(transaction.date.toLocalDate( ).toString())
                }
                if (expanded.value) {
                    Text(transaction.description)
                }
                if (action != null) {
                    TextButton(onClick = { action(transaction.transactionId) }) {
                        Text("Select")
                    }
                }

            }



        })
}