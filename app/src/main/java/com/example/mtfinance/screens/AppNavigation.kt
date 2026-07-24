package com.example.mtfinance.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SetScaffolding(content = {

        NavHost(navController, startDestination = "home") {

            composable(Routes.HOME.route) { HomeScreen(navController) }
            composable(Routes.CATEGORY.route) { CategoryListScreen(navController) }
            composable(Routes.CATEGORY_FORM.route) { CategoryFormScreen(navHostController = navController) }
            composable(Routes.TRANSACTION.route) { TransactionListScreen(navHostController = navController) }
            composable(Routes.TRANSACTION_FORM_EDIT.route) { backStackEntry -> TransactionFormScreen(navHostController = navController, transactionId = backStackEntry.arguments?.getString("transactionId")?.toLong()) }
            composable(Routes.TRANSACTION_FORM.route) { TransactionFormScreen(navHostController = navController) }







        }


    }, nav = navController)



}

enum class Routes {
    HOME("home"),
    CATEGORY("category"),
    TRANSACTION("transaction"),
    TRANSACTION_FORM("transactionForm"),
    TRANSACTION_FORM_EDIT("transactionForm/{transactionId}"),
    TRANSACTION_IMPORT("transactionImport"),
    CATEGORY_FORM("categoryForm"),
    ;

    val route: String
    constructor(route: String) {
        this.route = route
    }



}

