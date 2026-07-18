package com.example.mtfinance.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
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
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryHeader()
        if (categories != null) {
            CategoryList(categories!!)
        }

    }


}

@Composable
fun CategoryListItem(categoryItem: CategoryWithTransactions?) {
    if (categoryItem != null) {
        Text(text = categoryItem.details,
        color = Color.Yellow)
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

