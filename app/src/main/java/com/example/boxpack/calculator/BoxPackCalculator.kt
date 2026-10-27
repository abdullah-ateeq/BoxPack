package com.example.boxpack.calculator

import com.example.boxpack.model.BatchResult
import com.example.boxpack.model.BoxDimensions
import com.example.boxpack.model.ContainerDimensions
import com.example.boxpack.model.DimensionFitNote
import com.example.boxpack.model.FitCheckResult
import com.example.boxpack.model.OrientationLock
import com.example.boxpack.model.OrientationResult
import com.example.boxpack.model.PackingClearance
import com.example.boxpack.model.PackingGaps
import com.example.boxpack.model.PackingOrientation
import com.example.boxpack.model.StackingConstraint
import com.example.boxpack.model.StackingMode
import com.example.boxpack.model.WeightConstraint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Core calculation engine for physical rectangular packing.
 * All calculations use millimeter units internally and never use volume division alone.
 */
object BoxPackCalculator {

    /**
     * Generate all unique axis-aligned orientations based on the box dimensions and orientation lock.
     */
    fun generateOrientations(
        box: BoxDimensions,
        lock: OrientationLock = OrientationLock.NONE,
        customOrientation: PackingOrientation? = null
    ): List<PackingOrientation> {
        if (!box.isValid()) return emptyList()

        if (customOrientation != null) {
            return listOf(customOrientation)
        }

        val w = box.widthMm
        val d = box.depthMm
        val h = box.heightMm

        val candidates = when (lock) {
            OrientationLock.FIXED -> listOf(
                PackingOrientation(w, d, h, "Original (W×D×H)")
            )
            OrientationLock.UPRIGHT_ONLY -> listOf(
                PackingOrientation(w, d, h, "Upright (W×D×H)"),
                PackingOrientation(d, w, h, "Upright Rotated (D×W×H)")
            )
            OrientationLock.NONE -> listOf(
                PackingOrientation(w, d, h, "W × D × H"),
                PackingOrientation(w, h, d, "W × H × D"),
                PackingOrientation(d, w, h, "D × W × H"),
                PackingOrientation(d, h, w, "D × H × W"),
                PackingOrientation(h, w, d, "H × W × D"),
                PackingOrientation(h, d, w, "H × D × W")
            )
        }

        // Filter duplicates when dimensions are equal (using a tolerance of 0.01 mm)
        val unique = mutableListOf<PackingOrientation>()
        for (cand in candidates) {
            val exists = unique.any {
                Math.abs(it.widthMm - cand.widthMm) < 0.01 &&
                Math.abs(it.depthMm - cand.depthMm) < 0.01 &&
                Math.abs(it.heightMm - cand.heightMm) < 0.01
            }
            if (!exists) {
                unique.add(cand)
            }
        }
        return unique
    }

