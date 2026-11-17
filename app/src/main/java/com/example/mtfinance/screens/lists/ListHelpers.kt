/**
 * @file ListHelpers.kt
 * @scope Reusable list containers and search input components for categories and transactions.
 * @purpose Centralizes list UI wrappers, list renderers, and search filtering text fields.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.Transaction

/**
 * Renders a search input field for filtering lists.
 * @param search The current search query string.
 * @param setSearch Callback invoked when the search query changes.
 */
@Composable
fun Search(search: String?, setSearch: ((String) -> Unit)?) {
    OutlinedTextField(
        value = search ?: "",
        onValueChange = { setSearch?.invoke(it) },
        label = { Text("Search") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
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
