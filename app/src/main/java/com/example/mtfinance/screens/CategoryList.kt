package com.example.mtfinance.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mtfinance.src.viewmodels.CategoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.trackingengine.Category

import com.example.mtfinance.src.trackingengine.CategoryWithTransactions


@Composable
fun CategoryListScreen(
    NavHostController: NavHostController,
    categoryViewModel: CategoryViewModel = hiltViewModel()



) {
    val categories by categoryViewModel.filteredCategories.observeAsState()
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()


   DefaultColumn {
        if (selectedCategory == null) {


            CategoryHeader()
            CategorySearch()
            if (categories != null && categories!!.isNotEmpty()) {
                CategoryList(categories!!)
            } else {
                Text("No categories Found", color = MaterialTheme.colorScheme.primary)
            }
        }
        else {
            CategoryDashBoard()
        }
        Button(onClick = { NavHostController.navigate("home") }) {
            Text(text = "Go to Home", color = Color.Yellow)
        }
    }



}

@Composable
fun CategorySearch(categoryViewModel: CategoryViewModel = hiltViewModel()) {
    val searchQuery by categoryViewModel.getSearchQuery().observeAsState()
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Search: ", color = MaterialTheme.colorScheme.primary)
        TextField(searchQuery ?: "", onValueChange = {
            categoryViewModel.setSearchQuery(it)
        })

    }
}
@Composable
fun CategoryDashBoard(
    categoryViewModel: CategoryViewModel = hiltViewModel()
)
{
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(text = "Category Dashboard", color = MaterialTheme.colorScheme.primary)
        if (selectedCategory != null) {
            Text(text = selectedCategory!!.category.name, color = MaterialTheme.colorScheme.primary)
            CategoryDetails(selectedCategory!!)
            SubCategoryList(selectedCategory!!.category.getChildren(false).toList())
            TransactionList(selectedCategory!!)
            Button(onClick = { categoryViewModel.resetSelectedCategory() }) {
                Text(text = "Back", color = Color.Yellow)
            }
        }
        else {
            Text(text = "No category selected", color = MaterialTheme.colorScheme.primary)
        }
    }
}



@Composable
fun CategoryListItem(categoryItem: Category?, categoryViewModel: CategoryViewModel = hiltViewModel()) {
    val expanded = remember { mutableStateOf(false) }
    if (categoryItem != null) {

        Row(content = {

            Button(
                content = { Text(text = categoryItem.name,
                    color = Color.Yellow,
                    fontSize = 14.sp) },
                onClick = { expanded.value = !expanded.value
                    categoryViewModel.setSelectedCategory(categoryItem.categoryId)
                }



            )

        })

    }
}

@Composable
fun TransactionList(categoryItem: CategoryWithTransactions) {
    if (categoryItem.transactions != null && categoryItem.transactions.isNotEmpty()) {
        Text(categoryItem.transactions.toString(), color = Color.White)
    }
    else {
        Text("No transactions", color = MaterialTheme.colorScheme.primary)
    }

}

@Composable
fun CategoryDetails(categoryItem: CategoryWithTransactions) {
    Text(categoryItem.category.description, color = MaterialTheme.colorScheme.primary)
    Text(categoryItem.category.monthlyBudget.toString(), color = MaterialTheme.colorScheme.primary)
    Text(categoryItem.category.type.toString(), color = MaterialTheme.colorScheme.primary)
    if (categoryItem.category.parent != null) {
        CategoryListItem(categoryItem.category.parent)
    }
    else {
        Text("No parent", color = MaterialTheme.colorScheme.primary)
    }


}

@Composable
fun CategoryHeader() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Category List!",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )



    }
}

@Composable
fun CategoryList(categories: List<CategoryWithTransactions>) {
    LazyColumn {
        items(categories.size) { index ->
            CategoryListItem(categories[index].category)
        }
    }
}

@Composable
fun SubCategoryList(categories: List<Category>) {
    LazyColumn {
        items(categories.size) { index ->
            CategoryListItem(categories[index])
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyApp() {

    CategoryHeader()
}