    /**
     * Calculate packing dimensions and capacity for a single orientation.
     */
    fun calculateOrientation(
        container: ContainerDimensions,
        wallThicknessMm: Double,
        boxOrientation: PackingOrientation,
        originalBox: BoxDimensions,
        gaps: PackingGaps = PackingGaps(),
        clearance: PackingClearance = PackingClearance(),
        stacking: StackingConstraint = StackingConstraint(),
        weightConstraint: WeightConstraint? = null
    ): OrientationResult {
        val usableWidth = max(0.0, container.widthMm - 2.0 * max(0.0, wallThicknessMm))
        val usableDepth = max(0.0, container.depthMm - 2.0 * max(0.0, wallThicknessMm))
        val usableHeight = max(0.0, container.heightMm - 2.0 * max(0.0, wallThicknessMm))

        val packingWidth = max(0.0, usableWidth - 2.0 * max(0.0, clearance.widthMm))
        val packingDepth = max(0.0, usableDepth - 2.0 * max(0.0, clearance.depthMm))
        val packingHeight = max(0.0, usableHeight - 2.0 * max(0.0, clearance.heightMm))

        // Effective dimensions include box gaps (packing envelope)
        val effectiveWidth = boxOrientation.widthMm + max(0.0, gaps.widthMm)
        val effectiveDepth = boxOrientation.depthMm + max(0.0, gaps.depthMm)
        val effectiveHeight = boxOrientation.heightMm + max(0.0, gaps.heightMm)

        val acrossLong = if (effectiveWidth > 0.0) floor(packingWidth / effectiveWidth).toLong() else 0L
        val deepLong = if (effectiveDepth > 0.0) floor(packingDepth / effectiveDepth).toLong() else 0L
        var layersLong = if (effectiveHeight > 0.0) floor(packingHeight / effectiveHeight).toLong() else 0L

        // Apply stacking constraint
        when (stacking.mode) {
            StackingMode.DISABLED -> {
                if (layersLong > 0L) layersLong = 1L
            }
            StackingMode.LIMITED -> {
                layersLong = min(layersLong, max(1, stacking.maxLayers).toLong())
            }
            StackingMode.ALLOWED -> {
                // Unlimited layers up to container height
            }
        }

        // Clamp to safe Int ranges
        val across = min(Int.MAX_VALUE.toLong(), max(0L, acrossLong)).toInt()
        val deep = min(Int.MAX_VALUE.toLong(), max(0L, deepLong)).toInt()
        val layers = min(Int.MAX_VALUE.toLong(), max(0L, layersLong)).toInt()

        val boxesPerLayerLong = across.toLong() * deep.toLong()
        val boxesPerLayer = min(Int.MAX_VALUE.toLong(), boxesPerLayerLong).toInt()

        val geometricCapacityLong = boxesPerLayerLong * layers.toLong()
        val geometricCapacity = min(Int.MAX_VALUE.toLong(), geometricCapacityLong).toInt()

        // Weight evaluation
        var isLimitingByWeight = false
        var weightCapacity = Int.MAX_VALUE
        var finalCapacity = geometricCapacity

        if (weightConstraint != null && weightConstraint.isEnabled && weightConstraint.boxWeightKg > 0.0) {
            if (weightConstraint.hasMaxLoad && weightConstraint.maxLoadKg > 0.0) {
                val maxLoadCapacityLong = floor(weightConstraint.maxLoadKg / weightConstraint.boxWeightKg).toLong()
                weightCapacity = min(Int.MAX_VALUE.toLong(), max(0L, maxLoadCapacityLong)).toInt()
                if (weightCapacity < geometricCapacity) {
                    finalCapacity = weightCapacity
                    isLimitingByWeight = true
                }
            }
        }

        val totalWeightKg = if (weightConstraint != null && weightConstraint.isEnabled) {
            finalCapacity * weightConstraint.boxWeightKg
        } else {
            0.0
        }

        // Physical occupied volume uses ACTUAL original box dimensions
        val actualBoxVolumeMm3 = originalBox.volumeMm3
        val occupiedVolumeMm3 = finalCapacity.toDouble() * actualBoxVolumeMm3

        val packingVolumeMm3 = packingWidth * packingDepth * packingHeight
        val unusedVolumeMm3 = max(0.0, packingVolumeMm3 - occupiedVolumeMm3)

        val utilizationPercent = if (packingVolumeMm3 > 0.0) {
            min(100.0, max(0.0, (occupiedVolumeMm3 / packingVolumeMm3) * 100.0))
        } else {
            0.0
        }

        // Floor utilization
        val floorAreaMm2 = packingWidth * packingDepth
        val floorUsedMm2 = (across.toDouble() * effectiveWidth) * (deep.toDouble() * effectiveDepth)
        val floorUtilizationPercent = if (floorAreaMm2 > 0.0) {
            min(100.0, max(0.0, (floorUsedMm2 / floorAreaMm2) * 100.0))
        } else {
            0.0
        }

        // Vertical utilization
        val verticalUsedMm = layers.toDouble() * effectiveHeight
        val verticalUtilizationPercent = if (packingHeight > 0.0) {
            min(100.0, max(0.0, (verticalUsedMm / packingHeight) * 100.0))
        } else {
            0.0
        }

        val remainingWidthMm = max(0.0, packingWidth - across.toDouble() * effectiveWidth)
        val remainingDepthMm = max(0.0, packingDepth - deep.toDouble() * effectiveDepth)
        val remainingHeightMm = max(0.0, packingHeight - layers.toDouble() * effectiveHeight)

        val theoreticalVolumeRatio = if (actualBoxVolumeMm3 > 0.0) {
            packingVolumeMm3 / actualBoxVolumeMm3
        } else {
            0.0
        }

        return OrientationResult(
            orientation = boxOrientation,
            boxesAcross = across,
            boxesDeep = deep,
            layers = layers,
            boxesPerLayer = boxesPerLayer,
            capacity = finalCapacity,
            remainingWidthMm = remainingWidthMm,
            remainingDepthMm = remainingDepthMm,
            remainingHeightMm = remainingHeightMm,
            occupiedVolumeMm3 = occupiedVolumeMm3,
            unusedVolumeMm3 = unusedVolumeMm3,
            utilizationPercent = utilizationPercent,
            floorUtilizationPercent = floorUtilizationPercent,
            verticalUtilizationPercent = verticalUtilizationPercent,
            theoreticalVolumeRatio = theoreticalVolumeRatio,
            isBestFit = false,
            isLimitingByWeight = isLimitingByWeight,
            weightCapacity = weightCapacity,
            totalWeightKg = totalWeightKg
        )
    }

