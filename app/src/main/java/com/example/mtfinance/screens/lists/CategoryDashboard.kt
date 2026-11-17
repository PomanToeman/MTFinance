/**
 * @file CategoryDashboard.kt
 * @scope Category dashboard views and budgeting charts (pie chart, budget usage stacked bars).
 * @purpose Renders detailed analytics, budget breakdowns, and sub-category distributions for selected categories.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.MiniHeader
import com.example.mtfinance.screens.displayAmount
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.viewmodels.CategoryViewModel
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

/**
 * Displays the full category dashboard including budget bars, details, parent/sub-categories, and charts.
 */
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

            // Month Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { categoryViewModel.navigatePreviousMonth() }) {
                    Text("< Prev")
                }
                Button(onClick = { categoryViewModel.resetToCurrentMonth() }) {
                    Text("Current Month")
                }
                OutlinedButton(onClick = { categoryViewModel.navigateNextMonth() }) {
                    Text("Next >")
                }
            }

            CategoryStackedBar(categoryViewModel = categoryViewModel)
            CategoryDetails(selectedCategory!!)
            if (selectedCategory?.category?.parent != null) {
                MiniHeader("Parent")
                CategoryListItem(
                    selectedCategory?.category?.parent,
                    actionOne = { Long -> categoryViewModel.setSelectedCategory(Long) }
                )
            } else {
                MiniHeader("Root")
            }
            MiniHeader("Sub-Categories")
            CategoryRawList(
                selectedCategory!!.category.getChildren(false).toList(),
                { Long -> categoryViewModel.setSelectedCategory(Long) }
            )
            CategoryPieChart(categoryViewModel = categoryViewModel)
            MiniHeader("Transactions")
            TransactionListforCategory(selectedCategory!!)

            Button(onClick = { categoryViewModel.resetSelectedCategory() }) {
                Text(text = "Back", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Text(text = "No category selected", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Renders a pie chart distribution of sub-category spending for the selected category.
 */
@Composable
fun CategoryPieChart(
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    val cumulativeTotal by categoryViewModel.totalIncludingSub.observeAsState()
    val totalExcludingSub by categoryViewModel.totalExcludingSub.observeAsState()
    val childrenTotals by categoryViewModel.childrenTotals.observeAsState()

    Text(
        "Total distribution - Pie Chart",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    var values: List<Float>
    var labels = childrenTotals?.map { entry -> entry.key.category.name } ?: emptyList()

    val modelProducer = remember { PieChartModelProducer() }

    LaunchedEffect(cumulativeTotal, totalExcludingSub, childrenTotals) {
        values = childrenTotals?.map { entry -> entry.value.toFloat() } ?: emptyList()
        values = values.plus(totalExcludingSub?.toFloat() ?: 0f)
        labels = childrenTotals?.map { entry -> entry.key.category.name } ?: emptyList()
        labels = labels.plus(selectedCategory?.category?.name ?: "Main category")

        modelProducer.runTransaction {
            pieSeries {
                series(values)
            }
        }
    }

    PieChartHost(
        chart = rememberPieChart(
            sliceProvider = PieChart.SliceProvider.series(
                vicoTheme.pieChartColors.mapIndexed { index, color ->
                    PieChart.Slice(
                        fill = Fill(color),
                        label = PieChart.SliceLabel.Inside(
                            TextComponent(androidx.compose.ui.text.TextStyle(if (index == 2) Color.Black else Color.White), lineCount = 3)
                        )
                    )
                }
            ),
            valueFormatter = { _, value, index ->
                val label = labels.getOrNull(index) ?: ""
                "$label\n$$value"
            }
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    )
}

/**
 * Renders a budget usage stacked bar chart comparing cumulative spending against the monthly budget.
 */
@Composable
fun CategoryStackedBar(
    categoryViewModel: CategoryViewModel = hiltViewModel()
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
            Text(
                "From ${startDate?.format(DateFormat.DD_MM_YY.toFormatter())} to ${endDate?.format(DateFormat.DD_MM_YY.toFormatter())}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Right
            )
        }

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
                Text(displayAmount(cumulativeTotal), fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isOverBudget) "Over Budget" else "Remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) Color.Red else Color.Unspecified
                )
                Text(
                    displayAmount(remaining),
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) Color.Red else Color.Unspecified
                )
            }
        }

        Text(
            "Budget: ${displayAmount(budget)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Pure version of the stacked bar for Compose Previews.
 */
@Composable
fun CategoryStackedBarPure(
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
                Text("$${cumulativeTotal.setScale(2, RoundingMode.HALF_UP)}", fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isOverBudget) "Over Budget" else "Remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) Color.Red else Color.Unspecified
                )
                Text(
                    "$${remaining.abs().setScale(2, RoundingMode.HALF_UP)}",
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) Color.Red else Color.Unspecified
                )
            }
        }
    }
}
