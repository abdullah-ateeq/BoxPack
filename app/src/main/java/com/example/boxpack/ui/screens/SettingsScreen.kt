package com.example.boxpack.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationMode
import com.example.boxpack.model.StackingMode
import com.example.boxpack.ui.BoxPackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BoxPackViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // SECTION: APPEARANCE
            Text(
                text = "APPEARANCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Theme", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    viewModel.updateSettings(settings.copy(themeMode = key))
                                }
                            ) {
                                RadioButton(
                                    selected = settings.themeMode == key,
                                    onClick = { viewModel.updateSettings(settings.copy(themeMode = key)) }
                                )
                                Text(text = label, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Dynamic Color", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Adapt to Android system wallpaper tones", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(dynamicColor = it)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: CALCULATOR DEFAULTS
            Text(
                text = "CALCULATOR DEFAULTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Default Unit
                    var unitMenuExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Default Unit", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        ExposedDropdownMenuBox(
                            expanded = unitMenuExpanded,
                            onExpandedChange = { unitMenuExpanded = !unitMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = "${settings.defaultUnit.label} (${settings.defaultUnit.symbol})",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(170.dp),
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
                                            viewModel.updateSettings(settings.copy(defaultUnit = unit))
                                            unitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Precision Decimal Places (0, 1, 2, 3, 4)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Decimal Places", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row {
                            listOf(0, 1, 2, 3, 4).forEach { dec ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable { viewModel.updateSettings(settings.copy(decimalPlaces = dec)) }
                                ) {
                                    RadioButton(
                                        selected = settings.decimalPlaces == dec,
                                        onClick = { viewModel.updateSettings(settings.copy(decimalPlaces = dec)) }
                                    )
                                    Text(text = "$dec", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: DISPLAY SETTINGS
            Text(
                text = "DISPLAY SETTINGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        title = "Show Container Diagram",
                        subtitle = "Display 3D wireframe box with dimensions",
                        checked = settings.showContainerDiagram,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showContainerDiagram = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Show Packing Grid",
                        subtitle = "Visual Top, Front, and Side layer grids",
                        checked = settings.showPackingGrid,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showPackingGrid = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Show Orientation Comparison",
                        subtitle = "List and compare all valid box orientations",
                        checked = settings.showOrientationComparison,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showOrientationComparison = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Show Calculation Breakdown",
                        subtitle = "Expandable step-by-step formula explanations",
                        checked = settings.showCalculationBreakdown,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showCalculationBreakdown = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Show Utilization Metrics",
                        subtitle = "Volume, floor, and vertical utilization %",
                        checked = settings.showUtilization,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showUtilization = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Show Volume Theory Comparison",
                        subtitle = "Educational volume ratio vs physical capacity comparison",
                        checked = settings.showVolumeComparison,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showVolumeComparison = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: BEHAVIOR
            Text(
                text = "BEHAVIOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        title = "Live Calculation",
                        subtitle = "Recalculate automatically while typing",
                        checked = settings.liveCalculation,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(liveCalculation = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Confirm Delete",
                        subtitle = "Confirm before deleting a history item",
                        checked = settings.confirmDelete,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(confirmDelete = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Animations",
                        subtitle = "Smooth UI and diagram transitions",
                        checked = settings.animations,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(animations = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reset Settings Button (Does not delete history)
            OutlinedButton(
                onClick = { viewModel.resetSettings() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Settings to Default")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
