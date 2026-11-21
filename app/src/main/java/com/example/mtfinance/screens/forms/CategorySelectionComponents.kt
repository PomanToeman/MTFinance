/**
 * @file CategorySelectionComponents.kt
 * @scope Scoped category selection buttons, dialogs, and parent/transaction category container boxes.
 * @purpose Centralizes category selection components for form screens.
 */
package com.example.mtfinance.screens.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.mtfinance.screens.lists.*
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions

/**
 * Allows you to view and choose a category from a list via a dialogue. Can input actions as composables for specific selection actions.
 */
@Composable
fun ChooseCategoryForm(
    categorySelection: List<CategoryWithTransactions?>?,
    chosenCategories: Set<CategoryWithTransactions?>? = null,
    select: ((Long) -> Unit)? = null,
    selectLabel: String? = null,
    remove: ((Long) -> Unit)? = null,
    removeLabel: String? = null,
    dismissOnSelection: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = !showDialog }) {
        Text("Choose Category")
    }

    if (showDialog) {
        CategorySelectionDialog(
            categorySelection = categorySelection,
            chosenCategories = chosenCategories,
            select = select,
            selectLabel = selectLabel,
            remove = remove,
            removeLabel = removeLabel,
            dismissOnSelection = dismissOnSelection,
            onDismissRequest = { showDialog = false }
        )
    }
}

/**
 * Reusable dialog for choosing or toggling categories from categorySelection.
 */
@Composable
fun CategorySelectionDialog(
    categorySelection: List<CategoryWithTransactions?>?,
    chosenCategories: Set<CategoryWithTransactions?>? = null,
    select: ((Long) -> Unit)? = null,
    selectLabel: String? = null,
    remove: ((Long) -> Unit)? = null,
    removeLabel: String? = null,
    dismissOnSelection: Boolean = false,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        "Select Category",
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (chosenCategories == null) {
                    item {
                        if (categorySelection != null) {
                            CategoryList(
                                categories = categorySelection.filterNotNull(),
                                actionOne = {
                                    if (select != null) {
                                        select(it)
                                        if (dismissOnSelection) onDismissRequest()
                                    }
                                },
                                actionOneLabel = selectLabel,
                                actionTwo = remove,
                                actionTwoLabel = removeLabel,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                } else {
                    if (categorySelection != null) {
                        items(categorySelection.size) { index ->
                            val item = categorySelection[index]
                            if (item != null && chosenCategories.contains(item)) {
                                CategoryListItem(
                                    item,
                                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                    actionOne = {
                                        if (remove != null) {
                                            remove(item.category.categoryId)
                                            if (dismissOnSelection) onDismissRequest()
                                        }
                                    },
                                    actionOneLabel = removeLabel
                                )
                            } else if (item != null) {
                                CategoryListItem(
                                    item,
                                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    actionOne = {
                                        if (select != null) {
                                            select(item.category.categoryId)
                                            if (dismissOnSelection) onDismissRequest()
                                        }
                                    },
                                    actionOneLabel = selectLabel
                                )
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(8.dp).fillMaxWidth()
                    ) {
                        Text("Close", textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/**
 * A unified seamless container box that integrates the category selection button,
 * dynamic listing of chosen categories (scrollable after 5 items), individual remove actions,
 * empty state messaging, and field-level error messages.
 */
@Composable
fun CategorySelectionBox(
    label: String,
    chosenCategories: Collection<Category>?,
    categorySelection: List<CategoryWithTransactions?>?,
    buttonLabel: String = "Choose Category",
    emptyMessage: String = "No categories selected",
    isError: Boolean = false,
    errorMessage: String? = null,
    onSelect: ((Long) -> Unit)? = null,
    selectLabel: String = "Select",
    onRemove: ((Long) -> Unit)? = null,
    removeLabel: String = "Remove",
    dismissOnSelection: Boolean = false,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val categoryList = chosenCategories?.toList() ?: emptyList()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (enabled) {
                    Button(onClick = { showDialog = true }) {
                        Text(buttonLabel)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (categoryList.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else if (categoryList.size <= 5) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categoryList.forEach { category ->
                        CategoryListItem(
                            categoryItem = category,
                            actionOne = if (enabled && onRemove != null) { { onRemove(category.categoryId) } } else null,
                            actionOneLabel = if (enabled && onRemove != null) removeLabel else null,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categoryList.size) { index ->
                        val category = categoryList[index]
                        CategoryListItem(
                            categoryItem = category,
                            actionOne = if (enabled && onRemove != null) { { onRemove(category.categoryId) } } else null,
                            actionOneLabel = if (enabled && onRemove != null) removeLabel else null,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            if (isError && !errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showDialog) {
        val chosenSet: Set<CategoryWithTransactions?>? = remember(categorySelection, chosenCategories) {
            if (categorySelection == null || chosenCategories == null) {
                null
            } else {
                val chosenIds = chosenCategories.map { it.categoryId }.toSet()
                categorySelection.filter { it != null && chosenIds.contains(it.category.categoryId) }.toSet()
            }
        }

        CategorySelectionDialog(
            categorySelection = categorySelection,
            chosenCategories = chosenSet,
            select = onSelect,
            selectLabel = selectLabel,
            remove = onRemove,
            removeLabel = removeLabel,
            dismissOnSelection = dismissOnSelection,
            onDismissRequest = { showDialog = false }
        )
    }
}
