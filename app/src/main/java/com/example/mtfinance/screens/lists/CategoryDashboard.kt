/**
 * @file CategoryDashboard.kt
 * @scope Category dashboard views and budgeting charts (pie chart, budget usage stacked bars).
 * @purpose Renders detailed analytics, budget breakdowns, sub-category distributions, and month picker navigation.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.mtfinance.src.viewmodels.SettingsViewModel
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
import java.time.LocalDate

/**
 * Displays the full category dashboard including budget bars, details, parent/sub-categories, and charts.
 */
@Composable
fun FullCategoryDashBoard(
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val selectedCategory by categoryViewModel.selectedCategory.observeAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    val currentOffset by categoryViewModel.monthOffset.observeAsState(0)

    val today = LocalDate.now()
    val activeDate = today.plusMonths(currentOffset.toLong())
    val monthLabel = if (activeDate.year == today.year) {
        activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
    } else {
        "${activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${activeDate.year}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Header(text = "Category Dashboard")

        if (selectedCategory != null) {
            MiniHeader(text = selectedCategory!!.category.name + " - " + selectedCategory!!.category.type.toString())

            // Month Navigation Controls with Month Dialogue Picker Trigger
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
                Button(onClick = { showMonthPicker = true }) {
                    Text(text = monthLabel)
                }
                OutlinedButton(onClick = { categoryViewModel.navigateNextMonth() }) {
                    Text("Next >")
                }
            }

            if (showMonthPicker) {
                MonthPickerDialog(
                    initialMonthOffset = currentOffset,
                    onDismiss = { showMonthPicker = false },
                    onMonthSelected = { targetOffset ->
                        categoryViewModel.jumpToMonthOffset(targetOffset)
                    }
                )
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
                Text(text = "Back", color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Text(text = "No category selected", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Interactive dialog allowing users to pick a specific historical or future month directly.
 */
@Composable
fun MonthPickerDialog(
    initialMonthOffset: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    val today = LocalDate.now()
    var selectedYear by remember { mutableStateOf(today.year + (initialMonthOffset / 12)) }
    var selectedMonth by remember { mutableStateOf(today.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to Specific Month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Target Year and Month:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Year: ", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedYear-- }) { Text("<") }
                        Text("$selectedYear", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { selectedYear++ }) { Text(">") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Month: ", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedMonth > 1) selectedMonth-- else { selectedMonth = 12; selectedYear-- } }) { Text("<") }
                        Text(
                            java.time.Month.of(selectedMonth).name,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { if (selectedMonth < 12) selectedMonth++ else { selectedMonth = 1; selectedYear++ } }) { Text(">") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val targetYearMonth = java.time.YearMonth.of(selectedYear, selectedMonth)
                val currentYearMonth = java.time.YearMonth.of(today.year, today.monthValue)
                val computedOffset = (targetYearMonth.year - currentYearMonth.year) * 12 + (targetYearMonth.monthValue - currentYearMonth.monthValue)
                onMonthSelected(computedOffset)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val currencySymbol = systemPreferences?.currencySymbol ?: "$"
    val dateFormatter = DateFormat.fromStringOrDefault(systemPreferences?.dateFormat, DateFormat.DD_MM_YYYY).toFormatter()

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
                "From ${startDate?.format(dateFormatter)} to ${endDate?.format(dateFormatter)}",
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
                    .fillMaxWidth(budgetFraction)
                    .height(30.dp)
            ) {
                if (!isOverBudget) {
                    val spentRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    val remainingRatio = (1f - spentRatio).coerceIn(0f, 1f)

                    if (spentRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(spentRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (remainingRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(remainingRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        )
                    }
                } else {
                    val overflowRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat()
                    } else 1f

                    Box(
                        modifier = Modifier
                            .weight(overflowRatio.coerceAtLeast(1f))
                            .fillMaxHeight()
                            .background(Color.Red)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total Spent", style = MaterialTheme.typography.labelSmall)
                Text(displayAmount(cumulativeTotal, currencySymbol), fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isOverBudget) "Over Budget" else "Remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) Color.Red else Color.Unspecified
                )
                Text(
                    displayAmount(remaining, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) Color.Red else Color.Unspecified
                )
            }
        }

        Text(
            "Budget: ${displayAmount(budget, currencySymbol)}",
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
                    .fillMaxWidth(budgetFraction)
                    .height(30.dp)
            ) {
                if (!isOverBudget) {
                    val spentRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    val remainingRatio = (1f - spentRatio).coerceIn(0f, 1f)

                    if (spentRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(spentRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (remainingRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(remainingRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        )
                    }
                } else {
                    val overflowRatio = if (budget > BigDecimal.ZERO) {
                        cumulativeTotal.divide(budget, 4, RoundingMode.HALF_UP).toFloat()
                    } else 1f

                    Box(
                        modifier = Modifier
                            .weight(overflowRatio.coerceAtLeast(1f))
                            .fillMaxHeight()
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
