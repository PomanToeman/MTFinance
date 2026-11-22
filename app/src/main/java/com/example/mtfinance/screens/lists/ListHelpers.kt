/**
 * @file ListHelpers.kt
 * @scope Reusable list containers and search input components for categories and transactions.
 * @purpose Centralizes list UI wrappers, list renderers, search filtering, and universal pagination.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.DefaultSettings
import com.example.mtfinance.src.trackingengine.Transaction
import com.example.mtfinance.src.trackingengine.TrackingType
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.utlis.PaginationHelper
import com.example.mtfinance.src.viewmodels.SettingsViewModel

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
 * Renders a universal pagination bar showing item counts and page navigation.
 */
@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    shownCount: Int,
    onPageChanged: (Int) -> Unit
) {
    if (totalItems > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Showing $shownCount of $totalItems items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (currentPage > 1) onPageChanged(currentPage - 1) },
                    enabled = currentPage > 1
                ) {
                    Text(ButtonMessages.PREV.getMessage())
                }
                Text(
                    text = "Page $currentPage of ${Math.max(1, totalPages)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { if (currentPage < totalPages) onPageChanged(currentPage + 1) },
                    enabled = currentPage < totalPages
                ) {
                    Text(ButtonMessages.NEXT.getMessage())
                }
            }
        }
    }
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
 * Renders a list container with a border listing [CategoryWithTransactions] items and pagination.
 */
@Composable
fun CategoryList(
    categories: Collection<CategoryWithTransactions>,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    updateTrigger: Long? = 0,
    pageSize: Int? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val effectivePageSize = pageSize ?: (systemPreferences?.pageSize ?: DefaultSettings.DEFAULT_PAGE_COUNT)
    var currentPage by remember(effectivePageSize) { mutableStateOf(1) }

    val itemList = categories.toList()
    val totalItems = itemList.size
    val totalPages = PaginationHelper.calculateTotalPages(totalItems, effectivePageSize)
    if (currentPage > totalPages) {
        currentPage = Math.max(1, totalPages)
    }
    val pageItems = PaginationHelper.getPageSlice(itemList, currentPage, effectivePageSize)

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(8.dp)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape)
        ) {
            items(
                pageItems.size,
                key = { index -> "${pageItems[index].category.categoryId}_${updateTrigger}_${currentPage}" }
            ) { index ->
                CategoryListItem(
                    pageItems[index],
                    actionOne = actionOne,
                    actionOneLabel = actionOneLabel,
                    actionTwo = actionTwo,
                    actionTwoLabel = actionTwoLabel,
                    backgroundColor = backgroundColor
                )
            }
        }
        PaginationBar(
            currentPage = currentPage,
            totalPages = totalPages,
            totalItems = totalItems,
            shownCount = pageItems.size,
            onPageChanged = { currentPage = it }
        )
    }
}

/**
 * Renders a lightweight list container for plain [Category] items with pagination.
 */
@Composable
fun CategoryListBasic(
    categories: Collection<Category>,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String? = null,
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String? = null,
    pageSize: Int? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val effectivePageSize = pageSize ?: (systemPreferences?.pageSize ?: DefaultSettings.DEFAULT_PAGE_COUNT)
    var currentPage by remember(effectivePageSize) { mutableStateOf(1) }

    val itemList = categories.toList()
    val totalItems = itemList.size
    val totalPages = PaginationHelper.calculateTotalPages(totalItems, effectivePageSize)
    if (currentPage > totalPages) {
        currentPage = Math.max(1, totalPages)
    }
    val pageItems = PaginationHelper.getPageSlice(itemList, currentPage, effectivePageSize)

    Column(modifier = Modifier.fillMaxWidth()) {
        pageItems.forEach { category ->
            CategoryListItem(
                category,
                actionOne = actionOne,
                actionOneLabel = actionOneLabel,
                actionTwo = actionTwo,
                actionTwoLabel = actionTwoLabel
            )
        }
        PaginationBar(
            currentPage = currentPage,
            totalPages = totalPages,
            totalItems = totalItems,
            shownCount = pageItems.size,
            onPageChanged = { currentPage = it }
        )
    }
}

/**
 * Renders a list container with a border listing [Transaction] items and pagination.
 */
@Composable
fun TransactionList(
    transactions: Collection<Transaction>,
    modifier: Modifier = Modifier,
    actionOne: ((Long) -> Unit)? = null,
    actionOneLabel: String = ButtonMessages.DELETE.getMessage(),
    actionTwo: ((Long) -> Unit)? = null,
    actionTwoLabel: String = ButtonMessages.EDIT.getMessage(),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    pageSize: Int? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val effectivePageSize = pageSize ?: (systemPreferences?.pageSize ?: DefaultSettings.DEFAULT_PAGE_COUNT)
    var currentPage by remember(effectivePageSize) { mutableStateOf(1) }

    val itemList = transactions.toList()
    val totalItems = itemList.size
    val totalPages = PaginationHelper.calculateTotalPages(totalItems, effectivePageSize)
    if (currentPage > totalPages) {
        currentPage = Math.max(1, totalPages)
    }
    val pageItems = PaginationHelper.getPageSlice(itemList, currentPage, effectivePageSize)

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(8.dp)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape)
        ) {
            items(
                pageItems.size,
                key = { index -> "${pageItems[index].transactionId}_${currentPage}" }
            ) { index ->
                TransactionListItem(
                    pageItems[index],
                    actionOne = actionOne,
                    actionOneLabel = actionOneLabel,
                    actionTwo = actionTwo,
                    actionTwoLabel = actionTwoLabel,
                    backgroundColor = backgroundColor
                )
            }
        }
        PaginationBar(
            currentPage = currentPage,
            totalPages = totalPages,
            totalItems = totalItems,
            shownCount = pageItems.size,
            onPageChanged = { currentPage = it }
        )
    }
}
