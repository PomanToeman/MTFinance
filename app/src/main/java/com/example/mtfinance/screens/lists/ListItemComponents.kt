/**
 * @file ListItemComponents.kt
 * @scope Row items, cards, and detail views for categories and transactions.
 * @purpose Provides composable UI elements for rendering individual category and transaction items as well as detail panels.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mtfinance.screens.displayAmount
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.example.mtfinance.src.trackingengine.Transaction
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays essential information of a category in a compact box. Can be expanded to show description and action buttons.
 * @param categoryItem The category to display.
 * @param actionOne Primary action callback.
 * @param actionOneLabel Label for primary action.
 * @param actionTwo Secondary action callback.
 * @param actionTwoLabel Label for secondary action.
 * @param backgroundColor Background color for the item card.
 */
@Composable
fun CategoryListItem(
    categoryItem: Category?,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val expanded = remember { mutableStateOf(false) }
    if (categoryItem != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = !expanded.value }
                .background(color = backgroundColor, shape = RectangleShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                content = {
                    Row {
                        Text(
                            categoryItem.name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Left,
                            minLines = 1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$" + categoryItem.monthlyBudget.setScale(2, RoundingMode.HALF_UP).toString(),
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(categoryItem.type.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (expanded.value) {
                        Text(
                            MessageCli.CATEGORY_DESCRIPTION.getMessage(categoryItem.description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            minLines = 1,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row {
                            if (actionOne != null) {
                                TextButton(onClick = { actionOne(categoryItem.categoryId) }) {
                                    Text(
                                        actionOneLabel ?: "Show more",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (actionTwo != null) {
                                TextButton(onClick = { actionTwo(categoryItem.categoryId) }) {
                                    Text(
                                        actionTwoLabel ?: "Show more",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Renders a category list item from a [CategoryWithTransactions] wrapper.
 */
@Composable
fun CategoryListItem(
    categoryWithTransactions: CategoryWithTransactions,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    CategoryListItem(
        categoryWithTransactions.category,
        actionOne,
        actionOneLabel,
        actionTwo,
        actionTwoLabel,
        backgroundColor
    )
}

/**
 * Renders transactions associated with a given category or shows a fallback message if empty.
 */
@Composable
fun TransactionListforCategory(categoryItem: CategoryWithTransactions) {
    if (categoryItem.transactions != null && categoryItem.transactions.isNotEmpty()) {
        TransactionList(categoryItem.transactions)
    } else {
        Text("No transactions", color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Displays detailed information for a category inside a bordered box.
 */
@Composable
fun CategoryDetails(categoryItem: CategoryWithTransactions) {
    Text(
        "Category Details",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Left
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            MessageCli.CATEGORY_DESCRIPTION.getMessage(categoryItem.category.description),
            color = MaterialTheme.colorScheme.primary,
            minLines = 1,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}

/**
 * Displays essential information of a transaction in a compact box. Can be expanded to show actions and description.
 */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String = "Select",
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String = "Select",
    expanded: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val expandedState = remember { mutableStateOf(expanded) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandedState.value = !expandedState.value }
            .background(color = backgroundColor, shape = RectangleShape),
        content = {
            Column(Modifier.fillMaxWidth().padding(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        transaction.name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Left,
                        minLines = 1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(displayAmount(transaction.amount), textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        transaction.date.toLocalDate().format(DateFormat.FULL_DATE.toFormatter()).toString(),
                        textAlign = TextAlign.Left,
                        minLines = 1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(transaction.type.toString(), textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (expandedState.value) {
                    Text(transaction.description, minLines = 1, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
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
        }
    )
}

/**
 * Displays all the details of a given transaction.
 */
@Composable
fun TransactionDetails(transaction: Transaction) {
    Text(transaction.name)
    Text(transaction.description)
    Text(transaction.amount.toString())
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    Text(transaction.date.format(formatter).toString())
}
