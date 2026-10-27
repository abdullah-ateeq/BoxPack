package com.example.boxpack.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxpack.data.ExportHelper
import com.example.boxpack.model.BoxDimensions
import com.example.boxpack.model.ContainerDimensions
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationMode
import com.example.boxpack.model.PackingOrientation
import com.example.boxpack.model.StackingMode
import com.example.boxpack.ui.BoxPackViewModel
import com.example.boxpack.ui.components.BestFitCard
import com.example.boxpack.ui.components.Box3DDiagram
import com.example.boxpack.ui.components.CalculationBreakdownCard
import com.example.boxpack.ui.components.FitCheckBanner
import com.example.boxpack.ui.components.OrientationComparisonCard
import com.example.boxpack.ui.components.SecondaryMetricsSection
import com.example.boxpack.ui.components.VolumeTheoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BoxPackViewModel,
    onNavigateToLayout: () -> Unit,
    onNavigateToBatch: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }

    // Export Launchers using SAF
    var pendingExportType by remember { mutableStateOf<String?>(null) } // "TXT", "CSV", "JSON"
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            when (pendingExportType) {
                "CSV" -> "text/csv"
                "JSON" -> "application/json"
                else -> "text/plain"
            }
        )
    ) { uri ->
        if (uri != null && state.bestResult != null) {
            val cont = ContainerDimensions(
                state.selectedUnit.toMm(state.containerWidthInput.toDoubleOrNull() ?: 0.0),
                state.selectedUnit.toMm(state.containerDepthInput.toDoubleOrNull() ?: 0.0),
                state.selectedUnit.toMm(state.containerHeightInput.toDoubleOrNull() ?: 0.0)
            )
            val box = BoxDimensions(
                state.selectedUnit.toMm(state.boxWidthInput.toDoubleOrNull() ?: 0.0),
                state.selectedUnit.toMm(state.boxDepthInput.toDoubleOrNull() ?: 0.0),
                state.selectedUnit.toMm(state.boxHeightInput.toDoubleOrNull() ?: 0.0)
            )
            val content = when (pendingExportType) {
                "CSV" -> ExportHelper.generateCsv(cont, box, state.bestResult!!, state.selectedUnit, state.settings.decimalPlaces)
                "JSON" -> ExportHelper.generateJson(cont, box, state.bestResult!!, state.selectedUnit, state.settings.decimalPlaces)
                else -> ExportHelper.generateTxt(cont, box, state.bestResult!!, state.selectedUnit, state.settings.decimalPlaces)
            }
            ExportHelper.writeToUri(context, uri, content)
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BoxPack",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Find the maximum number of boxes that fit.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share & Save Quick Action Buttons
                Row {
                    IconButton(
                        onClick = {
                            state.bestResult?.let { best ->
                                val cont = ContainerDimensions(
                                    state.selectedUnit.toMm(state.containerWidthInput.toDoubleOrNull() ?: 0.0),
                                    state.selectedUnit.toMm(state.containerDepthInput.toDoubleOrNull() ?: 0.0),
                                    state.selectedUnit.toMm(state.containerHeightInput.toDoubleOrNull() ?: 0.0)
                                )
                                val box = BoxDimensions(
                                    state.selectedUnit.toMm(state.boxWidthInput.toDoubleOrNull() ?: 0.0),
                                    state.selectedUnit.toMm(state.boxDepthInput.toDoubleOrNull() ?: 0.0),
                                    state.selectedUnit.toMm(state.boxHeightInput.toDoubleOrNull() ?: 0.0)
                                )
                                val shareText = ExportHelper.generateShareText(cont, box, best, state.selectedUnit, state.settings.decimalPlaces)
                                ExportHelper.shareResult(context, shareText)
                            }
                        },
                        enabled = state.bestResult != null
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share result")
                    }

                    IconButton(
                        onClick = {
                            saveTitleInput = "Calculation #${state.history.size + 1}"
                            showSaveDialog = true
                        },
                        enabled = state.bestResult != null
                    ) {
                        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Save to history")
                    }

                    IconButton(onClick = onNavigateToAbout) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "About BoxPack")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UNIT SYSTEM SELECTOR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNIT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var unitMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = !unitMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = "${state.selectedUnit.label} (${state.selectedUnit.symbol})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .width(180.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        LengthUnit.values().forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.label} (${unit.symbol})") },
                                onClick = {
                                    viewModel.setUnit(unit)
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: CONTAINER DIMENSIONS
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CONTAINER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.containerWidthInput,
                            onValueChange = { viewModel.updateContainerInputs(it, state.containerDepthInput, state.containerHeightInput) },
                            label = { Text("Width (${state.selectedUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.containerDepthInput,
                            onValueChange = { viewModel.updateContainerInputs(state.containerWidthInput, it, state.containerHeightInput) },
                            label = { Text("Depth (${state.selectedUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.containerHeightInput,
                            onValueChange = { viewModel.updateContainerInputs(state.containerWidthInput, state.containerDepthInput, it) },
                            label = { Text("Height (${state.selectedUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Container 3D Diagram (if enabled in settings)
                    if (state.settings.showContainerDiagram) {
                        val cW = state.containerWidthInput.toDoubleOrNull() ?: 0.0
                        val cD = state.containerDepthInput.toDoubleOrNull() ?: 0.0
                        val cH = state.containerHeightInput.toDoubleOrNull() ?: 0.0
                        if (cW > 0.0 && cD > 0.0 && cH > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box3DDiagram(
                                widthMm = state.selectedUnit.toMm(cW),
                                depthMm = state.selectedUnit.toMm(cD),
                                heightMm = state.selectedUnit.toMm(cH),
                                unit = state.selectedUnit,
                                decimals = state.settings.decimalPlaces,
                                title = "CONTAINER",
                                isBox = false
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Wall Thickness Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = state.accountForWallThickness,
                            onCheckedChange = { viewModel.setWallThicknessEnabled(it) }
                        )
                        Text(
                            text = "Account for wall thickness",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (state.accountForWallThickness) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.wallThicknessInput,
                                onValueChange = { viewModel.setWallThicknessInput(it) },
                                label = { Text("Wall Thickness (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(180.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION: PACKING CLEARANCE
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                var clearanceExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearanceExpanded = !clearanceExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PACKING CLEARANCE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Intentional space around container walls",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = if (clearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = clearanceExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.sideClearanceInput,
                                    onValueChange = { viewModel.updateClearanceInputs(it, state.depthClearanceInput, state.heightClearanceInput) },
                                    label = { Text("Side (mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.depthClearanceInput,
                                    onValueChange = { viewModel.updateClearanceInputs(state.sideClearanceInput, it, state.heightClearanceInput) },
                                    label = { Text("Depth (mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.heightClearanceInput,
                                    onValueChange = { viewModel.updateClearanceInputs(state.sideClearanceInput, state.depthClearanceInput, it) },
                                    label = { Text("Height (mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION: SMALL BOX DIMENSIONS
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BOX",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.boxWidthInput,
                            onValueChange = { viewModel.updateBoxInputs(it, state.boxDepthInput, state.boxHeightInput) },
                            label = { Text("Width (${state.selectedUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.boxDepthInput,
                            onValueChange = { viewModel.updateBoxInputs(state.boxWidthInput, it, state.boxHeightInput) },
                            label = { Text("Depth (${state.selectedUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.boxHeightInput,
                            onValueChange = { viewModel.updateBoxInputs(state.boxWidthInput, state.boxDepthInput, it) },
                            label = { Text("Height (${state.selectedUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Box 3D Diagram
                    if (state.settings.showContainerDiagram) {
                        val bW = state.boxWidthInput.toDoubleOrNull() ?: 0.0
                        val bD = state.boxDepthInput.toDoubleOrNull() ?: 0.0
                        val bH = state.boxHeightInput.toDoubleOrNull() ?: 0.0
                        if (bW > 0.0 && bD > 0.0 && bH > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box3DDiagram(
                                widthMm = state.selectedUnit.toMm(bW),
                                depthMm = state.selectedUnit.toMm(bD),
                                heightMm = state.selectedUnit.toMm(bH),
                                unit = state.selectedUnit,
                                decimals = state.settings.decimalPlaces,
                                title = "BOX",
                                isBox = true
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Gaps between boxes
                    var gapsExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { gapsExpanded = !gapsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BOX GAP / SPACING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gap between boxes for ventilation or padding",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = if (gapsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = gapsExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.boxWidthGapInput,
                                    onValueChange = { viewModel.updateBoxGaps(it, state.boxDepthGapInput, state.boxHeightGapInput) },
                                    label = { Text("Width Gap (mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.boxDepthGapInput,
                                    onValueChange = { viewModel.updateBoxGaps(state.boxWidthGapInput, it, state.boxHeightGapInput) },
                                    label = { Text("Depth Gap (mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.boxHeightGapInput,
                                    onValueChange = { viewModel.updateBoxGaps(state.boxWidthGapInput, state.boxDepthGapInput, it) },
                                    label = { Text("Height Gap (mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION: BOX ORIENTATION CONTROLS
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BOX ORIENTATION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.orientationMode == OrientationMode.AUTOMATIC,
                            onClick = { viewModel.setOrientationMode(OrientationMode.AUTOMATIC) }
                        )
                        Text(
                            text = "Automatic / Best Fit (Tests all 6 orientations)",
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.setOrientationMode(OrientationMode.AUTOMATIC) }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.orientationMode == OrientationMode.ORIGINAL,
                            onClick = { viewModel.setOrientationMode(OrientationMode.ORIGINAL) }
                        )
                        Text(
                            text = "Keep Original Orientation",
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.setOrientationMode(OrientationMode.ORIGINAL) }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.orientationMode == OrientationMode.CUSTOM,
                            onClick = { viewModel.setOrientationMode(OrientationMode.CUSTOM) }
                        )
                        Text(
                            text = "Custom Orientation (Permutation of dimensions)",
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.setOrientationMode(OrientationMode.CUSTOM) }
                        )
                    }

                    if (state.orientationMode == OrientationMode.CUSTOM) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.customOrientationWidthInput,
                                onValueChange = { viewModel.setCustomOrientation(it, state.customOrientationDepthInput, state.customOrientationHeightInput) },
                                label = { Text("Width") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = state.customOrientationDepthInput,
                                onValueChange = { viewModel.setCustomOrientation(state.customOrientationWidthInput, it, state.customOrientationHeightInput) },
                                label = { Text("Depth") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = state.customOrientationHeightInput,
                                onValueChange = { viewModel.setCustomOrientation(state.customOrientationWidthInput, state.customOrientationDepthInput, it) },
                                label = { Text("Height") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Orientation locks
                    Text(
                        text = "ORIENTATION LOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.keepUpright,
                            onCheckedChange = { viewModel.setKeepUpright(it) }
                        )
                        Text(
                            text = "Keep box upright (Height cannot swap, only W×D)",
                            fontSize = 13.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.keepFixed,
                            onCheckedChange = { viewModel.setKeepFixed(it) }
                        )
                        Text(
                            text = "Keep box orientation fixed (No rotation)",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION: STACKING & WEIGHT (Optional)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                var advancedExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { advancedExpanded = !advancedExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stacking & Weight Constraints",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = advancedExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(text = "STACKING RULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.stackingMode == StackingMode.ALLOWED,
                                    onClick = { viewModel.setStackingMode(StackingMode.ALLOWED) }
                                )
                                Text(text = "Boxes may be stacked (Unlimited layers)", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.stackingMode == StackingMode.DISABLED,
                                    onClick = { viewModel.setStackingMode(StackingMode.DISABLED) }
                                )
                                Text(text = "No stacking (Single layer only)", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.stackingMode == StackingMode.LIMITED,
                                    onClick = { viewModel.setStackingMode(StackingMode.LIMITED) }
                                )
                                Text(text = "Maximum stacking layers", fontSize = 13.sp)
                            }

                            if (state.stackingMode == StackingMode.LIMITED) {
                                OutlinedTextField(
                                    value = state.maxStackingLayersInput,
                                    onValueChange = { viewModel.setMaxStackingLayers(it) },
                                    label = { Text("Max Layers") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .width(140.dp)
                                        .padding(start = 12.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Text(text = "WEIGHT & LOAD LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.calculateWeight,
                                    onCheckedChange = { viewModel.setCalculateWeight(it) }
                                )
                                Text(text = "Calculate total box weight", fontSize = 13.sp)
                            }

                            if (state.calculateWeight) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.boxWeightKgInput,
                                        onValueChange = { viewModel.setBoxWeightKg(it) },
                                        label = { Text("Box Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                    Checkbox(
                                        checked = state.hasMaxWeightLimit,
                                        onCheckedChange = { viewModel.setHasMaxWeightLimit(it) }
                                    )
                                    Text(text = "Container maximum load limit", fontSize = 13.sp)
                                }

                                if (state.hasMaxWeightLimit) {
                                    OutlinedTextField(
                                        value = state.maxLoadKgInput,
                                        onValueChange = { viewModel.setMaxLoadKg(it) },
                                        label = { Text("Max Load (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .width(180.dp)
                                            .padding(start = 12.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FIT CHECK BANNER
            state.fitCheckResult?.let { fitResult ->
                FitCheckBanner(
                    fitResult = fitResult,
                    unit = state.selectedUnit,
                    decimals = state.settings.decimalPlaces
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // PRIMARY RESULT: BEST FIT CARD
            state.bestResult?.let { best ->
                BestFitCard(
                    result = best,
                    unit = state.selectedUnit,
                    decimals = state.settings.decimalPlaces
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons: View Layout & Batch Requirement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToLayout,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.GridView, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Packing Layout")
                    }

                    FilledTonalButton(
                        onClick = onNavigateToBatch,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batch Calculator")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary metrics cards
                SecondaryMetricsSection(
                    result = best,
                    unit = state.selectedUnit,
                    decimals = state.settings.decimalPlaces,
                    showUtilization = state.settings.showUtilization
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Educational Theory Card
                if (state.settings.showVolumeComparison) {
                    val contW = state.selectedUnit.toMm(state.containerWidthInput.toDoubleOrNull() ?: 0.0)
                    val contD = state.selectedUnit.toMm(state.containerDepthInput.toDoubleOrNull() ?: 0.0)
                    val contH = state.selectedUnit.toMm(state.containerHeightInput.toDoubleOrNull() ?: 0.0)
                    val boxW = state.selectedUnit.toMm(state.boxWidthInput.toDoubleOrNull() ?: 0.0)
                    val boxD = state.selectedUnit.toMm(state.boxDepthInput.toDoubleOrNull() ?: 0.0)
                    val boxH = state.selectedUnit.toMm(state.boxHeightInput.toDoubleOrNull() ?: 0.0)

                    VolumeTheoryCard(
                        containerVolumeMm3 = contW * contD * contH,
                        boxVolumeMm3 = boxW * boxD * boxH,
                        actualCapacity = best.capacity,
                        unit = state.selectedUnit,
                        decimals = state.settings.decimalPlaces
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Calculation Breakdown Card
                if (state.settings.showCalculationBreakdown) {
                    val cont = ContainerDimensions(
                        state.selectedUnit.toMm(state.containerWidthInput.toDoubleOrNull() ?: 0.0),
                        state.selectedUnit.toMm(state.containerDepthInput.toDoubleOrNull() ?: 0.0),
                        state.selectedUnit.toMm(state.containerHeightInput.toDoubleOrNull() ?: 0.0)
                    )
                    val box = BoxDimensions(
                        state.selectedUnit.toMm(state.boxWidthInput.toDoubleOrNull() ?: 0.0),
                        state.selectedUnit.toMm(state.boxDepthInput.toDoubleOrNull() ?: 0.0),
                        state.selectedUnit.toMm(state.boxHeightInput.toDoubleOrNull() ?: 0.0)
                    )

                    CalculationBreakdownCard(
                        container = cont,
                        box = box,
                        result = best,
                        unit = state.selectedUnit,
                        decimals = state.settings.decimalPlaces
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Orientation Results comparison list
                if (state.settings.showOrientationComparison && state.allOrientations.isNotEmpty()) {
                    OrientationComparisonCard(
                        orientations = state.allOrientations,
                        unit = state.selectedUnit,
                        selectedOrientation = state.activeResult?.orientation,
                        onSelectOrientation = { viewModel.selectOrientationForView(it) },
                        decimals = state.settings.decimalPlaces
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Export Options Bar
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "EXPORT REPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pendingExportType = "TXT"
                                    exportLauncher.launch("boxpack_report.txt")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("TXT")
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingExportType = "CSV"
                                    exportLauncher.launch("boxpack_data.csv")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CSV")
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingExportType = "JSON"
                                    exportLauncher.launch("boxpack_export.json")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("JSON")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Calculation") },
            text = {
                OutlinedTextField(
                    value = saveTitleInput,
                    onValueChange = { saveTitleInput = it },
                    label = { Text("Title or Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentCalculation(saveTitleInput)
                        showSaveDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
