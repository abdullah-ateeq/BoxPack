package com.example.boxpack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.boxpack.calculator.BoxPackCalculator
import com.example.boxpack.data.StorageRepository
import com.example.boxpack.model.AppSettings
import com.example.boxpack.model.BatchResult
import com.example.boxpack.model.BoxDimensions
import com.example.boxpack.model.ContainerDimensions
import com.example.boxpack.model.FitCheckResult
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationLock
import com.example.boxpack.model.OrientationMode
import com.example.boxpack.model.OrientationResult
import com.example.boxpack.model.PackingClearance
import com.example.boxpack.model.PackingGaps
import com.example.boxpack.model.PackingOrientation
import com.example.boxpack.model.SavedCalculation
import com.example.boxpack.model.StackingConstraint
import com.example.boxpack.model.StackingMode
import com.example.boxpack.model.WeightConstraint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BoxPackUiState(
    // Unit & Settings
    val selectedUnit: LengthUnit = LengthUnit.CENTIMETERS,
    val settings: AppSettings = AppSettings(),

    // Container Inputs (stored as display strings in selectedUnit)
    val containerWidthInput: String = "120",
    val containerDepthInput: String = "80",
    val containerHeightInput: String = "60",

    // Container Wall Thickness
    val accountForWallThickness: Boolean = false,
    val wallThicknessInput: String = "5", // in mm or unit
    val wallThicknessUnit: LengthUnit = LengthUnit.MILLIMETERS,

    // Packing Clearance around container walls (in mm)
    val sideClearanceInput: String = "0",
    val depthClearanceInput: String = "0",
    val heightClearanceInput: String = "0",

    // Box Inputs (stored as display strings in selectedUnit)
    val boxWidthInput: String = "30",
    val boxDepthInput: String = "20",
    val boxHeightInput: String = "15",

    // Box Gaps / Spacing between boxes (in mm)
    val boxWidthGapInput: String = "0",
    val boxDepthGapInput: String = "0",
    val boxHeightGapInput: String = "0",

    // Orientation Controls
    val orientationMode: OrientationMode = OrientationMode.AUTOMATIC,
    val keepUpright: Boolean = false,
    val keepFixed: Boolean = false,
    val customOrientationWidthInput: String = "",
    val customOrientationDepthInput: String = "",
    val customOrientationHeightInput: String = "",

    // Stacking Constraints
    val stackingMode: StackingMode = StackingMode.ALLOWED,
    val maxStackingLayersInput: String = "3",

    // Weight Limits
    val calculateWeight: Boolean = false,
    val boxWeightKgInput: String = "2.5",
    val hasMaxWeightLimit: Boolean = false,
    val maxLoadKgInput: String = "500",

    // Batch Calculator Mode
    val batchRequiredBoxesInput: String = "250",
    val availableContainersInput: String = "10",
    val batchResult: BatchResult? = null,
    val multipleContainerTotalCapacity: Int = 0,

    // Calculation Results
    val bestResult: OrientationResult? = null,
    val activeResult: OrientationResult? = null, // currently selected orientation for layout
    val allOrientations: List<OrientationResult> = emptyList(),
    val fitCheckResult: FitCheckResult? = null,

    // Input Validation
    val containerInputError: String? = null,
    val boxInputError: String? = null,

    // History
    val history: List<SavedCalculation> = emptyList(),

    // UI feedback message
    val snackbarMessage: String? = null
)

class BoxPackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StorageRepository(application)

    private val _uiState = MutableStateFlow(BoxPackUiState())
    val uiState: StateFlow<BoxPackUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val loadedSettings = repository.getSettings()
        val loadedHistory = repository.getHistory()

        _uiState.update { current ->
            current.copy(
                settings = loadedSettings,
                selectedUnit = loadedSettings.defaultUnit,
                orientationMode = loadedSettings.defaultOrientationMode,
                stackingMode = loadedSettings.defaultStackingMode,
                history = loadedHistory
            )
        }
        recalculate()
    }

    fun setUnit(newUnit: LengthUnit) {
        val currentUnit = _uiState.value.selectedUnit
        if (currentUnit == newUnit) return

        // Convert existing display input values to new unit without losing value
        val cW = currentUnit.toMm(_uiState.value.containerWidthInput.toDoubleOrNull() ?: 1200.0)
        val cD = currentUnit.toMm(_uiState.value.containerDepthInput.toDoubleOrNull() ?: 800.0)
        val cH = currentUnit.toMm(_uiState.value.containerHeightInput.toDoubleOrNull() ?: 600.0)

        val bW = currentUnit.toMm(_uiState.value.boxWidthInput.toDoubleOrNull() ?: 300.0)
        val bD = currentUnit.toMm(_uiState.value.boxDepthInput.toDoubleOrNull() ?: 200.0)
        val bH = currentUnit.toMm(_uiState.value.boxHeightInput.toDoubleOrNull() ?: 150.0)

        val decimals = _uiState.value.settings.decimalPlaces

        _uiState.update { current ->
            current.copy(
                selectedUnit = newUnit,
                containerWidthInput = newUnit.formatValue(cW, decimals),
                containerDepthInput = newUnit.formatValue(cD, decimals),
                containerHeightInput = newUnit.formatValue(cH, decimals),
                boxWidthInput = newUnit.formatValue(bW, decimals),
                boxDepthInput = newUnit.formatValue(bD, decimals),
                boxHeightInput = newUnit.formatValue(bH, decimals)
            )
        }
        recalculate()
    }

    fun updateContainerInputs(width: String, depth: String, height: String) {
        _uiState.update {
            it.copy(
                containerWidthInput = width,
                containerDepthInput = depth,
                containerHeightInput = height
            )
        }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun updateBoxInputs(width: String, depth: String, height: String) {
        _uiState.update {
            it.copy(
                boxWidthInput = width,
                boxDepthInput = depth,
                boxHeightInput = height
            )
        }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun setWallThicknessEnabled(enabled: Boolean) {
        _uiState.update { it.copy(accountForWallThickness = enabled) }
        recalculate()
    }

    fun setWallThicknessInput(value: String) {
        _uiState.update { it.copy(wallThicknessInput = value) }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun updateClearanceInputs(side: String, depth: String, height: String) {
        _uiState.update {
            it.copy(
                sideClearanceInput = side,
                depthClearanceInput = depth,
                heightClearanceInput = height
            )
        }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun updateBoxGaps(width: String, depth: String, height: String) {
        _uiState.update {
            it.copy(
                boxWidthGapInput = width,
                boxDepthGapInput = depth,
                boxHeightGapInput = height
            )
        }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun setOrientationMode(mode: OrientationMode) {
        _uiState.update { it.copy(orientationMode = mode) }
        recalculate()
    }

    fun setKeepUpright(enabled: Boolean) {
        _uiState.update { it.copy(keepUpright = enabled, keepFixed = if (enabled) false else it.keepFixed) }
        recalculate()
    }

    fun setKeepFixed(enabled: Boolean) {
        _uiState.update { it.copy(keepFixed = enabled, keepUpright = if (enabled) false else it.keepUpright) }
        recalculate()
    }

    fun setCustomOrientation(width: String, depth: String, height: String) {
        _uiState.update {
            it.copy(
                customOrientationWidthInput = width,
                customOrientationDepthInput = depth,
                customOrientationHeightInput = height
            )
        }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun setStackingMode(mode: StackingMode) {
        _uiState.update { it.copy(stackingMode = mode) }
        recalculate()
    }

    fun setMaxStackingLayers(layers: String) {
        _uiState.update { it.copy(maxStackingLayersInput = layers) }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun setCalculateWeight(enabled: Boolean) {
        _uiState.update { it.copy(calculateWeight = enabled) }
        recalculate()
    }

    fun setBoxWeightKg(weight: String) {
        _uiState.update { it.copy(boxWeightKgInput = weight) }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun setHasMaxWeightLimit(hasLimit: Boolean) {
        _uiState.update { it.copy(hasMaxWeightLimit = hasLimit) }
        recalculate()
    }

    fun setMaxLoadKg(maxLoad: String) {
        _uiState.update { it.copy(maxLoadKgInput = maxLoad) }
        if (_uiState.value.settings.liveCalculation) {
            recalculate()
        }
    }

    fun setBatchRequiredBoxes(boxes: String) {
        _uiState.update { it.copy(batchRequiredBoxesInput = boxes) }
        recalculateBatch()
    }

    fun setAvailableContainers(containers: String) {
        _uiState.update { it.copy(availableContainersInput = containers) }
        recalculateBatch()
    }

    fun selectOrientationForView(orientation: PackingOrientation) {
        val found = _uiState.value.allOrientations.find {
            it.orientation.widthMm == orientation.widthMm &&
            it.orientation.depthMm == orientation.depthMm &&
            it.orientation.heightMm == orientation.heightMm
        }
        if (found != null) {
            _uiState.update { it.copy(activeResult = found) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun recalculate() {
        val state = _uiState.value
        val unit = state.selectedUnit

        val cW = state.containerWidthInput.toDoubleOrNull()
        val cD = state.containerDepthInput.toDoubleOrNull()
        val cH = state.containerHeightInput.toDoubleOrNull()

        if (cW == null || cD == null || cH == null || cW <= 0.0 || cD <= 0.0 || cH <= 0.0) {
            _uiState.update {
                it.copy(
                    containerInputError = "Container dimensions must be positive numbers",
                    bestResult = null,
                    activeResult = null,
                    allOrientations = emptyList(),
                    fitCheckResult = null
                )
            }
            return
        }

        val bW = state.boxWidthInput.toDoubleOrNull()
        val bD = state.boxDepthInput.toDoubleOrNull()
        val bH = state.boxHeightInput.toDoubleOrNull()

        if (bW == null || bD == null || bH == null || bW <= 0.0 || bD <= 0.0 || bH <= 0.0) {
            _uiState.update {
                it.copy(
                    boxInputError = "Box dimensions must be positive numbers",
                    bestResult = null,
                    activeResult = null,
                    allOrientations = emptyList(),
                    fitCheckResult = null
                )
            }
            return
        }

        val container = ContainerDimensions(
            widthMm = unit.toMm(cW),
            depthMm = unit.toMm(cD),
            heightMm = unit.toMm(cH)
        )

        val box = BoxDimensions(
            widthMm = unit.toMm(bW),
            depthMm = unit.toMm(bD),
            heightMm = unit.toMm(bH)
        )

        val wallMm = if (state.accountForWallThickness) {
            state.wallThicknessUnit.toMm(state.wallThicknessInput.toDoubleOrNull() ?: 0.0)
        } else 0.0

        val clearance = PackingClearance(
            widthMm = LengthUnit.MILLIMETERS.toMm(state.sideClearanceInput.toDoubleOrNull() ?: 0.0),
            depthMm = LengthUnit.MILLIMETERS.toMm(state.depthClearanceInput.toDoubleOrNull() ?: 0.0),
            heightMm = LengthUnit.MILLIMETERS.toMm(state.heightClearanceInput.toDoubleOrNull() ?: 0.0)
        )

        val gaps = PackingGaps(
            widthMm = LengthUnit.MILLIMETERS.toMm(state.boxWidthGapInput.toDoubleOrNull() ?: 0.0),
            depthMm = LengthUnit.MILLIMETERS.toMm(state.boxDepthGapInput.toDoubleOrNull() ?: 0.0),
            heightMm = LengthUnit.MILLIMETERS.toMm(state.boxHeightGapInput.toDoubleOrNull() ?: 0.0)
        )

        val lock = when {
            state.keepFixed || state.orientationMode == OrientationMode.ORIGINAL -> OrientationLock.FIXED
            state.keepUpright -> OrientationLock.UPRIGHT_ONLY
            else -> OrientationLock.NONE
        }

        val stacking = StackingConstraint(
            mode = state.stackingMode,
            maxLayers = state.maxStackingLayersInput.toIntOrNull() ?: 1
        )

        val weight = WeightConstraint(
            isEnabled = state.calculateWeight,
            boxWeightKg = state.boxWeightKgInput.toDoubleOrNull() ?: 0.0,
            hasMaxLoad = state.hasMaxWeightLimit,
            maxLoadKg = state.maxLoadKgInput.toDoubleOrNull() ?: 0.0
        )

        var customOri: PackingOrientation? = null
        if (state.orientationMode == OrientationMode.CUSTOM) {
            val custW = state.customOrientationWidthInput.toDoubleOrNull()
            val custD = state.customOrientationDepthInput.toDoubleOrNull()
            val custH = state.customOrientationHeightInput.toDoubleOrNull()
            if (custW != null && custD != null && custH != null && custW > 0.0 && custD > 0.0 && custH > 0.0) {
                val candidate = PackingOrientation(
                    widthMm = unit.toMm(custW),
                    depthMm = unit.toMm(custD),
                    heightMm = unit.toMm(custH),
                    label = "Custom Orientation"
                )
                if (candidate.isPermutationOf(box)) {
                    customOri = candidate
                }
            }
        }

        val (best, all) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = wallMm,
            box = box,
            gaps = gaps,
            clearance = clearance,
            lock = lock,
            stacking = stacking,
            weightConstraint = weight,
            customOrientation = customOri
        )

        val fit = BoxPackCalculator.checkFit(
            container = container,
            wallThicknessMm = wallMm,
            box = box,
            clearance = clearance,
            lock = lock
        )

        _uiState.update {
            it.copy(
                containerInputError = null,
                boxInputError = null,
                bestResult = best,
                activeResult = best,
                allOrientations = all,
                fitCheckResult = fit
            )
        }

        recalculateBatch()
    }

    private fun recalculateBatch() {
        val state = _uiState.value
        val capacity = state.bestResult?.capacity ?: 0
        val reqBoxes = state.batchRequiredBoxesInput.toIntOrNull() ?: 0
        val availCont = state.availableContainersInput.toIntOrNull() ?: 0

        val batch = BoxPackCalculator.calculateBatch(reqBoxes, capacity)
        val totalAvail = availCont * capacity

        _uiState.update {
            it.copy(
                batchResult = batch,
                multipleContainerTotalCapacity = totalAvail
            )
        }
    }

    fun saveCurrentCalculation(customTitle: String? = null) {
        val state = _uiState.value
        val result = state.bestResult ?: return
        val unit = state.selectedUnit

        val cW = unit.toMm(state.containerWidthInput.toDoubleOrNull() ?: 0.0)
        val cD = unit.toMm(state.containerDepthInput.toDoubleOrNull() ?: 0.0)
        val cH = unit.toMm(state.containerHeightInput.toDoubleOrNull() ?: 0.0)

        val bW = unit.toMm(state.boxWidthInput.toDoubleOrNull() ?: 0.0)
        val bD = unit.toMm(state.boxDepthInput.toDoubleOrNull() ?: 0.0)
        val bH = unit.toMm(state.boxHeightInput.toDoubleOrNull() ?: 0.0)

        val title = customTitle?.takeIf { it.isNotBlank() } ?: "Calculation #${state.history.size + 1}"

        val saved = SavedCalculation(
            id = UUID.randomUUID().toString(),
            title = title,
            timestamp = System.currentTimeMillis(),
            containerWidthMm = cW,
            containerDepthMm = cD,
            containerHeightMm = cH,
            wallThicknessMm = if (state.accountForWallThickness) state.wallThicknessUnit.toMm(state.wallThicknessInput.toDoubleOrNull() ?: 0.0) else 0.0,
            boxWidthMm = bW,
            boxDepthMm = bD,
            boxHeightMm = bH,
            clearanceWidthMm = LengthUnit.MILLIMETERS.toMm(state.sideClearanceInput.toDoubleOrNull() ?: 0.0),
            clearanceDepthMm = LengthUnit.MILLIMETERS.toMm(state.depthClearanceInput.toDoubleOrNull() ?: 0.0),
            clearanceHeightMm = LengthUnit.MILLIMETERS.toMm(state.heightClearanceInput.toDoubleOrNull() ?: 0.0),
            gapWidthMm = LengthUnit.MILLIMETERS.toMm(state.boxWidthGapInput.toDoubleOrNull() ?: 0.0),
            gapDepthMm = LengthUnit.MILLIMETERS.toMm(state.boxDepthGapInput.toDoubleOrNull() ?: 0.0),
            gapHeightMm = LengthUnit.MILLIMETERS.toMm(state.boxHeightGapInput.toDoubleOrNull() ?: 0.0),
            orientationMode = state.orientationMode,
            capacity = result.capacity,
            across = result.boxesAcross,
            deep = result.boxesDeep,
            layers = result.layers,
            utilizationPercent = result.utilizationPercent,
            unit = unit
        )

        val updated = repository.saveCalculation(saved)
        _uiState.update { it.copy(history = updated, snackbarMessage = "Calculation saved to history") }
    }

    fun loadCalculation(calc: SavedCalculation) {
        val unit = calc.unit
        val decimals = _uiState.value.settings.decimalPlaces

        _uiState.update { current ->
            current.copy(
                selectedUnit = unit,
                containerWidthInput = unit.formatValue(calc.containerWidthMm, decimals),
                containerDepthInput = unit.formatValue(calc.containerDepthMm, decimals),
                containerHeightInput = unit.formatValue(calc.containerHeightMm, decimals),
                accountForWallThickness = calc.wallThicknessMm > 0.0,
                wallThicknessInput = if (calc.wallThicknessMm > 0.0) LengthUnit.MILLIMETERS.formatValue(calc.wallThicknessMm, decimals) else "5",
                boxWidthInput = unit.formatValue(calc.boxWidthMm, decimals),
                boxDepthInput = unit.formatValue(calc.boxDepthMm, decimals),
                boxHeightInput = unit.formatValue(calc.boxHeightMm, decimals),
                sideClearanceInput = LengthUnit.MILLIMETERS.formatValue(calc.clearanceWidthMm, decimals),
                depthClearanceInput = LengthUnit.MILLIMETERS.formatValue(calc.clearanceDepthMm, decimals),
                heightClearanceInput = LengthUnit.MILLIMETERS.formatValue(calc.clearanceHeightMm, decimals),
                boxWidthGapInput = LengthUnit.MILLIMETERS.formatValue(calc.gapWidthMm, decimals),
                boxDepthGapInput = LengthUnit.MILLIMETERS.formatValue(calc.gapDepthMm, decimals),
                boxHeightGapInput = LengthUnit.MILLIMETERS.formatValue(calc.gapHeightMm, decimals),
                orientationMode = calc.orientationMode,
                snackbarMessage = "Loaded: ${calc.title}"
            )
        }
        recalculate()
    }

    fun deleteCalculation(id: String) {
        val updated = repository.deleteCalculation(id)
        _uiState.update { it.copy(history = updated, snackbarMessage = "Calculation deleted") }
    }

    fun duplicateCalculation(calc: SavedCalculation) {
        val duplicated = calc.copy(
            id = UUID.randomUUID().toString(),
            title = "${calc.title} (Copy)",
            timestamp = System.currentTimeMillis()
        )
        val updated = repository.saveCalculation(duplicated)
        _uiState.update { it.copy(history = updated, snackbarMessage = "Duplicated: ${duplicated.title}") }
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun resetSettings() {
        val defaultSettings = repository.resetSettings()
        _uiState.update { it.copy(settings = defaultSettings, snackbarMessage = "Settings reset to defaults") }
    }
}
