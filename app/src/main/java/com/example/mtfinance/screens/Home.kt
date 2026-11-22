package com.example.mtfinance.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.lists.CategoryStackedBar
import com.example.mtfinance.src.viewmodels.HomeViewModel

/**
 * The home screen of the application.
 */
@Composable
fun HomeScreen(NavHostController: NavHostController, homeViewModel: HomeViewModel = hiltViewModel()) {
    val pinnedCategoryViewModel by homeViewModel.pinnedCategory.observeAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MiniHeader("General Category")
        if (pinnedCategoryViewModel != null) {
            CategoryStackedBar(categoryDashboardViewModel = pinnedCategoryViewModel!!)
        }
        Text("Home Screen")
        Button(
            onClick = { NavHostController.navigate(Routes.CATEGORY.route) }
        ) {
            Text("Go to Category List")
        }
    }
}
