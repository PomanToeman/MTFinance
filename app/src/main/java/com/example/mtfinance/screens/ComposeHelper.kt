/**
 * @file ComposeHelper.kt
 * @scope App-wide Compose scaffolding, top/bottom navigation bars, and reusable helper composables.
 * @purpose Provides unified layout scaffolding (SetScaffolding), enumerated bottom navigation bar, and helper components.
 */
package com.example.mtfinance.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.theme.MTFinanceTheme
import com.example.mtfinance.src.trackingengine.TrackingType

/**
 * Enumerated navigation destinations for the bottom app bar.
 */
enum class BottomNavDestination(val route: String, val icon: ImageVector, val label: String) {
    CATEGORY(Routes.CATEGORY.route, Icons.Default.Category, "Category"),
    TRANSACTION(Routes.TRANSACTION.route, Icons.Default.Receipt, "Transaction"),
    HOME(Routes.HOME.route, Icons.Default.Home, "Home")
}

/**
 * Sets root content activity with MTFinanceTheme.
 */
fun setComposeContent(activity: ComponentActivity, content: @Composable () -> Unit) {
    activity.setContent {
        MTFinanceTheme {
            content()
        }
    }
}

/**
 * Form helper component for selecting tracking type.
 */
@Composable
fun ChooseTypeForm(
    typeValue: TrackingType?,
    onTypeSelected: (TrackingType?) -> Unit,
    enabled: Boolean = true
) {
    com.example.mtfinance.screens.lists.ChooseTypeButton(
        typeValue = typeValue,
        onTypeSelected = onTypeSelected,
        includeNone = true
    )
}

/**
 * Reusable bottom navigation bar helper composable rendering 1/3-width clickable cards with icon and line dividers.
 */
@Composable
fun BottomNavigationBar(nav: NavHostController) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val destinations = BottomNavDestination.entries
            destinations.forEachIndexed { index, destination ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { nav.navigate(destination.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (index < destinations.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

/**
 * Global screen scaffolding layout providing top app bar and enumerated bottom navigation bar.
 */
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
                BottomNavigationBar(nav)
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                content()
            }
        }
    }
}
