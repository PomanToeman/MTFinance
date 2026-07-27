package com.example.mtfinance.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

/**
 * A standard column layout for the application. Meant to used as a basis for other layouts.
 */
@Composable
fun DefaultColumn(modifier: Modifier = Modifier, horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),


        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        content()
    }
}

/**
 * Puts a floating action button on the right bottom corner of the screen which enacts given action. Can have up to two actions.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FabRightBottomCorner(actionOne: () -> Unit, actionTwo: (() -> Unit)? = null, content: @Composable () -> Unit,  iconImageOne: ImageVector = Icons.Default.Add, iconImageTwo: ImageVector = Icons.Default.Add ) {
    Scaffold(

        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(onClick = actionOne, containerColor = Color.Black, contentColor = Color.White) {
                    Icon(imageVector = iconImageOne, contentDescription = "Add", tint = Color.White)
                }
                if (actionTwo != null) {
                    FloatingActionButton(onClick = actionTwo, containerColor = Color.Black, contentColor = Color.White) {
                        Icon(imageVector = iconImageTwo, contentDescription = "Add", tint = Color.White)
                    }
                }


            }

        },
        content = {
            content()
        }
    )
}

/**
 * Displays an amount with a dollar sign and two decimal places for the user.
 */
fun displayAmount(amount: BigDecimal): String {
    return "$" + amount.setScale(2).toString()
}

/**
 * Confirms to delete a particular item.
 */
@Composable
fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit, message: String = "Are you sure you want to delete this item?", content: @Composable (() -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Delete Confirmation") },
        text = { Column( modifier = Modifier.padding(6.dp)) {
            Text(message)
            if (content != null) {
                content()
            }
        } },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { onCancel() }) {
                Text("Cancel")
            }
        }
    )

}


