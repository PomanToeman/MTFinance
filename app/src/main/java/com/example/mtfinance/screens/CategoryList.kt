package com.example.mtfinance.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import com.example.mtfinance.src.viewmodels.CategoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.Category

import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import java.math.RoundingMode


@Composable
fun CategoryListScreen(
    NavHostController: NavHostController,
    categoryViewModel: CategoryViewModel = hiltViewModel()



) {
    val categories by categoryViewModel.filteredCategories.observeAsState()
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()


   DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (selectedCategory == null) {


            Header()
            CategorySearch()
            if (categories != null && categories!!.isNotEmpty()) {
                CategoryList(categories!!, actionOne = { Long -> categoryViewModel.setSelectedCategory(Long)})
            } else {
                Text("No categories Found", color = MaterialTheme.colorScheme.primary)
            }
        }
        else {
            CategoryDashBoard()
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
) {
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Header(text = "Category Dashboard")
        if (selectedCategory != null) {
            Text(
                text = MessageCli.CATEGORY_SELECTED.getMessage(selectedCategory!!.category.name),
                color = MaterialTheme.colorScheme.primary
            )
            CategoryDetails(selectedCategory!!)
            if (selectedCategory?.category?.parent != null) {
                CategoryListItem(
                    selectedCategory?.category?.parent,
                    actionOne = { Long -> categoryViewModel.setSelectedCategory(Long) })
            } else {
                Text("No parent", color = MaterialTheme.colorScheme.primary)
            }
            SubCategoryList(
                selectedCategory!!.category.getChildren(false).toList(),
                action = { Long -> categoryViewModel.setSelectedCategory(Long) })
            TransactionListforCategory(selectedCategory!!)
            Button(onClick = { categoryViewModel.resetSelectedCategory() }) {
                Text(text = "Back", color = Color.Yellow)
            }
        } else {
            Text(text = "No category selected", color = MaterialTheme.colorScheme.primary)
        }
    }
}



@Composable
fun CategoryListItem(categoryItem: Category?, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String? = null, actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String? = null, backgroundColor: Color = Color.Gray){
    val expanded = remember { mutableStateOf(false) }
    if (categoryItem != null) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 3. Toggle state on click
                .clickable { expanded.value = !expanded.value }.background(color = backgroundColor, shape = RectangleShape)


        ) {



                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    content = {
                        Text(categoryItem.name, color = MaterialTheme.colorScheme.primary)

                        if (expanded.value) {

                            Text(MessageCli.CATEGORY_DESCRIPTION.getMessage(categoryItem.description), color = MaterialTheme.colorScheme.primary)
                            Row() {
                                if (actionOne != null) {
                                    TextButton(onClick = { actionOne(categoryItem.categoryId) }) {
                                        Text(
                                            actionOneLabel ?: "Show more",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (actionTwo != null) {
                                    TextButton(onClick = { actionTwo(categoryItem.categoryId) }) {
                                        Text(
                                            actionTwoLabel ?: "Show more",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            }



                    }
                )

        }


    }
}

@Composable
fun TransactionListforCategory(categoryItem: CategoryWithTransactions) {
    Text("Transactions")
    if (categoryItem.transactions != null && categoryItem.transactions.isNotEmpty()) {

        TransactionList(categoryItem.transactions)
    }
    else {
        Text("No transactions", color = MaterialTheme.colorScheme.primary)
    }

}

@Composable
fun CategoryDetails(categoryItem: CategoryWithTransactions) {
    Text(MessageCli.CATEGORY_DESCRIPTION.getMessage(categoryItem.category.description), color = MaterialTheme.colorScheme.primary)
    Text(MessageCli.CATEGORY_MONTHLY_BUDGET.getMessage(categoryItem.category.monthlyBudget.setScale(2, RoundingMode.HALF_UP).toString()), color = MaterialTheme.colorScheme.primary)
    Text(MessageCli.TYPE_DISPLAY.getMessage(categoryItem.category.type.toString().lowercase()), color = MaterialTheme.colorScheme.primary)



}

@Composable
fun Header(text: String = "Category List") {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }





}

@Composable
fun CategoryList(categories: Collection<CategoryWithTransactions>, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String? = null, actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String? = null, backgroundColor: Color = Color.Gray ) {
    LazyColumn(modifier = Modifier.fillMaxSize().height(500.dp).padding(16.dp).border(
        width = 2.dp,
        color = Color.Black,
        shape = RectangleShape
    )) {

        items(categories.size) { index ->
            CategoryListItem(categories.elementAt(index).category, actionOne, actionOneLabel, actionTwo, actionTwoLabel, backgroundColor)
        }
    }
}

@Composable
fun SubCategoryList(categories: List<Category>, action: (Long) -> Unit = {}) {
    Column {
        categories.forEach { category ->
            CategoryListItem(category, action)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyApp() {

    Header()
}

