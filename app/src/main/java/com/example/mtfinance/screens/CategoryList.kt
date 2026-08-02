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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.viewmodels.CategoryViewModel

import androidx.navigation.NavHostController
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.Category

import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import java.math.BigDecimal
import java.math.RoundingMode


@Composable
fun CategoryListScreen(
    NavHostController: NavHostController,
    categoryViewModel: CategoryViewModel = hiltViewModel()



) {
    val categories by categoryViewModel.filteredCategories.observeAsState()
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    val searchQuery by categoryViewModel.searchQuery.observeAsState()
    val typeFilter by categoryViewModel.typeFilter.observeAsState()
    val updateTrigger by categoryViewModel.updateTrigger.observeAsState()


    FabRightBottomCorner(actionOne = { NavHostController.navigate(Routes.CATEGORY_FORM.route) }, content = {
        DefaultColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (selectedCategory == null) {


                Header("Category List")
                Search(searchQuery = searchQuery, onQueryChange = {categoryViewModel.setSearchQuery(it)})
                ChooseTypeForm(typeFilter, { type -> categoryViewModel.setTypeFilter(type) }, includeNone = true)
                if (categories != null && categories!!.isNotEmpty()) {
                    CategoryList(categories!!, actionOne = { Long -> categoryViewModel.setSelectedCategory(Long)}, actionOneLabel = "Show more", backgroundColor = Color.LightGray, actionTwo = { Long -> NavHostController.navigate(Routes.CATEGORY_FORM.route + "/" + Long)}, actionTwoLabel = "Edit", updateTrigger = updateTrigger)

                } else {
                    Text("No categories Found", color = MaterialTheme.colorScheme.primary)
                }
            }
            else {
                CategoryDashBoard()
            }

        }
    })





}

/**
 * Sets a search query for a given setter
 *
 */
@Composable
fun Search(searchQuery: String?, onQueryChange: ((String) -> Unit)?) {

    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Search: ", color = MaterialTheme.colorScheme.primary)
        TextField(searchQuery ?: "", onValueChange = {
            onQueryChange?.invoke(it)
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
            CategoryListBasic(
                selectedCategory!!.category.getChildren(false).toList(),
                action = { Long -> categoryViewModel.setSelectedCategory(Long) })
            TransactionListforCategory(selectedCategory!!)


            CategoryPieChart()
            CategoryStackedBar()

            
            Button(onClick = { categoryViewModel.resetSelectedCategory() }) {
                Text(text = "Back", color = Color.Yellow)
            }
        } else {
            Text(text = "No category selected", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Pie chart for the selected category (if any)
 */
@Composable
fun CategoryPieChart(
    categoryViewModel: CategoryViewModel = hiltViewModel(),

) {

    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    val cumulativeTotal by categoryViewModel.totalIncludingSub.observeAsState()
    val totalExcludingSub by categoryViewModel.totalExcludingSub.observeAsState()
    val childrenTotals by categoryViewModel.childrenTotals.observeAsState()

    Text("Total distribution - Pie Chart", color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

    var values: List<Float>

    var labels = childrenTotals?.map { entry -> entry.key.category.name } ?: emptyList()




    val modelProducer = remember { PieChartModelProducer() }

    LaunchedEffect(cumulativeTotal, totalExcludingSub, childrenTotals) {
        values = childrenTotals?.map { entry -> entry.value.toFloat() } ?: emptyList()
        values = values.plus(totalExcludingSub?.toFloat() ?: 0f)
        labels = childrenTotals?.map { entry -> entry.key.category.name } ?: emptyList()
        labels = labels.plus(selectedCategory?.category?.name ?: "Main category")

        println(values)
        modelProducer.runTransaction {
            pieSeries {
                series(values)


            }
        }

    }




    PieChartHost(
        chart =
            rememberPieChart(
                sliceProvider =
                    PieChart.SliceProvider.series(
                        vicoTheme.pieChartColors.mapIndexed { index, color ->
                            PieChart.Slice(
                                fill = Fill(color),
                                label =
                                    PieChart.SliceLabel.Inside(
                                        TextComponent(TextStyle(if (index == 2) Color.Black else Color.White), lineCount = 3)
                                    ))}),
                valueFormatter = { _, value, index,  ->
                    val label = labels.getOrNull(index) ?: ""
                    "$label\n$${value}"
                    }),

        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),


    )

}

@Composable
fun CategoryStackedBar(
    categoryViewModel: CategoryViewModel = hiltViewModel(),

) {
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    val cumulativeTotal by categoryViewModel.totalIncludingSub.observeAsState()
    val modelProducer = remember { CartesianChartModelProducer() }
    val remaining by categoryViewModel.remaining.observeAsState()

    Text("Total distribution - Bar Chart", color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

    LaunchedEffect(cumulativeTotal, remaining) {
        modelProducer.runTransaction {
            columnModel {
                columnModel {
                    series(listOf(cumulativeTotal?.toFloat() ?: 0f, remaining?.toFloat() ?: 0f))
                }
            }
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
                        .fillMaxWidth().padding(3.dp),
                    content = {
                        Row() {

                            Text(categoryItem.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Left, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)

                            Text("$" + categoryItem.monthlyBudget.setScale(2, RoundingMode.HALF_UP).toString(), textAlign = TextAlign.Right)


                        }
                        Text(categoryItem.type.toString(), color = Color.Black)


                        if (expanded.value) {

                            Text(MessageCli.CATEGORY_DESCRIPTION.getMessage(categoryItem.description), color = Color.Black, minLines = 1, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
fun CategoryListItem(categoryWithTransactions: CategoryWithTransactions, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String? = null, actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String? = null, backgroundColor: Color = Color.Gray) {
    CategoryListItem(categoryWithTransactions.category, actionOne, actionOneLabel, actionTwo, actionTwoLabel, backgroundColor)
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
fun CategoryList(categories: Collection<CategoryWithTransactions>, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String? = null, actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String? = null, backgroundColor: Color = Color.Gray, updateTrigger: Long? = 0 ) {
    LazyColumn(modifier = Modifier.fillMaxSize().height(500.dp).padding(16.dp).border(
        width = 2.dp,
        color = Color.Black,
        shape = RectangleShape
    )) {

        items(categories.size,  key = {  index -> "${categories.elementAt(index).category.categoryId}_${updateTrigger}" }) { index ->
            CategoryListItem(categories.elementAt(index), actionOne, actionOneLabel, actionTwo, actionTwoLabel, backgroundColor)
        }
    }
}

@Composable
@JvmName("CategoryListFromCategory")
fun CategoryList(categories: Collection<Category>, actionOne: ((Long) -> Unit)? = null, actionOneLabel: String? = null, actionTwo: ((Long) -> Unit)? = null, actionTwoLabel: String? = null, backgroundColor: Color = Color.Gray, updateTrigger: Long? = 0 ) {
    LazyColumn(modifier = Modifier.fillMaxSize().height(500.dp).padding(16.dp).border(
        width = 2.dp,
        color = Color.Black,
        shape = RectangleShape
    )) {

        items(categories.size , key = { index -> "${categories.elementAt(index).categoryId}_${updateTrigger}" }) { index ->
            CategoryListItem(categories.elementAt(index), actionOne, actionOneLabel, actionTwo, actionTwoLabel, backgroundColor)
        }
    }
}

@Composable
fun CategoryListBasic(categories: List<Category>, action: ((Long) -> Unit)? = null) {
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

