/**
 * @file SettingsScreen.kt
 * @scope Settings UI screen for persistent app configurations and app-wide commands.
 * @purpose Renders active theme controls, currency, date format, and pay period configurations with provisional state management.
 */
package com.example.mtfinance.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.forms.SelectForm
import com.example.mtfinance.screens.forms.SelectFormNonEditable
import com.example.mtfinance.screens.theme.SaveStatusColor
import com.example.mtfinance.screens.theme.ThemeMode
import com.example.mtfinance.screens.theme.getSaveButtonColors
import com.example.mtfinance.screens.theme.getSaveStatusThemeColor
import com.example.mtfinance.screens.theme.ThemeState
import com.example.mtfinance.src.CurrencyFormat
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.viewmodels.SettingsSaveState
import com.example.mtfinance.src.viewmodels.SettingsViewModel

/**
 * Main Settings screen composable rendering active theme state, display formats, and pay period configurations.
 */
@Composable
fun SettingsScreen(
    navHostController: NavHostController,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val saveState by settingsViewModel.saveState.observeAsState(SettingsSaveState.UNCHANGED)
    val isLoading by settingsViewModel.isLoading.observeAsState(false)
    val genericErrorMessage by settingsViewModel.genericErrorMessage.observeAsState("")
    val successMessage by settingsViewModel.successMessage.observeAsState("")
    val fieldErrors by settingsViewModel.fieldErrors.observeAsState()

    val provThemeMode by settingsViewModel.provisionalThemeMode.observeAsState("SYSTEM")
    val provDateFormat by settingsViewModel.provisionalDateFormat.observeAsState(DateFormat.DD_MM_YYYY)
    val provCurrencyFormat by settingsViewModel.provisionalCurrencyFormat.observeAsState(CurrencyFormat.NZD)
    val provStartDayInput by settingsViewModel.provisionalStartDayOfMonthInput.observeAsState("1")
    val provPageSizeInput by settingsViewModel.provisionalPageSizeInput.observeAsState("10")
    val provIncludeProvisional by settingsViewModel.provisionalIncludeProvisional.observeAsState(true)

    var deleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteTransactionsDialog by remember { mutableStateOf(false) }

    // Sync Room persisted theme to Compose ThemeState
    LaunchedEffect(systemPreferences) {
        systemPreferences?.let { prefs ->
            ThemeState.currentThemeMode = ThemeMode.fromNameOrDefault(prefs.themeMode, ThemeMode.SYSTEM)
        }
    }

    // Discard unsaved provisional changes on exit
    DisposableEffect(Unit) {
        onDispose {
            settingsViewModel.discardProvisionalChanges()
        }
    }

    DefaultColumn(
        modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Header("Settings & Configurations")

        // Top Provisional Warning Banner
        if (saveState == SettingsSaveState.PROVISIONAL) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, getSaveStatusThemeColor(SaveStatusColor.ORANGE), RectangleShape),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = "Provisional changes active — please save changes before exiting.",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (genericErrorMessage.isNotBlank()) {
            Text(text = genericErrorMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
        if (successMessage.isNotBlank()) {
            Text(text = successMessage, color = getSaveStatusThemeColor(SaveStatusColor.GREEN), fontWeight = FontWeight.Bold)
        }

        // 1. Theme Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Choose your preferred theme mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setProvisionalThemeMode("SYSTEM") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = provThemeMode.equals("SYSTEM", ignoreCase = true),
                        onClick = { settingsViewModel.setProvisionalThemeMode("SYSTEM") }
                    )
                    Text("System Default", modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setProvisionalThemeMode("LIGHT") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = provThemeMode.equals("LIGHT", ignoreCase = true),
                        onClick = { settingsViewModel.setProvisionalThemeMode("LIGHT") }
                    )
                    Text("Light Mode", modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setProvisionalThemeMode("DARK") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = provThemeMode.equals("DARK", ignoreCase = true),
                        onClick = { settingsViewModel.setProvisionalThemeMode("DARK") }
                    )
                    Text("Dark Mode", modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 2. Date Format & Currency Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Display Formats & Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Currency Selector
                SelectFormNonEditable(
                    label = "Currency Format",
                    options = CurrencyFormat.entries.map { it.getDisplayName() },
                    selectedOption = provCurrencyFormat.getDisplayName(),
                    onOptionSelected = { selectedName ->
                        val matched = CurrencyFormat.entries.firstOrNull { it.getDisplayName() == selectedName }
                        if (matched != null) {
                            settingsViewModel.setProvisionalCurrencyFormat(matched)
                        }
                    }
                )

                // Date Format Selector (Displays pattern string instead of enum name)
                SelectFormNonEditable(
                    label = "Date Display Format",
                    options = DateFormat.entries.map { it.getString() },
                    selectedOption = provDateFormat.getString(),
                    onOptionSelected = { selectedPattern ->
                        val matched = DateFormat.fromStringOrDefault(selectedPattern, DateFormat.DD_MM_YYYY)
                        settingsViewModel.setProvisionalDateFormat(matched)
                    }
                )
            }
        }

        // 3. Pay Period & Month Start Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pay Period & Month Start",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val dayError = fieldErrors?.get(SettingsViewModel.SystemPreferenceErrorFields.START_DAY_OF_MONTH)
                val isDayError = !dayError.isNullOrBlank()

                TextField(
                    label = { Text("Start Day of Month (1-31)") },
                    value = provStartDayInput,
                    onValueChange = { input -> settingsViewModel.setProvisionalStartDayOfMonth(input) },
                    isError = isDayError,
                    supportingText = {
                        if (isDayError && dayError != null) {
                            Text(text = dayError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                val pageSizeError = fieldErrors?.get(SettingsViewModel.SystemPreferenceErrorFields.PAGE_SIZE)
                val isPageSizeError = !pageSizeError.isNullOrBlank()

                TextField(
                    label = { Text("Page Size (1-100)") },
                    value = provPageSizeInput,
                    onValueChange = { input -> settingsViewModel.setProvisionalPageSize(input) },
                    isError = isPageSizeError,
                    supportingText = {
                        if (isPageSizeError && pageSizeError != null) {
                            Text(text = pageSizeError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setProvisionalIncludeProvisional(!provIncludeProvisional) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = provIncludeProvisional,
                        onCheckedChange = { settingsViewModel.setProvisionalIncludeProvisional(it) }
                    )
                    Text(
                        text = "Include Provisional / Future Days in Period",
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons: Save Changes, Discard Changes & Restore Defaults
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { settingsViewModel.saveChanges() },
                enabled = saveState == SettingsSaveState.PROVISIONAL && !settingsViewModel.hasFieldErrors(),
                modifier = Modifier.weight(1f),
                colors = getSaveButtonColors()
            ) {
                Text("Save Changes")
            }

            OutlinedButton(
                onClick = { settingsViewModel.discardProvisionalChanges() },
                enabled = saveState == SettingsSaveState.PROVISIONAL || saveState == SettingsSaveState.ERROR,
                modifier = Modifier.weight(1f)
            ) {
                Text("Discard Changes")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(
                onClick = { settingsViewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Defaults")
            }
        }

        // 4. Danger Zone
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.error, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Permanently wipe records or delete all transactions.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { deleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete All App Data")
                }
                Button(
                    onClick = { deleteTransactionsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete All Transactions (Retain Categories)")
                }
            }
        }

        if (deleteConfirmDialog) {
            DeleteConfirmationDialog(
                onDismiss = { deleteConfirmDialog = false },
                onConfirm = {
                    deleteConfirmDialog = false
                    settingsViewModel.deleteAllAppData()
                },
                onCancel = { deleteConfirmDialog = false },
                message = "Are you sure you want to permanently delete ALL categories and transactions? This action cannot be undone."
            )
        }

        if (deleteTransactionsDialog) {
            DeleteConfirmationDialog(
                onDismiss = { deleteTransactionsDialog = false },
                onConfirm = {
                    deleteTransactionsDialog = false
                    settingsViewModel.deleteTransactionsOnly()
                },
                onCancel = { deleteTransactionsDialog = false },
                message = "Are you sure you want to permanently delete ALL transactions while retaining categories? This action cannot be undone."
            )
        }

        Button(onClick = { navHostController.navigate(Routes.HOME.route) }) {
            Text("Back to Home")
        }

        if (isLoading) {
            LoadingDialog("Saving settings...")
        }
    }
}
