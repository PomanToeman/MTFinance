/**
 * @file SettingsScreen.kt
 * @scope Settings UI screen for persistent app configurations and app-wide commands.
 * @purpose Renders active theme controls, currency, date format, and pay period configurations.
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
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.theme.ThemeMode
import com.example.mtfinance.screens.theme.ThemeState
import com.example.mtfinance.src.CurrencyFormat
import com.example.mtfinance.src.DateFormat
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
    var deleteConfirmDialog by remember { mutableStateOf(false) }

    var currencyMenu by remember { mutableStateOf(false) }
    var dateFormatMenu by remember { mutableStateOf(false) }

    // Sync Room persisted theme to Compose ThemeState
    LaunchedEffect(systemPreferences) {
        systemPreferences?.let { prefs ->
            try {
                ThemeState.currentThemeMode = ThemeMode.valueOf(prefs.themeMode)
            } catch (_: Exception) {
            }
        }
    }

    DefaultColumn(
        modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Header("Settings & Configurations")

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
                    text = "Choose your preferred theme mode. Saved directly to database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val currentMode = systemPreferences?.themeMode ?: "SYSTEM"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setThemeMode("SYSTEM") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == "SYSTEM",
                        onClick = { settingsViewModel.setThemeMode("SYSTEM") }
                    )
                    Text("System Default", modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setThemeMode("LIGHT") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == "LIGHT",
                        onClick = { settingsViewModel.setThemeMode("LIGHT") }
                    )
                    Text("Light Mode", modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setThemeMode("DARK") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == "DARK",
                        onClick = { settingsViewModel.setThemeMode("DARK") }
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
                val currentCurrency = CurrencyFormat.fromString(systemPreferences?.currencyFormat ?: "NZD")
                TextField(
                    label = { Text("Currency Format") },
                    value = currentCurrency.getDisplayName(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { currencyMenu = !currencyMenu }) {
                            Icon(imageVector = Icons.Default.List, contentDescription = "Currency List")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (currencyMenu) {
                    CurrencyFormat.values().forEach { cur ->
                        DropdownMenuItem(
                            text = { Text(cur.getDisplayName()) },
                            onClick = {
                                settingsViewModel.setCurrencyFormat(cur)
                                currencyMenu = false
                            }
                        )
                    }
                }

                // Date Format Selector
                val currentDateFmt = systemPreferences?.dateFormat ?: "DD_MM_YYYY"
                TextField(
                    label = { Text("Date Display Format") },
                    value = currentDateFmt,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { dateFormatMenu = !dateFormatMenu }) {
                            Icon(imageVector = Icons.Default.List, contentDescription = "Date Format List")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (dateFormatMenu) {
                    DateFormat.values().forEach { fmt ->
                        DropdownMenuItem(
                            text = { Text(fmt.toString()) },
                            onClick = {
                                settingsViewModel.setDateFormat(fmt)
                                dateFormatMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 3. Pay Period & Time Window Settings
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
                Text(
                    text = "Configure start day of month (1-31).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val startDay = systemPreferences?.startDayOfMonth ?: 1
                TextField(
                    label = { Text("Start Day of Month (1-31)") },
                    value = startDay.toString(),
                    onValueChange = { input ->
                        try {
                            val day = input.toInt()
                            if (day in 1..31) {
                                settingsViewModel.setStartDayOfMonth(day)
                            }
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                val includeProv = systemPreferences?.includeProvisional ?: true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setIncludeProvisional(!includeProv) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeProv,
                        onCheckedChange = { settingsViewModel.setIncludeProvisional(it) }
                    )
                    Text(
                        text = "Include Provisional / Future Days in Period",
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Data Export & Import (Roadmap Placeholder)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RectangleShape),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data Import & Export (Coming Soon)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Export transactions and category hierarchies to CSV backups.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { }, enabled = false) { Text("Export Transactions") }
                    OutlinedButton(onClick = { }, enabled = false) { Text("Export Categories") }
                }
            }
        }

        // 5. Danger Zone
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.error, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Permanently wipe all transaction and category records.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { deleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All App Data")
                }
            }
        }

        if (deleteConfirmDialog) {
            DeleteConfirmationDialog(
                onDismiss = { deleteConfirmDialog = false },
                onConfirm = { deleteConfirmDialog = false },
                onCancel = { deleteConfirmDialog = false },
                message = "Are you sure you want to permanently delete ALL categories and transactions? This action cannot be undone."
            )
        }

        Button(onClick = { navHostController.navigate(Routes.HOME.route) }) {
            Text("Back to Home")
        }
    }
}
