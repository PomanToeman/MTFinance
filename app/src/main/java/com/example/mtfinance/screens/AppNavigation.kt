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

            composable("home") { HomeScreen(navController) }
            composable("category") { CategoryListScreen(navController) }
            composable("transaction") { TransactionListScreen(navHostController = navController) }
            composable("transactionForm") { TransactionFormScreen(navHostController = navController) }





        }


    }, nav = navController)



}