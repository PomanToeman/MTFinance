package com.example.mtfinance.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.mtfinance.screens.theme.ThemeMode
import com.example.mtfinance.screens.theme.ThemeState
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
        val isSystemDark = isSystemInDarkTheme()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mt finance") },
                    actions = {
                        val (icon, label) = when (ThemeState.currentThemeMode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "System"
                            ThemeMode.LIGHT -> Icons.Default.LightMode to "Light"
                            ThemeMode.DARK -> Icons.Default.DarkMode to "Dark"
                        }
                        TextButton(onClick = { ThemeState.toggleTheme(isSystemDark) }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Theme: $label"
                            )
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.labelMedium
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

@Composable
fun ChooseTypeForm(typeValue: TrackingType?, onTypeSelected: (TrackingType?) -> Unit, enabled: Boolean = true, includeNone: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.padding(16.dp)
    ) {
        Button(onClick = { expanded = !expanded }, enabled = enabled) {
            if (typeValue != null) {
                Text("Type: " + typeValue.toString().lowercase())
            } else {
                Text("Type: None")
            }
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
                    text = { Text("None") },
                    onClick = {
                        onTypeSelected(null)
                        expanded = false
                    }
                )
            }
        }
    }
}
