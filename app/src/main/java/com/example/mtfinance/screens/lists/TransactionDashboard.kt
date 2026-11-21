/**
 * @file TransactionDashboard.kt
 * @scope Transaction dashboard views and details summary.
 * @purpose Displays all necessary information of a selected transaction, including associated categories, direct edit button, and category links.
 */
package com.example.mtfinance.screens.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.mtfinance.screens.Header
import com.example.mtfinance.screens.forms.FORM_SECTION_SPACING
import com.example.mtfinance.screens.forms.FORM_SPACING_UNIT
import com.example.mtfinance.screens.Routes
import com.example.mtfinance.src.messages.ButtonMessages
import com.example.mtfinance.src.messages.CategoryMessages
import com.example.mtfinance.src.messages.ScreenMessages
import com.example.mtfinance.src.viewmodels.TransactionViewModel

/**
 * Displays all necessary information of a selected transaction within the viewModel.
 * This includes categories the transaction is under, direct edit button, and category dashboard/edit links.
 */
@Composable
fun TransactionDashboardScreen(
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    navHostController: NavHostController
) {
    val selectedTransaction by transactionViewModel.selectedTransaction.observeAsState()
    val categoriesUnderSelectedTransaction by transactionViewModel.categoriesUnderSelectedTransaction.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FORM_SPACING_UNIT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FORM_SPACING_UNIT)
    ) {
        if (selectedTransaction != null) {
            Header(ScreenMessages.TRANSACTION_DASHBOARD_HEADER.getMessage())
            TransactionDetails(selectedTransaction!!)

            // Direct Edit Button
            Button(
                onClick = { navHostController.navigate(Routes.TRANSACTION_FORM.route + "/${selectedTransaction!!.transactionId}") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(ButtonMessages.EDIT.getMessage())
            }
        } else {
            Header(CategoryMessages.NO_TRANSACTIONS.getMessage())
        }

        if (categoriesUnderSelectedTransaction != null && categoriesUnderSelectedTransaction!!.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Categories Associated",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CategoryRawList(
                        categories = categoriesUnderSelectedTransaction!!,
                        actionOne = { categoryId: Long ->
                            navHostController.navigate(Routes.CATEGORY_FORM.route + "/$categoryId")
                        },
                        actionOneLabel = ButtonMessages.EDIT.getMessage(),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FORM_SECTION_SPACING - FORM_SPACING_UNIT))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(onClick = { transactionViewModel.resetSelectedTransaction() }) {
                Text(ButtonMessages.BACK.getMessage())
            }
        }
    }
}
