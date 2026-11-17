/**
 * @file CategoryListScreen.kt
 * @scope Main screen composable for category listing, searching, filtering, and dashboard navigation.
 * @purpose Renders the list of categories with filtering options, search input, and FAB for creation.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.ChooseTypeForm
import com.example.mtfinance.screens.DefaultColumn
import com.example.mtfinance.screens.FabRightBottomCorner
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.src.viewmodels.CategoryViewModel

/**
 * Main screen composable displaying categories, dashboard toggle, and actions.
 */
@Composable
fun CategoryListScreen(
    navHostController: NavHostController,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.filteredCategories.observeAsState()
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    val searchQuery by categoryViewModel.searchQuery.observeAsState()
    val typeFilter by categoryViewModel.typeFilter.observeAsState()
    val updateTrigger by categoryViewModel.updateTrigger.observeAsState()

    FabRightBottomCorner(
        actionOne = { navHostController.navigate(Routes.CATEGORY_FORM.route) },
        content = {
            DefaultColumn(modifier = Modifier.padding(4.dp).verticalScroll(rememberScrollState())) {
                if (selectedCategory == null) {
                    Header("Category List")
                    Search(searchQuery, { categoryViewModel.setSearchQuery(it) })
                    ChooseTypeForm(typeFilter, { type -> categoryViewModel.setTypeFilter(type) }, includeNone = true)

                    if (categories != null && categories!!.isNotEmpty()) {
                        CategoryList(
                            categories!!,
                            actionOne = { categoryId: Long -> categoryViewModel.setSelectedCategory(categoryId) },
                            actionOneLabel = "Show More",
                            actionTwo = { categoryId: Long -> navHostController.navigate(Routes.CATEGORY_FORM.route + "/" + categoryId) },
                            actionTwoLabel = "Edit",
                            updateTrigger = updateTrigger
                        )
                    } else {
                        Text(text = "No categories found", color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    FullCategoryDashBoard(categoryViewModel = categoryViewModel)
                }
            }
        },
        iconImageOne = Icons.Default.Add
    )
}
