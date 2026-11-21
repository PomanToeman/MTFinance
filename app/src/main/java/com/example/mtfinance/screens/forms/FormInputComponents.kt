/**
 * @file FormInputComponents.kt
 * @scope Scoped UI helper components for form text, number, select, and date input fields.
 * @purpose Centralizes form input fields with standardized OutlinedTextField styling and spacing constants.
 */
package com.example.mtfinance.screens.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mtfinance.src.DateFormat
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val FORM_SPACING_UNIT = 16.dp
val FORM_SECTION_SPACING = 32.dp

/**
 * Hybrid Date Picker Field matching other form OutlinedTextFields identically.
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

    val initialMillis = remember(value) {
        value?.toLocalDate()?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Input,
        initialSelectedDateMillis = initialMillis
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formattedSystemText,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("Select Date") },
            readOnly = true,
            enabled = enabled,
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
                    if (enabled && value != null) {
                        IconButton(
                            onClick = {
                                if (onTextChange != null) {
                                    onTextChange("")
                                } else {
                                    onValueChange(null)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear Date"
                            )
                        }
                    }
                    if (isError) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = { if (enabled) showDialog = true },
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

        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 96.dp)
                    .clickable { showDialog = true }
            )
        }
    }

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
                            if (onTextChange != null) {
                                onTextChange(selectedLocalDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
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
fun TextFieldForm(
    label: String,
    value: String?,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    maxLines: Int = 1,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = ""
) {
    OutlinedTextField(
        value = value ?: "",
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        maxLines = maxLines,
        placeholder = { Text(placeholder) },
        enabled = enabled,
        isError = isError,
        singleLine = maxLines == 1,
        modifier = Modifier.fillMaxWidth(),
        supportingText = {
            if (isError && !errorMessage.isNullOrBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
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
        }
    )
}

/**
 * Allows you to input an amount into a field with a given setter. Allows dollars and cents (via decimal)
 */
@Composable
fun NumberFieldForm(
    label: String,
    value: BigDecimal?,
    setter: (BigDecimal?) -> Unit,
    currencySymbol: String = "$",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = ""
) {
    OutlinedTextField(
        label = { Text(label) },
        value = value?.toString() ?: "",
        prefix = { Text("$currencySymbol ") },
        placeholder = { Text("0.00") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        supportingText = {
            if (isError && !errorMessage.isNullOrBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
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
        onValueChange = { input ->
            val filteredInput = input.filter { it.isDigit() || it == '.' }

            if (filteredInput.isNotEmpty()) {
                if (filteredInput.chars().filter { ch -> ch.toChar() == '.' }.count() == 1L) {
                    if (filteredInput.last() == '.') {
                        setter(BigDecimal(filteredInput + "0"))
                        return@OutlinedTextField
                    }
                    if (filteredInput.first() == '.') {
                        setter(BigDecimal("0" + filteredInput))
                        return@OutlinedTextField
                    } else {
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
            } else {
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
fun SelectForm(
    label: String,
    options: List<String?>?,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var textFieldValue by remember(selectedOption) { mutableStateOf(selectedOption ?: "") }
    val filteredOptions = options?.filter { opt ->
        opt != null && (textFieldValue.isBlank() || opt.contains(textFieldValue, ignoreCase = true))
    } ?: emptyList()

    ExposedDropdownMenuBox(
        expanded = isMenuExpanded,
        onExpandedChange = { isMenuExpanded = !isMenuExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { value ->
                textFieldValue = value
                onOptionSelected(value)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded)
            }
        )

        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                filteredOptions.forEach { selectionOption ->
                    if (selectionOption != null) {
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                textFieldValue = selectionOption
                                onOptionSelected(selectionOption)
                                isMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
