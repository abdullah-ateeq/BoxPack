package com.example.boxpack.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ControlPointDuplicate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxpack.model.SavedCalculation
import com.example.boxpack.ui.BoxPackViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: BoxPackViewModel,
    onNavigateBack: () -> Unit,
    onOpenCalculation: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var itemToDelete by remember { mutableStateOf<SavedCalculation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Calculations (${state.history.size}/10)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No saved calculations yet.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the save icon on the Home screen to save up to 10 calculations offline.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.history, key = { it.id }) { calc ->
                    HistoryItemCard(
                        calc = calc,
                        onOpen = {
                            viewModel.loadCalculation(calc)
                            onOpenCalculation()
                        },
                        onDuplicate = {
                            viewModel.duplicateCalculation(calc)
                        },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val unit = calc.unit
                            val text = """
                                BoxPack: ${calc.title}
                                Container: ${unit.formatValue(calc.containerWidthMm)} × ${unit.formatValue(calc.containerDepthMm)} × ${unit.formatValue(calc.containerHeightMm)} ${unit.symbol}
                                Box: ${unit.formatValue(calc.boxWidthMm)} × ${unit.formatValue(calc.boxDepthMm)} × ${unit.formatValue(calc.boxHeightMm)} ${unit.symbol}
                                Capacity: ${calc.capacity} boxes (${calc.across} × ${calc.deep} × ${calc.layers})
                                Utilization: ${String.format(Locale.US, "%.1f%%", calc.utilizationPercent)}
                            """.trimIndent()
                            val clip = ClipData.newPlainText("BoxPack Calculation", text)
                            clipboard.setPrimaryClip(clip)
                        },
                        onDelete = {
                            if (state.settings.confirmDelete) {
                                itemToDelete = calc
                            } else {
                                viewModel.deleteCalculation(calc.id)
                            }
                        }
                    )
                }
            }
        }
    }

    itemToDelete?.let { calc ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Calculation") },
            text = { Text("Are you sure you want to delete \"${calc.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCalculation(calc.id)
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    calc: SavedCalculation,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val unit = calc.unit
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(calc.timestamp) { dateFormat.format(Date(calc.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = calc.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${calc.capacity} BOXES",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Container: ${unit.formatValue(calc.containerWidthMm)} × ${unit.formatValue(calc.containerDepthMm)} × ${unit.formatValue(calc.containerHeightMm)} ${unit.symbol}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Box: ${unit.formatValue(calc.boxWidthMm)} × ${unit.formatValue(calc.boxDepthMm)} × ${unit.formatValue(calc.boxHeightMm)} ${unit.symbol}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Arrangement: ${calc.across} × ${calc.deep} × ${calc.layers} • ${String.format(Locale.US, "%.1f%%", calc.utilizationPercent)} util",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formattedDate,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpen) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Open calculation", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDuplicate) {
                    Icon(imageVector = Icons.Default.ControlPointDuplicate, contentDescription = "Duplicate calculation")
                }
                IconButton(onClick = onCopy) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy details")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
