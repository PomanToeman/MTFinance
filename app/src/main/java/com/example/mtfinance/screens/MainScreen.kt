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

import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import okhttp3.internal.http2.Header


@Composable
fun CategoryListScreen(
    categoryViewModel: CategoryViewModel = hiltViewModel()

) {
    val categories by categoryViewModel.allCategories.observeAsState()
    Header
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryHeader()
        if (categories != null) {
            CategoryList(categories!!)
        }
        else {
            Text("No categories", color = Color.White)
        }
        CategoryDashBoard()

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
        Text(text = "Category Dashboard", color = Color.White)
        if (selectedCategory != null) {
            Text(text = selectedCategory!!.category.name, color = Color.White)
            CategoryDetails(selectedCategory!!)
            TransactionList(selectedCategory!!)
        }
        else {
            Text(text = "No category selected", color = Color.White)
        }
    }
}

@Composable
fun CategoryListItem(categoryItem: CategoryWithTransactions?, categoryViewModel: CategoryViewModel = hiltViewModel()) {
    val expanded = remember { mutableStateOf(false) }
    if (categoryItem != null) {

        Row(content = {

            Button(
                content = { Text(text = categoryItem.details,
                    color = Color.Yellow,
                    fontSize = 14.sp) },
                onClick = { expanded.value = !expanded.value
                    categoryViewModel.setSelectedCategory(categoryItem.category.categoryId)
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
        Text("No transactions", color = Color.White)
    }

}

@Composable
fun CategoryDetails(categoryItem: CategoryWithTransactions) {
    Text(categoryItem.category.description, color = Color.White)
    Text(categoryItem.category.monthlyBudget.toString(), color = Color.White)
    Text(categoryItem.category.type.toString(), color = Color.White)
    if (categoryItem.category.parent != null) {
        Text(categoryItem.category.parent.toString(), color = Color.White)
    }
    else {
        Text("No parent", color = Color.White)
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
            CategoryListItem(categories[index])
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyApp() {

    CategoryHeader()
}

