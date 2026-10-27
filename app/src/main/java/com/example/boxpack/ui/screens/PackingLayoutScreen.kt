package com.example.boxpack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.ui.BoxPackViewModel
import com.example.boxpack.ui.components.PackingViewDiagram
import com.example.boxpack.ui.components.PackingViewType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingLayoutScreen(
    viewModel: BoxPackViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentLayer by remember { mutableIntStateOf(1) }

    val result = state.activeResult ?: state.bestResult
    val unit = state.selectedUnit
    val decimals = state.settings.decimalPlaces

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Packing Layout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (result == null || result.capacity <= 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No packing arrangement to display.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ensure container and box dimensions are valid on the Home screen.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            val cW = unit.toMm(state.containerWidthInput.toDoubleOrNull() ?: 1200.0)
            val cD = unit.toMm(state.containerDepthInput.toDoubleOrNull() ?: 800.0)
            val cH = unit.toMm(state.containerHeightInput.toDoubleOrNull() ?: 600.0)

            val ori = result.orientation
            val effectiveBoxW = ori.widthMm + LengthUnit.MILLIMETERS.toMm(state.boxWidthGapInput.toDoubleOrNull() ?: 0.0)
            val effectiveBoxD = ori.depthMm + LengthUnit.MILLIMETERS.toMm(state.boxDepthGapInput.toDoubleOrNull() ?: 0.0)
            val effectiveBoxH = ori.heightMm + LengthUnit.MILLIMETERS.toMm(state.boxHeightGapInput.toDoubleOrNull() ?: 0.0)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top Tab Bar for Views: Top View, Front View, Side View
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Top View") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Front View") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Side View") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive View Diagram
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Top View: Width × Depth
                                PackingViewDiagram(
                                    viewType = PackingViewType.TOP,
                                    result = result,
                                    containerDimX = cW,
                                    containerDimY = cD,
                                    boxDimX = effectiveBoxW,
                                    boxDimY = effectiveBoxD,
                                    countX = result.boxesAcross,
                                    countY = result.boxesDeep,
                                    remainingX = result.remainingWidthMm,
                                    remainingY = result.remainingDepthMm,
                                    labelX = "Width",
                                    labelY = "Depth",
                                    unit = unit,
                                    decimals = decimals
                                )

                                // Layer Selector for Top View
                                if (result.layers > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { if (currentLayer > 1) currentLayer-- },
                                            enabled = currentLayer > 1
                                        ) {
                                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous layer")
                                        }
                                        Text(
                                            text = "Layer $currentLayer of ${result.layers}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { if (currentLayer < result.layers) currentLayer++ },
                                            enabled = currentLayer < result.layers
                                        ) {
                                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next layer")
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Front View: Width × Height
                                PackingViewDiagram(
                                    viewType = PackingViewType.FRONT,
                                    result = result,
                                    containerDimX = cW,
                                    containerDimY = cH,
                                    boxDimX = effectiveBoxW,
                                    boxDimY = effectiveBoxH,
                                    countX = result.boxesAcross,
                                    countY = result.layers,
                                    remainingX = result.remainingWidthMm,
                                    remainingY = result.remainingHeightMm,
                                    labelX = "Width",
                                    labelY = "Height",
                                    unit = unit,
                                    decimals = decimals
                                )
                            }
                            2 -> {
                                // Side View: Depth × Height
                                PackingViewDiagram(
                                    viewType = PackingViewType.SIDE,
                                    result = result,
                                    containerDimX = cD,
                                    containerDimY = cH,
                                    boxDimX = effectiveBoxD,
                                    boxDimY = effectiveBoxH,
                                    countX = result.boxesDeep,
                                    countY = result.layers,
                                    remainingX = result.remainingDepthMm,
                                    remainingY = result.remainingHeightMm,
                                    labelX = "Depth",
                                    labelY = "Height",
                                    unit = unit,
                                    decimals = decimals
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ARRANGEMENT DETAILS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ARRANGEMENT SPECIFICATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Box Capacity:", fontSize = 13.sp)
                            Text(text = "${result.capacity} boxes", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Boxes Per Layer:", fontSize = 13.sp)
                            Text(text = "${result.boxesPerLayer} boxes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Stacking Layers:", fontSize = 13.sp)
                            Text(text = "${result.layers} layers", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Grid Distribution:", fontSize = 13.sp)
                            Text(text = "${result.boxesAcross} across × ${result.boxesDeep} deep", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "UNUSED SPACING REMAINING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Width Remainder:", fontSize = 12.sp)
                            Text(text = unit.formatMm(result.remainingWidthMm, decimals), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Depth Remainder:", fontSize = 12.sp)
                            Text(text = unit.formatMm(result.remainingDepthMm, decimals), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Height Remainder:", fontSize = 12.sp)
                            Text(text = unit.formatMm(result.remainingHeightMm, decimals), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Unused Volume:", fontSize = 12.sp)
                            Text(text = LengthUnit.formatVolume(result.unusedVolumeMm3, unit, decimals), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
