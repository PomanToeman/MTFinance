/**
 * @file CategoryListScreen.kt
 * @scope Main screen composable for category listing, searching, filtering, and dashboard navigation.
 * @purpose Renders the list of categories with filtering options, search input, and dedicated bordered icon buttons between header and search.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.DefaultColumn
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.viewmodels.CategoryViewModel
import com.example.mtfinance.src.viewmodels.CategoryDashboardViewModel

/**
 * Main screen composable displaying categories, dashboard toggle, and actions.
 */
@Composable
fun CategoryListScreen(
    navHostController: NavHostController,
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    categoryDashboardViewModel: CategoryDashboardViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.filteredCategories.observeAsState()
    val selectedCategory by categoryDashboardViewModel.getSelectedCategory().observeAsState()
    val searchQuery by categoryViewModel.searchQuery.observeAsState()
    val typeFilter by categoryViewModel.typeFilter.observeAsState()
    val updateTrigger by categoryViewModel.updateTrigger.observeAsState()

    DefaultColumn(modifier = Modifier.padding(4.dp).verticalScroll(rememberScrollState())) {
        if (selectedCategory == null) {
            Header("Category List")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(onClick = { navHostController.navigate(Routes.CATEGORY_FORM.route) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = ButtonMessages.ADD.getMessage())
                    }
                }
            }
            Search(searchQuery) { categoryViewModel.setSearchQuery(it) }
            ChooseTypeButton(typeFilter, { type -> categoryViewModel.setTypeFilter(type) }, includeNone = true)

            if (!categories.isNullOrEmpty()) {
                CategoryList(
                    categories!!,
                    actionOne = { categoryId: Long -> categoryDashboardViewModel.setSelectedCategory(categoryId) },
                    actionOneLabel = "Show More",
                    actionTwo = { categoryId: Long -> navHostController.navigate(Routes.CATEGORY_FORM.route + "/" + categoryId) },
                    actionTwoLabel = "Edit",
                    updateTrigger = updateTrigger
                )
            } else {
                Text(text = "No categories found", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            FullCategoryDashBoard(navHostController = navHostController, categoryDashboardViewModel = categoryDashboardViewModel)
        }
    }
}
