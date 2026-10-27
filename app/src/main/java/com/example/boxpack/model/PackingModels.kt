package com.example.boxpack.model

/**
 * Physical container outer dimensions in millimeters.
 */
data class ContainerDimensions(
    val widthMm: Double,
    val depthMm: Double,
    val heightMm: Double
) {
    val volumeMm3: Double get() = widthMm * depthMm * heightMm

    fun isValid(): Boolean = widthMm > 0.0 && depthMm > 0.0 && heightMm > 0.0
}

/**
 * Physical box dimensions in millimeters.
 */
data class BoxDimensions(
    val widthMm: Double,
    val depthMm: Double,
    val heightMm: Double
) {
    val volumeMm3: Double get() = widthMm * depthMm * heightMm

    fun isValid(): Boolean = widthMm > 0.0 && depthMm > 0.0 && heightMm > 0.0
}

/**
 * Clearance space around container walls in millimeters.
 */
data class PackingClearance(
    val widthMm: Double = 0.0,
    val depthMm: Double = 0.0,
    val heightMm: Double = 0.0
) {
    fun isValid(): Boolean = widthMm >= 0.0 && depthMm >= 0.0 && heightMm >= 0.0
}

/**
 * Gap between individual boxes in millimeters.
 */
data class PackingGaps(
    val widthMm: Double = 0.0,
    val depthMm: Double = 0.0,
    val heightMm: Double = 0.0
) {
    fun isValid(): Boolean = widthMm >= 0.0 && depthMm >= 0.0 && heightMm >= 0.0
    val hasGaps: Boolean get() = widthMm > 0.0 || depthMm > 0.0 || heightMm > 0.0
}

/**
 * Axis-aligned orientation of the box in millimeters.
 */
data class PackingOrientation(
    val widthMm: Double,
    val depthMm: Double,
    val heightMm: Double,
    val label: String = ""
) {
    fun formatDimensions(unit: LengthUnit, decimals: Int = 1): String {
        return "${unit.formatValue(widthMm, decimals)} × ${unit.formatValue(depthMm, decimals)} × ${unit.formatValue(heightMm, decimals)} ${unit.symbol}"
    }

    /**
     * Checks if this orientation is a valid permutation of the original box dimensions.
     */
    fun isPermutationOf(original: BoxDimensions, toleranceMm: Double = 0.1): Boolean {
        val origSorted = listOf(original.widthMm, original.depthMm, original.heightMm).sorted()
        val curSorted = listOf(widthMm, depthMm, heightMm).sorted()
        return origSorted.zip(curSorted).all { (a, b) -> Math.abs(a - b) <= toleranceMm }
    }
}

/**
 * Calculation result for a specific orientation.
 */
data class OrientationResult(
    val orientation: PackingOrientation,
    val boxesAcross: Int,
    val boxesDeep: Int,
    val layers: Int,
    val boxesPerLayer: Int,
    val capacity: Int,
    val remainingWidthMm: Double,
    val remainingDepthMm: Double,
    val remainingHeightMm: Double,
    val occupiedVolumeMm3: Double,
    val unusedVolumeMm3: Double,
    val utilizationPercent: Double,
    val floorUtilizationPercent: Double = 0.0,
    val verticalUtilizationPercent: Double = 0.0,
    val theoreticalVolumeRatio: Double = 0.0,
    val isBestFit: Boolean = false,
    val isLimitingByWeight: Boolean = false,
    val weightCapacity: Int = Int.MAX_VALUE,
    val totalWeightKg: Double = 0.0
)

/**
 * Result of batch calculations for fulfilling required boxes.
 */
data class BatchResult(
    val requiredBoxes: Int,
    val capacityPerContainer: Int,
    val containersRequired: Int,
    val totalCapacity: Int,
    val unusedCapacity: Int,
    val lastContainerBoxes: Int
)

/**
 * Weight constraint configuration.
 */
data class WeightConstraint(
    val isEnabled: Boolean = false,
    val boxWeightKg: Double = 0.0,
    val hasMaxLoad: Boolean = false,
    val maxLoadKg: Double = 0.0
)

/**
 * Stacking limits configuration.
 */
data class StackingConstraint(
    val mode: StackingMode = StackingMode.ALLOWED,
    val maxLayers: Int = 1
)

enum class OrientationMode {
    AUTOMATIC,
    ORIGINAL,
    CUSTOM
}

enum class StackingMode {
    ALLOWED,
    DISABLED,
    LIMITED
}

enum class OrientationLock {
    NONE,           // All 6 orientations tested
    UPRIGHT_ONLY,   // Height is locked to original box height; only width and depth may swap
    FIXED           // Fixed to original orientation only
}

data class DimensionFitNote(
    val axisName: String,
    val boxDimMm: Double,
    val containerDimMm: Double,
    val fits: Boolean
)

data class FitCheckResult(
    val fits: Boolean,
    val maxCapacity: Int,
    val bestOrientation: PackingOrientation?,
    val failedDimensions: List<DimensionFitNote>
)

data class SavedCalculation(
    val id: String,
    val title: String,
    val timestamp: Long,
    val containerWidthMm: Double,
    val containerDepthMm: Double,
    val containerHeightMm: Double,
    val wallThicknessMm: Double,
    val boxWidthMm: Double,
    val boxDepthMm: Double,
    val boxHeightMm: Double,
    val clearanceWidthMm: Double,
    val clearanceDepthMm: Double,
    val clearanceHeightMm: Double,
    val gapWidthMm: Double,
    val gapDepthMm: Double,
    val gapHeightMm: Double,
    val orientationMode: OrientationMode,
    val capacity: Int,
    val across: Int,
    val deep: Int,
    val layers: Int,
    val utilizationPercent: Double,
    val unit: LengthUnit = LengthUnit.CENTIMETERS
)

data class AppSettings(
    val defaultUnit: LengthUnit = LengthUnit.CENTIMETERS,
    val defaultOrientationMode: OrientationMode = OrientationMode.AUTOMATIC,
    val defaultBoxGapMm: Double = 0.0,
    val defaultWallThicknessMm: Double = 0.0,
    val defaultStackingMode: StackingMode = StackingMode.ALLOWED,
    val showContainerDiagram: Boolean = true,
    val showPackingGrid: Boolean = true,
    val showOrientationComparison: Boolean = true,
    val showCalculationBreakdown: Boolean = true,
    val showUtilization: Boolean = true,
    val showVolumeComparison: Boolean = true,
    val decimalPlaces: Int = 2,
    val liveCalculation: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val confirmDelete: Boolean = true,
    val restoreLastCalculation: Boolean = true,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val dynamicColor: Boolean = true
)
