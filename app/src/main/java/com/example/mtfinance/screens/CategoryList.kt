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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.viewmodels.CategoryViewModel

import androidx.navigation.NavHostController
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.MessageCli
import com.example.mtfinance.src.trackingengine.Category

import com.example.mtfinance.src.trackingengine.CategoryWithTransactions
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
        DefaultColumn(modifier = Modifier.padding(4.dp).verticalScroll(rememberScrollState())) {
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
                FullCategoryDashBoard()
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
fun FullCategoryDashBoard(
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
            MiniHeader(text = selectedCategory!!.category.name + " - " + selectedCategory!!.category.type.toString())
            CategoryStackedBar( categoryViewModel = categoryViewModel)
            CategoryDetails(selectedCategory!!)
            if (selectedCategory?.category?.parent != null) {
                MiniHeader("Parent")
                CategoryListItem(
                    selectedCategory?.category?.parent,
                    actionOne = { Long -> categoryViewModel.setSelectedCategory(Long) })
            } else {
                MiniHeader("Root")
            }
            MiniHeader("Sub-Categories")
            CategoryList(
                selectedCategory!!.category.getChildren(false).toList(),
                { Long -> categoryViewModel.setSelectedCategory(Long) })
            CategoryPieChart( categoryViewModel = categoryViewModel)
            MiniHeader("Transactions")
            TransactionListforCategory(selectedCategory!!)





            
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
    val cumulativeTotal by categoryViewModel.totalIncludingSub.observeAsState(BigDecimal.ZERO)
    val remaining by categoryViewModel.remaining.observeAsState(BigDecimal.ZERO)
    val budget = selectedCategory?.category?.monthlyBudget ?: BigDecimal.ONE
    val startDate by categoryViewModel.startDate.observeAsState()
    val endDate by categoryViewModel.endDate.observeAsState()


    val isOverBudget = remaining < BigDecimal.ZERO

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budget Usage", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Text("From ${startDate?.format(DateFormat.DD_MM_YY.toFormatter())} to ${endDate?.format(DateFormat.DD_MM_YY.toFormatter())}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Right)
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            // The "Budget Container" outline. We'll use 80% of width as the 100% budget mark.
            val budgetFraction = 0.8f

            // Outline Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(budgetFraction)
                    .height(30.dp)
                    .border(2.dp, if (isOverBudget) Color.Red else Color.Black)
            )

            // Spent/Remaining Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                if (!isOverBudget) {
                    // Normal state: [Spent | Remaining]
                    val spentRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat()
                    } else 0f
                    
                    val remainingRatio = 1f - spentRatio

                    // Spent portion
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(spentRatio * budgetFraction)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    // Remaining portion (inside outline)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(remainingRatio * budgetFraction)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                } else {
                    // Overflow state: [Spent (Red)]
                    // The bar fills the outline and overflows into the remaining 20% width.
                    val overflowRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat()
                    } else 1f
                    
                    // We cap the visual overflow at the screen edge (1.0f)
                    val visualWidth = (overflowRatio * budgetFraction).coerceAtMost(1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(visualWidth)
                            .height(30.dp)
                            .background(Color.Red)
                    )
                }
            }
        }

        // Labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total Spent", style = MaterialTheme.typography.labelSmall)
                Text(displayAmount(cumulativeTotal), fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (isOverBudget) "Over Budget" else "Remaining", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isOverBudget) Color.Red else Color.Unspecified)
                Text(
                    displayAmount(remaining),
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) Color.Red else Color.Unspecified)
            }
        }
        
        Text("Budget: ${displayAmount(budget)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(), 
            textAlign = TextAlign.Center)
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

    if (categoryItem.transactions != null && categoryItem.transactions.isNotEmpty()) {

        TransactionList(categoryItem.transactions)
    }
    else {
        Text("No transactions", color = MaterialTheme.colorScheme.primary)
    }

}

@Composable
fun CategoryDetails(categoryItem: CategoryWithTransactions) {
    Text("Category Details", color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Left)
    Column( modifier = Modifier.fillMaxWidth().padding(16.dp).border(width = 2.dp, color = Color.Black, shape = RectangleShape), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.Start) {
        Text(MessageCli.CATEGORY_DESCRIPTION.getMessage(categoryItem.category.description), color = MaterialTheme.colorScheme.primary, minLines = 1, maxLines = 10, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(4.dp))
    }




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

@Preview(showBackground = true)
@Composable
fun PreviewCategoryStackedBar() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Normal State")
            // Visualizing the structure without a real ViewModel
            CategoryStackedBarPure(
                cumulativeTotal = BigDecimal("45.00"),
                remaining = BigDecimal("55.00"),
                budget = BigDecimal("100.00")
            )
            
            Text("Over Budget State")
            CategoryStackedBarPure(
                cumulativeTotal = BigDecimal("125.00"),
                remaining = BigDecimal("-25.00"),
                budget = BigDecimal("100.00")
            )
        }
    }
}

/**
 * Pure version for Previews
 */
@Composable
private fun CategoryStackedBarPure(
    cumulativeTotal: BigDecimal,
    remaining: BigDecimal,
    budget: BigDecimal
) {
    val isOverBudget = remaining < BigDecimal.ZERO

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Budget Usage", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            val budgetFraction = 0.8f

            Box(
                modifier = Modifier
                    .fillMaxWidth(budgetFraction)
                    .height(30.dp)
                    .border(2.dp, if (isOverBudget) Color.Red else Color.Black)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                if (!isOverBudget) {
                    val spentRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat()
                    } else 0f
                    
                    val remainingRatio = 1f - spentRatio

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(spentRatio * budgetFraction)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(remainingRatio * budgetFraction)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                } else {
                    val overflowRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat()
                    } else 1f
                    
                    val visualWidth = (overflowRatio * budgetFraction).coerceAtMost(1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(visualWidth)
                            .height(30.dp)
                            .background(Color.Red)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total Spent", style = MaterialTheme.typography.labelSmall)
                Text("$${cumulativeTotal.setScale(2, RoundingMode.HALF_UP)}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (isOverBudget) "Over Budget" else "Remaining", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isOverBudget) Color.Red else Color.Unspecified)
                Text("$${remaining.abs().setScale(2, RoundingMode.HALF_UP)}", 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (isOverBudget) Color.Red else Color.Unspecified)
            }
        }
    }
}