    /**
     * Test all valid orientations, evaluate capacity, and identify best orientation.
     */
    fun calculateBestOrientation(
        container: ContainerDimensions,
        wallThicknessMm: Double,
        box: BoxDimensions,
        gaps: PackingGaps = PackingGaps(),
        clearance: PackingClearance = PackingClearance(),
        lock: OrientationLock = OrientationLock.NONE,
        stacking: StackingConstraint = StackingConstraint(),
        weightConstraint: WeightConstraint? = null,
        customOrientation: PackingOrientation? = null
    ): Pair<OrientationResult?, List<OrientationResult>> {
        val orientations = generateOrientations(box, lock, customOrientation)
        if (orientations.isEmpty()) return Pair(null, emptyList())

        val results = orientations.map { orientation ->
            calculateOrientation(
                container = container,
                wallThicknessMm = wallThicknessMm,
                boxOrientation = orientation,
                originalBox = box,
                gaps = gaps,
                clearance = clearance,
                stacking = stacking,
                weightConstraint = weightConstraint
            )
        }

        // Sort as specified in prompt:
        // 1. Capacity descending
        // 2. Unused volume ascending
        // 3. Number of layers descending
        // 4. Highest utilization descending
        // 5. Prefer original orientation
        val sorted = results.sortedWith(
            compareByDescending<OrientationResult> { it.capacity }
                .thenBy { it.unusedVolumeMm3 }
                .thenByDescending { it.layers }
                .thenByDescending { it.utilizationPercent }
                .thenByDescending {
                    it.orientation.widthMm == box.widthMm &&
                    it.orientation.depthMm == box.depthMm &&
                    it.orientation.heightMm == box.heightMm
                }
        )

        val best = sorted.firstOrNull()?.copy(isBestFit = true)
        val markedResults = sorted.map {
            if (it.orientation == best?.orientation) it.copy(isBestFit = true) else it
        }

        return Pair(best, markedResults)
    }

    /**
     * Checks if the box fits in at least one orientation, and provides dimension failure notes if not.
     */
    fun checkFit(
        container: ContainerDimensions,
        wallThicknessMm: Double,
        box: BoxDimensions,
        clearance: PackingClearance = PackingClearance(),
        lock: OrientationLock = OrientationLock.NONE
    ): FitCheckResult {
        if (!container.isValid() || !box.isValid()) {
            return FitCheckResult(
                fits = false,
                maxCapacity = 0,
                bestOrientation = null,
                failedDimensions = emptyList()
            )
        }

        val (best, _) = calculateBestOrientation(
            container = container,
            wallThicknessMm = wallThicknessMm,
            box = box,
            clearance = clearance,
            lock = lock
        )

        val usableWidth = max(0.0, container.widthMm - 2.0 * wallThicknessMm - 2.0 * clearance.widthMm)
        val usableDepth = max(0.0, container.depthMm - 2.0 * wallThicknessMm - 2.0 * clearance.depthMm)
        val usableHeight = max(0.0, container.heightMm - 2.0 * wallThicknessMm - 2.0 * clearance.heightMm)

        if (best != null && best.capacity > 0) {
            return FitCheckResult(
                fits = true,
                maxCapacity = best.capacity,
                bestOrientation = best.orientation,
                failedDimensions = emptyList()
            )
        }

        // Box does not fit; show attempted dimension comparison for original orientation
        val failed = listOf(
            DimensionFitNote("Width", box.widthMm, usableWidth, box.widthMm <= usableWidth),
            DimensionFitNote("Depth", box.depthMm, usableDepth, box.depthMm <= usableDepth),
            DimensionFitNote("Height", box.heightMm, usableHeight, box.heightMm <= usableHeight)
        )

        return FitCheckResult(
            fits = false,
            maxCapacity = 0,
            bestOrientation = null,
            failedDimensions = failed
        )
    }

    /**
     * Batch calculation using ceiling division for required containers.
     */
    fun calculateBatch(requiredBoxes: Int, capacityPerContainer: Int): BatchResult {
        if (requiredBoxes <= 0 || capacityPerContainer <= 0) {
            return BatchResult(
                requiredBoxes = max(0, requiredBoxes),
                capacityPerContainer = max(0, capacityPerContainer),
                containersRequired = 0,
                totalCapacity = 0,
                unusedCapacity = 0,
                lastContainerBoxes = 0
            )
        }

        val containersRequired = ceil(requiredBoxes.toDouble() / capacityPerContainer.toDouble()).toInt()
        val totalCapacity = containersRequired * capacityPerContainer
        val unusedCapacity = totalCapacity - requiredBoxes
        val remainder = requiredBoxes % capacityPerContainer
        val lastContainerBoxes = if (remainder == 0) capacityPerContainer else remainder

        return BatchResult(
            requiredBoxes = requiredBoxes,
            capacityPerContainer = capacityPerContainer,
            containersRequired = containersRequired,
            totalCapacity = totalCapacity,
            unusedCapacity = unusedCapacity,
            lastContainerBoxes = lastContainerBoxes
        )
    }
}
