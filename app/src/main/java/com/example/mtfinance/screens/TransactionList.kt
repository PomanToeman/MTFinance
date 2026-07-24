package com.example.mtfinance.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.TransactionViewModel
import java.math.RoundingMode
import java.time.format.DateTimeFormatter


@Composable
fun TransactionListScreen(transactionViewModel: TransactionViewModel = hiltViewModel(), navHostController: NavHostController) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()
    val searchQuery by transactionViewModel.searchQuery.observeAsState()
    val selectedTransaction by transactionViewModel.selectedTransaction.observeAsState()
    FabRightBottomCorner(onClick = { navHostController.navigate(Routes.TRANSACTION_FORM.route) }, content = {
        DefaultColumn(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (selectedTransaction == null) {
                Header("Transaction List")
                Row() {
                    Text("Search")
                    TextFieldForm(
                        "Search",
                        searchQuery,
                        onValueChange = { transactionViewModel.setSearchQuery(it) })
                }
                if (filteredTransactions != null && filteredTransactions!!.isNotEmpty()) {

                    TransactionList(
                        filteredTransactions!!,
                        actionOne = { transactionViewModel.setSelectedTransaction(it) },
                        actionOneLabel = "Show more",
                        actionTwo = { navHostController.navigate(Routes.TRANSACTION_FORM.route + "/$it") },
                        actionTwoLabel = "Edit",
                        backgroundColor = Color.LightGray
                    )
                } else {
                    Text("No Transactions Found")
                }
            }
            else {
                TransactionDashboardScreen()
            }
        }
    }, iconImage = androidx.compose.material.icons.Icons.Default.Add)


}

@Composable
fun TransactionList(transactions: Collection<Transaction>, modifier: Modifier = Modifier, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String = "Select", actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String = "Select", backgroundColor: Color = Color.Gray) {
    LazyColumn(modifier = modifier.fillMaxSize().height(500.dp).padding(16.dp).border(
        width = 2.dp,
        color = Color.Black,
        shape = RectangleShape
    ))
         {
        items(transactions.size) { index ->
            TransactionListItem(transactions.elementAt(index), actionOne = actionOne, actionOneLabel = actionOneLabel, actionTwo = actionTwo, actionTwoLabel = actionTwoLabel, backgroundColor = backgroundColor)
        }
    }

}

@Composable
fun TransactionListItem(transaction: Transaction, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String = "Select", actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String = "Select", expanded: Boolean = false, backgroundColor: Color = Color.Gray) {
    val expanded = remember { mutableStateOf(expanded) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 3. Toggle state on click
            .clickable { expanded.value = !expanded.value }
            .background(color = backgroundColor, shape = RectangleShape), content = {
            Column(Modifier.fillMaxWidth().padding(3.dp)) {
                Row() {

                    Text(transaction.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Left, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    Text("$" + transaction.amount.setScale(2, RoundingMode.HALF_UP).toString(), textAlign = TextAlign.Right)


                }
                val formatter = DateTimeFormatter.ofPattern("E dd MMM yyyy")
                Text(transaction.date.toLocalDate().format(formatter).toString(), textAlign = TextAlign.Left)
                if (expanded.value) {
                    Text(transaction.description, minLines = 1, maxLines = 3)
                    Row() {
                        if (actionOne != null) {
                            TextButton(onClick = { actionOne(transaction.transactionId) }) {
                                Text(actionOneLabel)
                            }
                        }
                        if (actionTwo != null) {
                            TextButton(onClick = { actionTwo(transaction.transactionId) }) {
                                Text(actionTwoLabel)
                            }
                        }
                    }

                }


            }



        })
}

@Composable
fun TransactionDashboardScreen(transactionViewModel: TransactionViewModel = hiltViewModel()) {
    val selectedTransaction by transactionViewModel.selectedTransaction.observeAsState()
    val categoriesUnderSelectedTransaction by transactionViewModel.categoriesUnderSelectedTransaction.observeAsState()
    Column(modifier = Modifier.fillMaxSize(), content =  {
        if (selectedTransaction != null) {
            Header("Transaction Dashboard")

        }


        Button(onClick = { transactionViewModel.resetSelectedTransaction() }) {
            Text("Back")
        }


    })

}
