/**
 * @file ListHelpers.kt
 * @scope Reusable list containers and search input components for categories and transactions.
 * @purpose Centralizes list UI wrappers, list renderers, and search filtering text fields.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.trackingengine.TrackingType

/**
 * Renders a search input field for filtering lists with a clear button.
 * @param search The current search query string.
 * @param setSearch Callback invoked when the search query changes.
 */
@Composable
fun Search(search: String?, setSearch: ((String) -> Unit)?) {
    OutlinedTextField(
        value = search ?: "",
        onValueChange = { setSearch?.invoke(it) },
        label = { Text("Search") },
        trailingIcon = {
            if (!search.isNullOrEmpty()) {
                IconButton(onClick = { setSearch?.invoke("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}

/**
 * Renders a square-styled type selection button for list screen filters.
 * @param typeValue The current tracking type filter.
 * @param onTypeSelected Callback when type filter changes.
 * @param includeNone Whether to include "None" option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseTypeButton(
    typeValue: TrackingType?,
    onTypeSelected: (TrackingType?) -> Unit,
    includeNone: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(8.dp)) {
        Button(
            onClick = { expanded = !expanded },
            shape = RectangleShape,
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth()
        ) {
            val text = if (typeValue != null) "Type: ${typeValue.toString().lowercase()}" else "Type: All"
            Text(text)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (type in TrackingType.entries) {
                DropdownMenuItem(
                    text = { Text(type.toString().lowercase()) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
            if (includeNone) {
                DropdownMenuItem(
                    text = { Text("All / None") },
                    onClick = {
                        onTypeSelected(null)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Renders a lazy list container with a border listing [CategoryWithTransactions] items.
 * @param categories Collection of categories with transactions to display.
 * @param actionOne Optional primary action callback (e.g. select/edit).
 * @param actionOneLabel Label for the primary action button.
 * @param actionTwo Optional secondary action callback (e.g. delete).
 * @param actionTwoLabel Label for the secondary action button.
 * @param backgroundColor Background color for list item cards.
 * @param updateTrigger Optional state trigger to force recomposition.
 */
@Composable
fun CategoryList(
    categories: Collection<CategoryWithTransactions>,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    updateTrigger: Long? = 0
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .height(500.dp)
            .padding(16.dp)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape)
    ) {
        items(
            categories.size,
            key = { index -> "${categories.elementAt(index).category.categoryId}_${updateTrigger}" }
        ) { index ->
            CategoryListItem(
                categories.elementAt(index),
                actionOne = actionOne,
                actionOneLabel = actionOneLabel,
                actionTwo = actionTwo,
                actionTwoLabel = actionTwoLabel,
                backgroundColor = backgroundColor
            )
        }
    }
}

/**
 * Renders a lazy list container with a border listing raw [Category] items.
 * @param categories Collection of categories to display.
 * @param actionOne Optional primary action callback.
 * @param actionOneLabel Label for the primary action button.
 * @param actionTwo Optional secondary action callback.
 * @param actionTwoLabel Label for the secondary action button.
 * @param backgroundColor Background color for list item cards.
 * @param updateTrigger Optional state trigger to force recomposition.
 */
@Composable
fun CategoryRawList(
    categories: Collection<Category>,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    updateTrigger: Long? = 0
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .height(500.dp)
            .padding(16.dp)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape)
    ) {
        items(
            categories.size,
            key = { index -> "${categories.elementAt(index).categoryId}_${updateTrigger}" }
        ) { index ->
            CategoryListItem(
                categories.elementAt(index),
                actionOne = actionOne,
                actionOneLabel = actionOneLabel,
                actionTwo = actionTwo,
                actionTwoLabel = actionTwoLabel,
                backgroundColor = backgroundColor
            )
        }
    }
}

/**
 * Renders a basic, lightweight list container for [Category] items.
 * @param categories List of categories to display.
 * @param actionOne Optional primary action callback.
 */
@Composable
fun CategoryListBasic(categories: List<Category>, actionOne: ((Long) -> Unit)? = null) {
    Column {
        categories.forEach { category ->
            CategoryListItem(category, actionOne = actionOne)
        }
    }
}

/**
 * Renders a lazy list container with a border listing [Transaction] items.
 * @param transactions Collection of transactions to display.
 * @param modifier Modifier for layout customization.
 * @param actionOne Optional primary action callback.
 * @param actionOneLabel Label for primary action.
 * @param actionTwo Optional secondary action callback.
 * @param actionTwoLabel Label for secondary action.
 * @param backgroundColor Background color for transaction items.
 */
@Composable
fun TransactionList(
    transactions: Collection<Transaction>,
    modifier: Modifier = Modifier,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String = "Delete",
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String = "Edit",
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .height(500.dp)
            .padding(16.dp)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape)
    ) {
        items(transactions.size) { index ->
            TransactionListItem(
                transactions.elementAt(index),
                actionOne = actionOne,
                actionOneLabel = actionOneLabel,
                actionTwo = actionTwo,
                actionTwoLabel = actionTwoLabel,
                backgroundColor = backgroundColor
            )
        }
    }
}
