package com.example.mtfinance.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.theme.MTFinanceTheme
import com.example.mtfinance.src.trackingengine.TrackingType

fun setComposeContent(activity: ComponentActivity, content: @Composable () -> Unit) {
    activity.setContent {
        MTFinanceTheme {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetScaffolding(content: @Composable () -> Unit, nav: NavHostController) {
    MTFinanceTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mt finance") },
                    actions = {
                        IconButton(onClick = { nav.navigate(Routes.SETTINGS.route) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(modifier = Modifier.padding(11.dp)) {
                    Button(onClick = { nav.navigate("category") }, modifier = Modifier.padding(11.dp)) {
                        Text("Category")
                    }
                    Button(onClick = { nav.navigate("transaction") }) {
                        Text("Transaction")
                    }
                    Button(onClick = { nav.navigate("home") }) {
                        Text("Home")
                    }
                }
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseTypeForm(
    typeValue: TrackingType?,
    onTypeSelected: (TrackingType?) -> Unit,
    enabled: Boolean = true,
    includeNone: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var rawText by remember(typeValue) {
        mutableStateOf(typeValue?.toString()?.lowercase() ?: if (includeNone) "none" else "")
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = rawText,
            onValueChange = { input ->
                rawText = input
                val matched = TrackingType.entries.firstOrNull { it.name.equals(input.trim(), ignoreCase = true) }
                if (matched != null) {
                    onTypeSelected(matched)
                } else if (includeNone && input.trim().equals("none", ignoreCase = true)) {
                    onTypeSelected(null)
                }
            },
            label = { Text("Type") },
            readOnly = false,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (type in TrackingType.entries) {
                DropdownMenuItem(
                    text = { Text(type.toString().lowercase()) },
                    onClick = {
                        rawText = type.toString().lowercase()
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
            if (includeNone) {
                DropdownMenuItem(
                    text = { Text("none") },
                    onClick = {
                        rawText = "none"
                        onTypeSelected(null)
                        expanded = false
                    }
                )
            }
        }
    }
}
