package com.example.mtfinance.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImportExport
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
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.TransactionViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


/**
 * Meant to display all transactions of the app via the transactionViewModel. Which can filtered through a search query.
 * Also allows you to add and edit transactions via navigation. And select a transaction to view more details via a dashboard.
 * @param transactionViewModel The viewModel to use.
 * @param navHostController The navigation controller to use.
 */
@Composable
fun TransactionListScreen(transactionViewModel: TransactionViewModel = hiltViewModel(), navHostController: NavHostController) {
    val filteredTransactions by transactionViewModel.filteredTransactions.observeAsState()
    val searchQuery by transactionViewModel.searchQuery.observeAsState()
    val selectedTransaction by transactionViewModel.selectedTransaction.observeAsState()
    val typeFilter by transactionViewModel.typeFilter.observeAsState()

    FabRightBottomCorner(actionOne = {
        navHostController.navigate(Routes.TRANSACTION_FORM.route)
                                   }, actionTwo = { navHostController.navigate(Routes.TRANSACTION_IMPORT.route) }, iconImageTwo = Icons.Default.ImportExport, content = {
        DefaultColumn(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (selectedTransaction == null) {
                Header(MessageCli.TRANSACTION_LIST_HEADER.getMessage())
                Search(searchQuery) { transactionViewModel.setSearchQuery(it) }
                ChooseTypeForm(typeFilter, onTypeSelected = {transactionViewModel.setTypeFilter(it)}, includeNone = true, enabled = true)
                if (filteredTransactions != null && filteredTransactions!!.isNotEmpty()) {

                    TransactionList(
                        filteredTransactions!!,
                        actionOne = { transactionViewModel.setSelectedTransaction(it) },
                        actionOneLabel = MessageCli.SHOW_MORE_BUTTON.getMessage(),
                        actionTwo = { navHostController.navigate(Routes.TRANSACTION_FORM.route + "/$it") },
                        actionTwoLabel = MessageCli.EDIT_BUTTON.getMessage(),
                        backgroundColor = Color.LightGray
                    )
                } else {
                    Text(MessageCli.NO_TRANSACTIONS.getMessage())
                }
            }
            else {
                TransactionDashboardScreen()
            }
        }
    }, iconImageOne = Icons.Default.Add)


}

/**
 * Displays a given collection of transactions in a compact list.
 * You can add actions will be given to each of the transactions.
 * @param transactions The transactions to display.
 * @param modifier The modifier to apply to the list.
 * @param actionOne The action to perform when a transaction is selected.
 * @param actionOneLabel The label to display on the action button.
 * @param actionTwo The action to perform when a transaction is selected.
 * @param actionTwoLabel The label to display on the action button.
 * @param backgroundColor The background color to apply to the list.
 */
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

/**
 * Meant to display all essential information of a transaction in a compact box, including a name and amount.
 * You can add actions to the transaction for it to be selected or edited.
 * Can be expanded to show the actions and description.
 * @param transaction The transaction to display.
 * @param actionOne The action to perform when the transaction is selected.
 * @param actionOneLabel The label to display on the action button.
 * @param actionTwo The action to perform when the transaction is selected.
 * @param actionTwoLabel The label to display on the action button.
 */
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
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {

                    Text(transaction.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Left, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    Text(displayAmount(transaction.amount), textAlign = TextAlign.Right)


                }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(transaction.date.toLocalDate().format(DateFormat.FULL_DATE.toFormatter()).toString(), textAlign = TextAlign.Left, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(transaction.type.toString(), textAlign = TextAlign.Right)
                }


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
            Header(MessageCli.TRANSACTION_DASHBOARD_HEADER.getMessage())

        }
        else {
            Header(MessageCli.NO_TRANSACTIONS.getMessage())
        }
        if (selectedTransaction != null) {
            TransactionDetails(selectedTransaction!!)
        }


        if (categoriesUnderSelectedTransaction != null) {
            Text("Categories")
            CategoryList(categoriesUnderSelectedTransaction!!)
        }



        Button(onClick = { transactionViewModel.resetSelectedTransaction() }) {
            Text(MessageCli.BACK_BUTTON.getMessage())
        }


    })

}

/**
 * Displays all the details of a given transaction.
 * @param transaction The transaction to display.
 */
@Composable
fun TransactionDetails(transaction: Transaction) {
    Text(transaction.name)
    Text(transaction.description)
    Text(transaction.amount.toString())
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    Text(transaction.date.format(formatter).toString())
}
