/**
 * @file CategoryDashboard.kt
 * @scope Category dashboard views and budgeting charts (pie chart, budget usage stacked bars).
 * @purpose Renders detailed analytics, proportional budget breakdown, sub-category distributions, search, and time-filtered transactions.
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import com.example.mtfinance.src.viewmodels.CategoryDashboardViewModel
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
 * Displays the polished Category Dashboard including proportional budget stacked bar, charts, details, subcategories, and search.
 */
@Composable
fun FullCategoryDashBoard(
    categoryDashboardViewModel: CategoryDashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedCategory by categoryDashboardViewModel.getSelectedCategory().observeAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    val currentOffset by categoryDashboardViewModel.getMonthOffset().observeAsState(0)

    val today = LocalDate.now()
    val activeDate = today.plusMonths(currentOffset.toLong())
    val monthLabel = if (activeDate.year == today.year) {
        activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
    } else {
        "${activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${activeDate.year}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Header(text = "Category Dashboard")

        if (selectedCategory != null) {
            // 1. Category Name Header Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = selectedCategory!!.category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Type: ${selectedCategory!!.category.type}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Month Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { categoryDashboardViewModel.navigatePreviousMonth() }) {
                    Text("< Prev")
                }
                Button(onClick = { showMonthPicker = true }) {
                    Text(text = monthLabel)
                }
                OutlinedButton(onClick = { categoryDashboardViewModel.navigateNextMonth() }) {
                    Text("Next >")
                }
            }

            if (showMonthPicker) {
                MonthPickerDialog(
                    initialMonthOffset = currentOffset,
                    onDismiss = { showMonthPicker = false },
                    onMonthSelected = { targetOffset ->
                        categoryDashboardViewModel.jumpToMonthOffset(targetOffset)
                    }
                )
            }

            // 2. Statistics Card (Proportional Budget Stacked Bar & Spending Trend)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Budget Statistics & Spending Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    CategoryStackedBar(categoryDashboardViewModel = categoryDashboardViewModel, settingsViewModel = settingsViewModel)
                    CategoryOverTimeChart(categoryDashboardViewModel = categoryDashboardViewModel)
                }
            }

            // 3. Category Details Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Description: ${selectedCategory!!.category.description?.ifEmpty { "None" } ?: "None"}")
                    Text("Monthly Budget: ${selectedCategory!!.category.monthlyBudget}")
                    val parent = selectedCategory!!.category.parent
                    Text("Parent / Root: ${parent?.name ?: "Root Category"}")
                }
            }

            // 4. Sub-Category Card (Dynamic size, count, search with clear button, show more, pie chart)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val subcategories = selectedCategory!!.category.getChildren(false).toList()
                    val subQuery by categoryDashboardViewModel.getSubcategorySearchQuery().observeAsState("")
                    val filteredSubs = subcategories.filter { it.name.contains(subQuery, ignoreCase = true) }

                    Text("Sub-Categories (${subcategories.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (subcategories.isEmpty()) {
                        Text("No sub-categories defined for this category.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Search(search = subQuery, setSearch = { categoryDashboardViewModel.setSubcategorySearchQuery(it) })

                        var showAllSubs by remember { mutableStateOf(false) }
                        val displayedSubs = if (showAllSubs) filteredSubs else filteredSubs.take(3)

                        if (displayedSubs.isEmpty()) {
                            Text("No matching sub-categories found.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                displayedSubs.forEach { sub ->
                                    CategoryListItem(categoryItem = sub, actionOne = { catId -> categoryDashboardViewModel.setSelectedCategory(catId) })
                                }
                            }
                            if (filteredSubs.size > 3) {
                                TextButton(onClick = { showAllSubs = !showAllSubs }) {
                                    Text(if (showAllSubs) "Show Less" else "Show More (${filteredSubs.size - 3} more)")
                                }
                            }
                        }

                        CategoryPieChart(categoryDashboardViewModel = categoryDashboardViewModel)
                    }
                }
            }

            // 5. Transaction Card (Search + Time Period Filter Switch)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val txQuery by categoryDashboardViewModel.getTransactionSearchQuery().observeAsState("")
                    val filterPeriod by categoryDashboardViewModel.getFilterTransactionsByPeriod().observeAsState(true)
                    val startDate by categoryDashboardViewModel.getStartDate().observeAsState()
                    val endDate by categoryDashboardViewModel.getEndDate().observeAsState()

                    Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Search(search = txQuery, setSearch = { categoryDashboardViewModel.setTransactionSearchQuery(it) })

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Filter to Active Pay Period", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = filterPeriod,
                            onCheckedChange = { categoryDashboardViewModel.setFilterTransactionsByPeriod(it) }
                        )
                    }

                    val rawTransactions = selectedCategory!!.transactions
                    val filteredTxs = rawTransactions.filter { tx ->
                        val matchesQuery = tx.getName().contains(txQuery, ignoreCase = true) || tx.getDescription().contains(txQuery, ignoreCase = true)
                        val matchesPeriod = !filterPeriod || (startDate != null && endDate != null && !tx.getDate().toLocalDate().isBefore(startDate) && !tx.getDate().toLocalDate().isAfter(endDate))
                        matchesQuery && matchesPeriod
                    }

                    if (filteredTxs.isEmpty()) {
                        Text("No matching transactions found.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            filteredTxs.forEach { tx ->
                                TransactionDetails(transaction = tx)
                            }
                        }
                    }
                }
            }

            Button(onClick = { categoryDashboardViewModel.resetSelectedCategory() }) {
                Text(text = "Back to Category List", color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Text(text = "No category selected", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
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
    categoryDashboardViewModel: CategoryDashboardViewModel = hiltViewModel()
) {
    val selectedCategory by categoryDashboardViewModel.getSelectedCategory().observeAsState()
    val totalExcludingSub by categoryDashboardViewModel.getTotalExcludingSub().observeAsState()
    val childrenTotals by categoryDashboardViewModel.getChildrenTotals().observeAsState()

    Text(
        "Sub-Category Spending Distribution",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
    )

    var values: List<Float>
    var labels = childrenTotals?.map { entry -> entry.key.category.name } ?: emptyList()

    val modelProducer = remember { PieChartModelProducer() }

    LaunchedEffect(totalExcludingSub, childrenTotals) {
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
            .height(220.dp)
    )
}

/**
 * Renders a spending summary over time for the active period.
 */
@Composable
fun CategoryOverTimeChart(
    categoryDashboardViewModel: CategoryDashboardViewModel = hiltViewModel()
) {
    val selectedCategory by categoryDashboardViewModel.getSelectedCategory().observeAsState()
    val startDate by categoryDashboardViewModel.getStartDate().observeAsState()
    val endDate by categoryDashboardViewModel.getEndDate().observeAsState()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Spending Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        val txCount = selectedCategory?.transactions?.count { tx ->
            startDate != null && endDate != null && !tx.getDate().toLocalDate().isBefore(startDate) && !tx.getDate().toLocalDate().isAfter(endDate)
        } ?: 0
        Text("Active Period Transactions: $txCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Renders a rehauled proportional budget usage stacked bar chart comparing cumulative spending against the monthly budget.
 */
@Composable
fun CategoryStackedBar(
    categoryDashboardViewModel: CategoryDashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val currencySymbol = systemPreferences?.currencySymbol ?: "$"
    val dateFormatter = DateFormat.fromStringOrDefault(systemPreferences?.dateFormat, DateFormat.DD_MM_YYYY).toFormatter()

    val selectedCategory by categoryDashboardViewModel.getSelectedCategory().observeAsState()
    val cumulativeTotal by categoryDashboardViewModel.getTotalIncludingSub().observeAsState(BigDecimal.ZERO)
    val remaining by categoryDashboardViewModel.getRemaining().observeAsState(BigDecimal.ZERO)
    val budget = selectedCategory?.category?.monthlyBudget ?: BigDecimal.ONE
    val startDate by categoryDashboardViewModel.getStartDate().observeAsState()
    val endDate by categoryDashboardViewModel.getEndDate().observeAsState()

    val isOverBudget = remaining < BigDecimal.ZERO

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budget Proportional Usage", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "From ${startDate?.format(dateFormatter)} to ${endDate?.format(dateFormatter)}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Right
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            val budgetFraction = 1f

            Box(
                modifier = Modifier
                    .fillMaxWidth(budgetFraction)
                    .height(30.dp)
                    .border(2.dp, if (isOverBudget) Color.Red else Color.Black, RoundedCornerShape(4.dp))
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
                    // Proportional representation when exceeding budget: Budget portion vs Excess portion
                    val budgetRatio = budget.divide(cumulativeTotal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                    val excessRatio = (1f - budgetRatio).coerceIn(0f, 1f)

                    if (budgetRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(budgetRatio)
                                .fillMaxHeight()
                                .background(Color(0xFFFFA500)) // Orange for consumed budget
                        )
                    }
                    if (excessRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(excessRatio)
                                .fillMaxHeight()
                                .background(Color.Red) // Red for budget excess
                        )
                    }
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
            "Monthly Budget: ${displayAmount(budget, currencySymbol)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
