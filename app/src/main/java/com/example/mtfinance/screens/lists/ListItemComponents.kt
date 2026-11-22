/**
 * @file ListItemComponents.kt
 * @scope Row items, cards, and detail views for categories and transactions.
 * @purpose Provides composable UI elements for rendering individual category and transaction items as well as detail panels.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.screens.displayAmount
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.messages.CategoryMessages
import com.example.mtfinance.src.trackingengine.TrackingUtlis
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.viewmodels.SettingsViewModel

/**
 * Displays essential information of a category in a compact box. Can be expanded to show description and action buttons.
 */
@Composable
fun CategoryListItem(
    categoryItem: Category?,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val currencySymbol = systemPreferences?.currencySymbol ?: "$"
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
                            displayAmount(categoryItem.monthlyBudget, currencySymbol),
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(categoryItem.type.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (expanded.value) {
                        Text(
                            CategoryMessages.CATEGORY_DESCRIPTION.getMessage(categoryItem.description),
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
            CategoryMessages.CATEGORY_DESCRIPTION.getMessage(categoryItem.category.description),
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
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val currencySymbol = systemPreferences?.currencySymbol ?: "$"
    val dateFormatter = DateFormat.fromStringOrDefault(systemPreferences?.dateFormat, DateFormat.DD_MM_YYYY).toFormatter()

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
                    Text(displayAmount(transaction.amount, currencySymbol), textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        transaction.date.toLocalDate().format(dateFormatter).toString(),
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
 * Displays all the details of a given transaction inside a high-contrast outlined Card.
 */
@Composable
fun TransactionDetails(
    transaction: Transaction,
    categories: List<Category>? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val currencySymbol = systemPreferences?.currencySymbol ?: "$"
    val dateFormatter = DateFormat.fromStringOrDefault(systemPreferences?.dateFormat, DateFormat.DD_MM_YYYY).toFormatter()

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Name:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(transaction.name, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Amount:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(displayAmount(transaction.amount, currencySymbol), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Type:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(transaction.type.toString(), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(transaction.date.format(dateFormatter).toString(), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Categories:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (categories.isNullOrEmpty()) "None" else categories.joinToString { it.name },
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Description:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = transaction.description.ifBlank { TrackingUtlis.EMPTY_DESCRIPTION },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
