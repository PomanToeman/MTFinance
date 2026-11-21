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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.mtfinance.screens.lists.*
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.trackingengine.Category
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Allows you to view and choose a category from a list via a dialogue. Can input actions as composables for specific selection actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            color = Color.White
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        "Select Category",
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
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
                                backgroundColor = Color.LightGray
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
                                    backgroundColor = Color.Green,
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
                                    backgroundColor = Color.LightGray,
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
                        Text("Close", textAlign = TextAlign.Right)
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

/**
 * Allows the user to enter a date manually via a text field (supporting ISO yyyy-MM-dd and dd/MM/yyyy formats)
 * or choose a date from a DatePickerDialog backup option.
 *
 * Date parsing and formatting occur ONLY when the user presses Enter/Done or exits the field (focus loss).
 * When re-focusing, the text reverts to the raw entered format (or dd/MM/yyyy if chosen via calendar dialog).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String = "Date",
    value: LocalDateTime?,
    onValueChange: (LocalDateTime?) -> Unit,
    onTextChange: ((String) -> Unit)? = null,
    systemDateFormat: DateFormat = DateFormat.DD_MM_YYYY,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val formattedSystemText = remember(value, systemDateFormat) {
        if (value != null) {
            try {
                value.format(systemDateFormat.toFormatter())
            } catch (e: Exception) {
                value.toString()
            }
        } else {
            ""
        }
    }

    var rawInputText by remember(formattedSystemText) { mutableStateOf(formattedSystemText) }

    val commitDate = remember(rawInputText) {
        {
            if (onTextChange != null) {
                onTextChange(rawInputText)
            } else {
                val parsed = DateFormat.parseSimple(rawInputText)
                onValueChange(parsed)
            }
        }
    }

    val displayValue = if (isFocused) {
        rawInputText
    } else {
        if (!isError && value != null) {
            formattedSystemText
        } else {
            rawInputText
        }
    }

    val initialMillis = remember(value) {
        value?.toLocalDate()?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    TextField(
        value = displayValue,
        onValueChange = { newText ->
            rawInputText = newText
        },
        label = { Text(label) },
        enabled = enabled,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { commitDate() },
            onNext = { commitDate() }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val prevFocused = isFocused
                isFocused = focusState.isFocused
                if (prevFocused && !focusState.isFocused) {
                    commitDate()
                }
            },
        supportingText = {
            if (isError && !errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(
                    onClick = { showDialog = true },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Select Date from Calendar"
                    )
                }
            }
        }
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedLocalDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val selectedDateTime = selectedLocalDate.atStartOfDay()
                            val ddMmYyyyFormatted = selectedLocalDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            rawInputText = ddMmYyyyFormatted
                            if (onTextChange != null) {
                                onTextChange(ddMmYyyyFormatted)
                            } else {
                                onValueChange(selectedDateTime)
                            }
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Overload for backwards compatibility */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(value: String? = null, valuelong: LocalDate?, onValueChange: (Long?) -> Unit, enabled: Boolean = true) {
    val dateTime = valuelong?.atStartOfDay()
    DatePickerField(
        label = "Date",
        value = dateTime,
        onValueChange = { dt ->
            onValueChange(dt?.toLocalDate()?.toEpochDay()?.times(1000 * 60 * 60 * 24))
        },
        enabled = enabled
    )
}

@Composable
fun TextFieldForm(label: String, value: String?, onValueChange: (String) -> Unit, minLines: Int = 1, maxLines: Int = 1, placeholder: String = "", enabled: Boolean = true, isError: Boolean = false, errorMessage: String? = "") {
    TextField(label = { Text(label) }, value = value ?: "", onValueChange = onValueChange, minLines = minLines, maxLines = maxLines, placeholder = { Text(placeholder)}, enabled = enabled, isError = isError,
        supportingText = {
            if (isError) {
                if (errorMessage != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        trailingIcon = {
            if (isError) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        })
}

/**
 * Allows you to input an amount into a field with a given setter. Allows dollars and cents (via decimal)
 */
@Composable
fun NumberFieldForm(label: String, value: BigDecimal?, setter: (BigDecimal?) -> Unit, enabled: Boolean = true, isError: Boolean = false, errorMessage: String? = "") {
    OutlinedTextField(
        label = { Text(label) },
        value = value?.toString() ?: "",
        prefix = { Text("$ ") },
        placeholder = { Text("0.00") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        supportingText = {
            if (isError) {
                if (errorMessage != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        trailingIcon = {
            if (isError) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        onValueChange = {input ->
            val filteredInput = input.filter { it.isDigit() || it == '.' }

            if (filteredInput.isNotEmpty() ) {

                if (filteredInput.chars().filter { ch -> ch.toChar() == '.' }.count() == 1L) {
                    if (filteredInput.last() == '.') {
                        setter(BigDecimal(filteredInput + "0"))
                        return@OutlinedTextField
                    }
                    if (filteredInput.first() == '.') {
                        setter(BigDecimal("0" + filteredInput))
                        return@OutlinedTextField
                    }
                    else {
                        val parts = filteredInput.split(".")
                        if (parts[1].length > 2) {
                            setter(BigDecimal(parts[0] + "." + parts[1].substring(0, 2)))
                            return@OutlinedTextField
                        }
                    }
                }


                try {
                    setter(BigDecimal(filteredInput))
                } catch (_: NumberFormatException) {
                    setter(value)
                }
            }
            else {
                setter(null)
            }


        },
        enabled = enabled

    )
}

/**
 * Allows you either type in or select from a list of options into a field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectForm(label: String, options: List<String?>?, selectedOption: String?, onOptionSelected: (String) -> Unit) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var textFieldValue: String? by remember { mutableStateOf(selectedOption ?: "") }
    val filteredOptions = options?.filter { textFieldValue?.let { other -> it?.contains(other, ignoreCase = true) }
        ?: false }

    ExposedDropdownMenuBox(
        expanded = isMenuExpanded,
        onExpandedChange = { isMenuExpanded = !isMenuExpanded },
    ) {
        TextField(
            value = textFieldValue ?: "",
            onValueChange = {value -> textFieldValue = value; onOptionSelected(value)},
            label = { Text(label) },
            trailingIcon = { IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded)
            }})

    }

    if (filteredOptions?.isNotEmpty() ?: false && isMenuExpanded) {
        filteredOptions.forEach { selectionOption ->
            DropdownMenuItem(
                text = {
                    if (selectionOption != null) {
                        Text(selectionOption)
                    }
                },
                onClick = {
                    textFieldValue = selectionOption
                    onOptionSelected(selectionOption ?: "")
                    isMenuExpanded = false
                }
            )
        }
    }
}
