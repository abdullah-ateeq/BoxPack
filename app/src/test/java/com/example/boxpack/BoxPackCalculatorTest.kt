package com.example.boxpack

import com.example.boxpack.calculator.BoxPackCalculator
import com.example.boxpack.model.BoxDimensions
import com.example.boxpack.model.ContainerDimensions
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationLock
import com.example.boxpack.model.PackingClearance
import com.example.boxpack.model.PackingGaps
import com.example.boxpack.model.StackingConstraint
import com.example.boxpack.model.StackingMode
import com.example.boxpack.model.WeightConstraint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxPackCalculatorTest {

    @Test
    fun testBasicPacking() {
        // Container: 120 × 80 × 60 cm (in mm: 1200 × 800 × 600)
        // Box: 30 × 20 × 15 cm (in mm: 300 × 200 × 150)
        val container = ContainerDimensions(1200.0, 800.0, 600.0)
        val box = BoxDimensions(300.0, 200.0, 150.0)

        val (best, orientations) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box
        )

        assertNotNull(best)
        assertEquals(64, best!!.capacity)
        assertEquals(4, best.boxesAcross)
        assertEquals(4, best.boxesDeep)
        assertEquals(4, best.layers)
        assertEquals(16, best.boxesPerLayer)
        assertEquals(0.0, best.remainingWidthMm, 0.001)
        assertEquals(0.0, best.remainingDepthMm, 0.001)
        assertEquals(0.0, best.remainingHeightMm, 0.001)
        assertEquals(100.0, best.utilizationPercent, 0.001)
        assertTrue(orientations.isNotEmpty())
    }

    @Test
    fun testNonDivisiblePacking() {
        // Container: 100 × 100 × 100 cm (1000 × 1000 × 1000 mm)
        // Box: 60 × 60 × 60 cm (600 × 600 × 600 mm)
        val container = ContainerDimensions(1000.0, 1000.0, 1000.0)
        val box = BoxDimensions(600.0, 600.0, 600.0)

        val (best, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box
        )

        assertNotNull(best)
        // 1000 / 600 = 1.666 -> floor is 1. Capacity = 1 * 1 * 1 = 1
        assertEquals(1, best!!.capacity)
        assertEquals(1, best.boxesAcross)
        assertEquals(1, best.boxesDeep)
        assertEquals(1, best.layers)
        // Theoretical ratio is 1,000,000,000 / 216,000,000 ≈ 4.629
        assertEquals(4.63, best.theoreticalVolumeRatio, 0.01)
    }

    @Test
    fun testOrientationSelection() {
        // Container: 100 × 80 × 50 cm
        // Box: 60 × 40 × 20 cm
        val container = ContainerDimensions(1000.0, 800.0, 500.0)
        val box = BoxDimensions(600.0, 400.0, 200.0)

        val (best, results) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box
        )

        assertNotNull(best)
        // Check all unique orientations generated
        val generated = BoxPackCalculator.generateOrientations(box)
        assertEquals(6, generated.size)

        // Make sure best is the one with highest capacity
        val maxCap = results.maxOf { it.capacity }
        assertEquals(maxCap, best!!.capacity)
        assertEquals(5, best.capacity) // e.g., 200 × 600 × 400 gives 5 × 1 × 1 = 5 boxes
    }

    @Test
    fun testRotationLockFixed() {
        // Container: 100 × 80 × 50 cm
        // Box: 60 × 40 × 20 cm
        val box = BoxDimensions(600.0, 400.0, 200.0)

        val orientationsFixed = BoxPackCalculator.generateOrientations(box, lock = OrientationLock.FIXED)
        assertEquals(1, orientationsFixed.size)
        assertEquals(600.0, orientationsFixed[0].widthMm, 0.001)
        assertEquals(400.0, orientationsFixed[0].depthMm, 0.001)
        assertEquals(200.0, orientationsFixed[0].heightMm, 0.001)
    }

    @Test
    fun testRotationLockUpright() {
        val box = BoxDimensions(600.0, 400.0, 200.0)
        val orientationsUpright = BoxPackCalculator.generateOrientations(box, lock = OrientationLock.UPRIGHT_ONLY)
        // Height (200.0) must be fixed to Z
        assertEquals(2, orientationsUpright.size)
        assertTrue(orientationsUpright.all { it.heightMm == 200.0 })
    }

    @Test
    fun testClearanceAndBoxGaps() {
        // Container: 100 × 100 × 100 cm (1000 mm)
        // Box: 25 × 25 × 25 cm (250 mm)
        // Gap: 1 cm (10 mm)
        // Effective box: 260 × 260 × 260 mm
        // floor(1000 / 260) = 3 per dimension -> 3 × 3 × 3 = 27
        val container = ContainerDimensions(1000.0, 1000.0, 1000.0)
        val box = BoxDimensions(250.0, 250.0, 250.0)
        val gaps = PackingGaps(10.0, 10.0, 10.0)

        val (best, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box,
            gaps = gaps
        )

        assertNotNull(best)
        assertEquals(27, best!!.capacity)
        assertEquals(3, best.boxesAcross)
        assertEquals(3, best.boxesDeep)
        assertEquals(3, best.layers)
        // Occupied volume must use ACTUAL box dimensions (250³ × 27 = 421,875,000 mm³)
        val expectedActualOccupied = 27.0 * (250.0 * 250.0 * 250.0)
        assertEquals(expectedActualOccupied, best.occupiedVolumeMm3, 0.001)
    }

    @Test
    fun testWallThickness() {
        // Container: 100 × 100 × 100 cm (1000 mm)
        // Wall thickness: 5 cm (50 mm)
        // Usable dimensions: 1000 - 2*50 = 900 mm
        // Box: 30 × 30 × 30 cm (300 mm)
        // 900 / 300 = 3 across each dimension -> 3 × 3 × 3 = 27 boxes
        val container = ContainerDimensions(1000.0, 1000.0, 1000.0)
        val box = BoxDimensions(300.0, 300.0, 300.0)

        val (best, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 50.0,
            box = box
        )

        assertNotNull(best)
        assertEquals(27, best!!.capacity)
        assertEquals(3, best.boxesAcross)
        assertEquals(3, best.boxesDeep)
        assertEquals(3, best.layers)
    }

    @Test
    fun testBatchCalculation() {
        // Required: 250 boxes, Capacity per container: 64
        // Containers: ceil(250 / 64) = 4
        // Total capacity: 256, Unused: 6, Last container: 250 % 64 = 58
        val batch = BoxPackCalculator.calculateBatch(250, 64)
        assertEquals(4, batch.containersRequired)
        assertEquals(256, batch.totalCapacity)
        assertEquals(6, batch.unusedCapacity)
        assertEquals(58, batch.lastContainerBoxes)
    }

    @Test
    fun testWeightLimiting() {
        // Capacity: 64 geometric
        // Box weight: 2.5 kg, Max load: 100 kg
        // Weight capacity: floor(100 / 2.5) = 40
        // Final capacity: 40
        val container = ContainerDimensions(1200.0, 800.0, 600.0)
        val box = BoxDimensions(300.0, 200.0, 150.0)
        val weight = WeightConstraint(
            isEnabled = true,
            boxWeightKg = 2.5,
            hasMaxLoad = true,
            maxLoadKg = 100.0
        )

        val (best, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box,
            weightConstraint = weight
        )

        assertNotNull(best)
        assertEquals(40, best!!.capacity)
        assertTrue(best.isLimitingByWeight)
        assertEquals(40, best.weightCapacity)
        assertEquals(100.0, best.totalWeightKg, 0.001)
    }

    @Test
    fun testBoxTooLarge() {
        // Container: 20 × 20 × 20 cm (200 mm)
        // Box: 30 × 10 × 10 cm (300 × 100 × 100 mm)
        val container = ContainerDimensions(200.0, 200.0, 200.0)
        val box = BoxDimensions(300.0, 100.0, 100.0)

        val fit = BoxPackCalculator.checkFit(container, 0.0, box)
        assertFalse(fit.fits)
        assertEquals(0, fit.maxCapacity)
        assertTrue(fit.failedDimensions.isNotEmpty())
    }

    @Test
    fun testZeroAndNegativeValidation() {
        val containerZero = ContainerDimensions(0.0, 800.0, 600.0)
        assertFalse(containerZero.isValid())

        val boxNegative = BoxDimensions(300.0, 200.0, -100.0)
        assertFalse(boxNegative.isValid())
    }

    @Test
    fun testLargeCapacitySafety() {
        // Massive warehouse container with very small items
        val container = ContainerDimensions(50_000.0, 50_000.0, 10_000.0)
        val box = BoxDimensions(100.0, 100.0, 100.0)

        val (best, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box
        )

        assertNotNull(best)
        // 500 across, 500 deep, 100 layers -> 500 * 500 * 100 = 25,000,000 boxes
        assertEquals(500, best!!.boxesAcross)
        assertEquals(500, best.boxesDeep)
        assertEquals(100, best.layers)
        assertEquals(25_000_000, best.capacity)
    }

    @Test
    fun testStackingModes() {
        val container = ContainerDimensions(1200.0, 800.0, 600.0)
        val box = BoxDimensions(300.0, 200.0, 150.0)

        // Disabled stacking with FIXED original orientation -> layers must be 1, capacity = 4 * 4 * 1 = 16
        val (bestDisabledFixed, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box,
            lock = OrientationLock.FIXED,
            stacking = StackingConstraint(mode = StackingMode.DISABLED)
        )
        assertNotNull(bestDisabledFixed)
        assertEquals(1, bestDisabledFixed!!.layers)
        assertEquals(16, bestDisabledFixed.capacity)

        // Limited stacking to 2 layers with FIXED orientation -> 4 * 4 * 2 = 32
        val (bestLimited, _) = BoxPackCalculator.calculateBestOrientation(
            container = container,
            wallThicknessMm = 0.0,
            box = box,
            lock = OrientationLock.FIXED,
            stacking = StackingConstraint(mode = StackingMode.LIMITED, maxLayers = 2)
        )
        assertNotNull(bestLimited)
        assertEquals(2, bestLimited!!.layers)
        assertEquals(32, bestLimited.capacity)
    }
}
