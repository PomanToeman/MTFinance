/**
 * @file CategoryDashboard.kt
 * @scope Category dashboard views and budgeting charts (pie chart, budget usage stacked bars).
 * @purpose Renders detailed analytics, proportional budget breakdown, sub-category distributions, search, and time-filtered transactions with pagination.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.screens.displayAmount
import com.example.mtfinance.src.DateFormat
import com.example.mtfinance.src.trackingengine.DefaultSettings
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
 * Displays the polished Category Dashboard including proportional budget stacked bar, charts, details, subcategories, search, and pagination.
 */
@Composable
fun FullCategoryDashBoard(
    navHostController: NavHostController,
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
            // 1. Category Name Header Card with Edit Button
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        Button(
                            onClick = { navHostController.navigate(Routes.CATEGORY_FORM.route + "/${selectedCategory!!.category.categoryId}") }
                        ) {
                            Text("Edit Category")
                        }
                    }
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
                    CategoryOverTimeChart(categoryDashboardViewModel = categoryDashboardViewModel, settingsViewModel = settingsViewModel)
                }
            }

            // 3. Category Details Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
                    val currencySymbol = systemPreferences?.currencySymbol ?: "$"

                    Text("Category Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Description: ${selectedCategory!!.category.description?.ifEmpty { "None" } ?: "None"}")
                    Text("Minimum Budget: ${displayAmount(selectedCategory!!.category.determineMinimumBudget(), currencySymbol)}")
                    val parent = selectedCategory!!.category.parent
                    Text("Parent / Root: ${parent?.name ?: "Root Category"}")
                }
            }

            // 4. Sub-Category Card (Dynamic size, count, search with clear button, show more, pagination, pie chart)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val subcategories = selectedCategory!!.category.getChildren(false).toList()
                    val subQuery by categoryDashboardViewModel.getSubcategorySearchQuery().observeAsState("")
                    val filteredSubs = subcategories.filter { it.name.contains(subQuery, ignoreCase = true) }
                    val systemPrefs by settingsViewModel.systemPreferences.observeAsState()
                    val pageSize = systemPrefs?.pageSize ?: DefaultSettings.DEFAULT_PAGE_COUNT
                    var subPage by remember { mutableStateOf(1) }

                    val totalSubs = filteredSubs.size
                    val totalSubPages = (totalSubs + pageSize - 1) / pageSize
                    val paginatedSubs = filteredSubs.drop((subPage - 1) * pageSize).take(pageSize)

                    Text("Sub-Categories (${subcategories.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (subcategories.isEmpty()) {
                        Text("No sub-categories defined for this category.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Search(search = subQuery, setSearch = { categoryDashboardViewModel.setSubcategorySearchQuery(it) })

                        if (paginatedSubs.isEmpty()) {
                            Text("No matching sub-categories found.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                paginatedSubs.forEach { sub ->
                                    CategoryListItem(
                                        categoryItem = sub,
                                        actionOne = { catId -> categoryDashboardViewModel.setSelectedCategory(catId) },
                                        actionOneLabel = "Show More",
                                        actionTwo = { catId -> navHostController.navigate(Routes.CATEGORY_FORM.route + "/$catId") },
                                        actionTwoLabel = "Edit"
                                    )
                                }
                            }
                            PaginationBar(
                                currentPage = subPage,
                                totalPages = totalSubPages,
                                totalItems = totalSubs,
                                shownCount = paginatedSubs.size,
                                onPageChanged = { subPage = it }
                            )
                        }

                        CategoryPieChart(categoryDashboardViewModel = categoryDashboardViewModel)
                    }
                }
            }

            // 5. Transaction Card (Search + Time Period Filter Switch + Transaction List Helper)
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
                        TransactionList(
                            transactions = filteredTxs,
                            actionOne = null,
                            actionTwo = null
                        )
                    }
                }
            }

            TextButton(
                onClick = { categoryDashboardViewModel.resetSelectedCategory() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(text = "Back to Category List", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
 * Renders a custom built-in Canvas spending trend chart showing daily cumulative spending growth and a horizontal budget reference line.
 */
@Composable
fun CategoryOverTimeChart(
    categoryDashboardViewModel: CategoryDashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val systemPreferences by settingsViewModel.systemPreferences.observeAsState()
    val currencySymbol = systemPreferences?.currencySymbol ?: "$"

    val selectedCategory by categoryDashboardViewModel.getSelectedCategory().observeAsState()
    val startDate by categoryDashboardViewModel.getStartDate().observeAsState()
    val endDate by categoryDashboardViewModel.getEndDate().observeAsState()
    val budget = selectedCategory?.category?.monthlyBudget ?: BigDecimal.ZERO

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Daily Cumulative Spending Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        if (startDate != null && endDate != null && selectedCategory != null) {
            val transactions = selectedCategory!!.transactions.filter { tx ->
                val txDate = tx.getDate().toLocalDate()
                !txDate.isBefore(startDate) && !txDate.isAfter(endDate)
            }

            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt().coerceAtLeast(0)
            val dailyTotals = mutableMapOf<LocalDate, BigDecimal>()
            for (i in 0..daysBetween) {
                dailyTotals[startDate!!.plusDays(i.toLong())] = BigDecimal.ZERO
            }
            for (tx in transactions) {
                val txDate = tx.getDate().toLocalDate()
                if (dailyTotals.containsKey(txDate)) {
                    dailyTotals[txDate] = dailyTotals[txDate]!!.add(tx.getAmount())
                }
            }

            val cumulativePoints = mutableListOf<Pair<LocalDate, BigDecimal>>()
            var runningTotal = BigDecimal.ZERO
            for ((date, amt) in dailyTotals) {
                runningTotal = runningTotal.add(amt)
                cumulativePoints.add(Pair(date, runningTotal))
            }

            val maxSpend = cumulativePoints.maxOfOrNull { it.second } ?: BigDecimal.ONE
            val maxY = maxOf(maxSpend.toFloat(), budget.toFloat(), 1f) * 1.1f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp)
            ) {
                val width = size.width
                val height = size.height
                val leftPadding = 64f
                val bottomPadding = 32f

                val chartWidth = width - leftPadding - 16f
                val chartHeight = height - bottomPadding - 16f

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 26f
                    isAntiAlias = true
                }

                // Draw X and Y axis lines
                drawLine(
                    color = Color.Gray,
                    start = Offset(leftPadding, 16f),
                    end = Offset(leftPadding, height - bottomPadding),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(leftPadding, height - bottomPadding),
                    end = Offset(width - 16f, height - bottomPadding),
                    strokeWidth = 2f
                )

                // Draw Y axis labels
                drawContext.canvas.nativeCanvas.drawText(displayAmount(BigDecimal.ZERO, currencySymbol), 4f, height - bottomPadding + 8f, textPaint)
                drawContext.canvas.nativeCanvas.drawText(displayAmount(BigDecimal.valueOf((maxY / 2f).toDouble()), currencySymbol), 4f, height - bottomPadding - chartHeight / 2f, textPaint)
                drawContext.canvas.nativeCanvas.drawText(displayAmount(BigDecimal.valueOf(maxY.toDouble()), currencySymbol), 4f, 24f, textPaint)

                // Draw budget reference line (horizontal)
                val budgetY = height - bottomPadding - (budget.toFloat() / maxY).coerceIn(0f, 1f) * chartHeight
                drawLine(
                    color = errorColor,
                    start = Offset(leftPadding, budgetY),
                    end = Offset(width - 16f, budgetY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                if (cumulativePoints.isNotEmpty()) {
                    val path = Path()
                    val pointCount = cumulativePoints.size
                    for (i in cumulativePoints.indices) {
                        val x = leftPadding + (if (pointCount > 1) i.toFloat() / (pointCount - 1) * chartWidth else 0f)
                        val y = height - bottomPadding - (cumulativePoints[i].second.toFloat() / maxY).coerceIn(0f, 1f) * chartHeight
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                        drawCircle(color = primaryColor, radius = 4f, center = Offset(x, y))

                        // Draw X-axis day number labels
                        if (i == 0 || i == pointCount - 1 || i % Math.max(1, pointCount / 5) == 0) {
                            val dayNum = cumulativePoints[i].first.dayOfMonth.toString()
                            drawContext.canvas.nativeCanvas.drawText(dayNum, x - 10f, height - bottomPadding + 24f, textPaint)
                        }
                    }
                    drawPath(path = path, color = primaryColor, style = Stroke(width = 4f))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Start: $startDate", style = MaterialTheme.typography.labelSmall)
                Text("Budget Line: ${displayAmount(budget, currencySymbol)}", style = MaterialTheme.typography.labelSmall, color = errorColor, fontWeight = FontWeight.Bold)
                Text("End: $endDate", style = MaterialTheme.typography.labelSmall)
            }
        }
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
