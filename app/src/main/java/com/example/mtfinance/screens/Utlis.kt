package com.example.mtfinance.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FabRightBottomCorner( onClick: () -> Unit, content: @Composable () -> Unit, iconImage: ImageVector = Icons.Default.Add) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onClick, containerColor = Color.Black ) {
                Icon(imageVector = iconImage, contentDescription = "Add", tint = Color.White)
            }


        },
        content = {
            content()
        }
    )
}


