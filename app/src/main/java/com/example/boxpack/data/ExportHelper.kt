package com.example.boxpack.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.boxpack.model.BoxDimensions
import com.example.boxpack.model.ContainerDimensions
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationResult
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.util.Locale

object ExportHelper {

    fun generateTxt(
        container: ContainerDimensions,
        box: BoxDimensions,
        result: OrientationResult,
        unit: LengthUnit,
        decimals: Int = 2
    ): String {
        val contW = unit.formatValue(container.widthMm, decimals)
        val contD = unit.formatValue(container.depthMm, decimals)
        val contH = unit.formatValue(container.heightMm, decimals)

        val boxW = unit.formatValue(box.widthMm, decimals)
        val boxD = unit.formatValue(box.depthMm, decimals)
        val boxH = unit.formatValue(box.heightMm, decimals)

        val oriW = unit.formatValue(result.orientation.widthMm, decimals)
        val oriD = unit.formatValue(result.orientation.depthMm, decimals)
        val oriH = unit.formatValue(result.orientation.heightMm, decimals)

        val unusedW = unit.formatValue(result.remainingWidthMm, decimals)
        val unusedD = unit.formatValue(result.remainingDepthMm, decimals)
        val unusedH = unit.formatValue(result.remainingHeightMm, decimals)

        val unusedVol = LengthUnit.formatVolume(result.unusedVolumeMm3, unit, decimals)

        return """
            BOXPACK
            
            Container:
            $contW × $contD × $contH ${unit.symbol}
            
            Box:
            $boxW × $boxD × $boxH ${unit.symbol}
            
            Best Orientation:
            $oriW × $oriD × $oriH ${unit.symbol}
            
            Boxes Across:
            ${result.boxesAcross}
            
            Boxes Deep:
            ${result.boxesDeep}
            
            Layers:
            ${result.layers}
            
            Boxes Per Layer:
            ${result.boxesPerLayer}
            
            Total Capacity:
            ${result.capacity}
            
            Unused Width:
            $unusedW ${unit.symbol}
            
            Unused Depth:
            $unusedD ${unit.symbol}
            
            Unused Height:
            $unusedH ${unit.symbol}
            
            Unused Volume:
            $unusedVol
            
            Volume Utilization:
            ${String.format(Locale.US, "%.1f%%", result.utilizationPercent)}
        """.trimIndent()
    }

    fun generateCsv(
        container: ContainerDimensions,
        box: BoxDimensions,
        result: OrientationResult,
        unit: LengthUnit,
        decimals: Int = 2
    ): String {
        val header = "ContainerWidth,ContainerDepth,ContainerHeight,BoxWidth,BoxDepth,BoxHeight,Orientation,Across,Deep,Layers,Capacity,UnusedWidth,UnusedDepth,UnusedHeight,Utilization"

        val contW = unit.formatValue(container.widthMm, decimals)
        val contD = unit.formatValue(container.depthMm, decimals)
        val contH = unit.formatValue(container.heightMm, decimals)

        val boxW = unit.formatValue(box.widthMm, decimals)
        val boxD = unit.formatValue(box.depthMm, decimals)
        val boxH = unit.formatValue(box.heightMm, decimals)

        val ori = "${unit.formatValue(result.orientation.widthMm, decimals)}x${unit.formatValue(result.orientation.depthMm, decimals)}x${unit.formatValue(result.orientation.heightMm, decimals)}"

        val unusedW = unit.formatValue(result.remainingWidthMm, decimals)
        val unusedD = unit.formatValue(result.remainingDepthMm, decimals)
        val unusedH = unit.formatValue(result.remainingHeightMm, decimals)

        val util = String.format(Locale.US, "%.1f", result.utilizationPercent)

        val row = "$contW,$contD,$contH,$boxW,$boxD,$boxH,$ori,${result.boxesAcross},${result.boxesDeep},${result.layers},${result.capacity},$unusedW,$unusedD,$unusedH,$util"

        return "$header\n$row\n"
    }

    fun generateJson(
        container: ContainerDimensions,
        box: BoxDimensions,
        result: OrientationResult,
        unit: LengthUnit,
        decimals: Int = 2
    ): String {
        val root = JSONObject()

        val contObj = JSONObject()
        val unitSuffix = unit.symbol.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        contObj.put("width$unitSuffix", unit.fromMm(container.widthMm))
        contObj.put("depth$unitSuffix", unit.fromMm(container.depthMm))
        contObj.put("height$unitSuffix", unit.fromMm(container.heightMm))
        root.put("container", contObj)

        val boxObj = JSONObject()
        boxObj.put("width$unitSuffix", unit.fromMm(box.widthMm))
        boxObj.put("depth$unitSuffix", unit.fromMm(box.depthMm))
        boxObj.put("height$unitSuffix", unit.fromMm(box.heightMm))
        root.put("box", boxObj)

        val bestObj = JSONObject()
        bestObj.put("width$unitSuffix", unit.fromMm(result.orientation.widthMm))
        bestObj.put("depth$unitSuffix", unit.fromMm(result.orientation.depthMm))
        bestObj.put("height$unitSuffix", unit.fromMm(result.orientation.heightMm))
        root.put("bestOrientation", bestObj)

        val packObj = JSONObject()
        packObj.put("across", result.boxesAcross)
        packObj.put("deep", result.boxesDeep)
        packObj.put("layers", result.layers)
        packObj.put("boxesPerLayer", result.boxesPerLayer)
        packObj.put("capacity", result.capacity)
        packObj.put("unusedWidth$unitSuffix", unit.fromMm(result.remainingWidthMm))
        packObj.put("unusedDepth$unitSuffix", unit.fromMm(result.remainingDepthMm))
        packObj.put("unusedHeight$unitSuffix", unit.fromMm(result.remainingHeightMm))
        packObj.put("utilizationPercent", result.utilizationPercent)
        root.put("packing", packObj)

        return root.toString(2)
    }

    fun generateShareText(
        container: ContainerDimensions,
        box: BoxDimensions,
        result: OrientationResult,
        unit: LengthUnit,
        decimals: Int = 1
    ): String {
        val contW = unit.formatValue(container.widthMm, decimals)
        val contD = unit.formatValue(container.depthMm, decimals)
        val contH = unit.formatValue(container.heightMm, decimals)

        val boxW = unit.formatValue(box.widthMm, decimals)
        val boxD = unit.formatValue(box.depthMm, decimals)
        val boxH = unit.formatValue(box.heightMm, decimals)

        val oriW = unit.formatValue(result.orientation.widthMm, decimals)
        val oriD = unit.formatValue(result.orientation.depthMm, decimals)
        val oriH = unit.formatValue(result.orientation.heightMm, decimals)

        return """
            BoxPack
            
            Container:
            $contW × $contD × $contH ${unit.symbol}
            
            Box:
            $boxW × $boxD × $boxH ${unit.symbol}
            
            Capacity:
            ${result.capacity} boxes
            
            Arrangement:
            ${result.boxesAcross} × ${result.boxesDeep} × ${result.layers}
            
            Best Orientation:
            $oriW × $oriD × $oriH ${unit.symbol}
            
            Utilization:
            ${String.format(Locale.US, "%.1f%%", result.utilizationPercent)}
        """.trimIndent()
    }

    fun shareResult(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BoxPack Calculation")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share BoxPack Result"))
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
