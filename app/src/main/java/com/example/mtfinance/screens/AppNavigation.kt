package com.example.mtfinance.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mtfinance.screens.forms.*
import com.example.mtfinance.screens.lists.*
import com.example.mtfinance.screens.theme.ThemeMode
import com.example.mtfinance.screens.theme.ThemeState
import com.example.mtfinance.src.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val navController = rememberNavController()

    LaunchedEffect(systemPreferences) {
        systemPreferences?.let { prefs ->
            try {
                ThemeState.currentThemeMode = ThemeMode.valueOf(prefs.themeMode)
            } catch (_: Exception) {
            }
        }
    }

    SetScaffolding(content = {
        NavHost(navController, startDestination = Routes.HOME.route) {
            composable(Routes.HOME.route) { HomeScreen(navController) }
            composable(Routes.CATEGORY.route) { CategoryListScreen(navController) }
            composable(Routes.CATEGORY_FORM.route) { CategoryFormScreen(navHostController = navController) }
            composable(Routes.CATEGORY_FORM_EDIT.route) { backStackEntry -> CategoryFormScreen(navHostController = navController, categoryId = backStackEntry.arguments?.getString("categoryId")?.toLong()) }
            composable(Routes.TRANSACTION.route) { TransactionListScreen(navHostController = navController) }
            composable(Routes.TRANSACTION_FORM_EDIT.route) { backStackEntry -> TransactionFormScreen(navHostController = navController, transactionId = backStackEntry.arguments?.getString("transactionId")?.toLong()) }
            composable(Routes.TRANSACTION_FORM.route) { TransactionFormScreen(navHostController = navController) }
            composable(Routes.TRANSACTION_IMPORT.route) { TransactionImportScreen(navHostController = navController) }
            composable(Routes.SETTINGS.route) { SettingsScreen(navHostController = navController) }
        }
    }, nav = navController)
}

enum class Routes(val route: String) {
    HOME("home"),
    CATEGORY("category"),
    TRANSACTION("transaction"),
    TRANSACTION_FORM("transactionForm"),
    TRANSACTION_FORM_EDIT("transactionForm/{transactionId}"),
    TRANSACTION_IMPORT("transactionImport"),
    CATEGORY_FORM("categoryForm"),
    CATEGORY_FORM_EDIT("categoryForm/{categoryId}"),
    SETTINGS("settings");

    fun createCategoryEditRoute(categoryId: Long): String = "categoryForm/$categoryId"
    fun createTransactionEditRoute(transactionId: Long): String = "transactionForm/$transactionId"
}
