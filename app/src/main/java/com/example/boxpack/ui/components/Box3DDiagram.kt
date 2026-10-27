package com.example.boxpack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.boxpack.model.LengthUnit
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders a proportional 3D isometric/axonometric diagram of a rectangular container or box
 * using Compose Canvas with dimension lines and clear labels.
 */
@Composable
fun Box3DDiagram(
    widthMm: Double,
    depthMm: Double,
    heightMm: Double,
    unit: LengthUnit,
    decimals: Int = 1,
    title: String = "CONTAINER",
    isBox: Boolean = false,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val strokeColor = if (isBox) Color(0xFFB45309) else Color(0xFF2563EB)
    val frontFill = if (isBox) Color(0xFFFDE68A) else Color(0xFFBFDBFE)
    val topFill = if (isBox) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
    val sideFill = if (isBox) Color(0xFFF59E0B).copy(alpha = 0.85f) else Color(0xFF93C5FD)

    Canvas(modifier = modifier) {
        if (widthMm <= 0.0 || depthMm <= 0.0 || heightMm <= 0.0) {
            drawInvalidPlaceholder(onSurfaceColor, "Enter valid dimensions")
            return@Canvas
        }

        val canvasW = size.width
        val canvasH = size.height

        // Project 3D box onto 2D canvas:
        // Front face: (0, 0) to (W, H)
        // Depth projection at 30 degrees: dx = D * cos(30), dy = -D * sin(30)
        val angleRad = Math.toRadians(32.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        // Normalize dimensions so the whole projected box fits inside canvas with padding
        val padding = 45f
        val availW = canvasW - padding * 2
        val availH = canvasH - padding * 2

        val maxDim = max(widthMm, max(depthMm, heightMm)).toFloat()
        // Compute bounding box of projected box
        val rawW = (widthMm / maxDim).toFloat()
        val rawD = (depthMm / maxDim).toFloat() * 0.75f // slightly foreshorten depth
        val rawH = (heightMm / maxDim).toFloat()

        val projWidth = rawW + rawD * cosA
        val projHeight = rawH + rawD * sinA

        val scale = min(availW / projWidth, availH / projHeight)

        val w = rawW * scale
        val d = rawD * scale
        val h = rawH * scale

        val dx = d * cosA
        val dy = d * sinA

        // Origin (bottom-left of front face)
        val originX = (canvasW - (w + dx)) / 2f
        val originY = (canvasH + (h - dy)) / 2f

        // Vertices:
        // Front face:
        val fBL = Offset(originX, originY)
        val fBR = Offset(originX + w, originY)
        val fTR = Offset(originX + w, originY - h)
        val fTL = Offset(originX, originY - h)

        // Back face (projected):
        val bTL = Offset(fTL.x + dx, fTL.y - dy)
        val bTR = Offset(fTR.x + dx, fTR.y - dy)
        val bBR = Offset(fBR.x + dx, fBR.y - dy)

        // Draw Top face
        val topPath = Path().apply {
            moveTo(fTL.x, fTL.y)
            lineTo(bTL.x, bTL.y)
            lineTo(bTR.x, bTR.y)
            lineTo(fTR.x, fTR.y)
            close()
        }
        drawPath(topPath, topFill, style = Fill)
        drawPath(topPath, strokeColor, style = Stroke(width = 2.5f))

        // Draw Right Side face
        val sidePath = Path().apply {
            moveTo(fTR.x, fTR.y)
            lineTo(bTR.x, bTR.y)
            lineTo(bBR.x, bBR.y)
            lineTo(fBR.x, fBR.y)
            close()
        }
        drawPath(sidePath, sideFill, style = Fill)
        drawPath(sidePath, strokeColor, style = Stroke(width = 2.5f))

        // Draw Front face
        val frontPath = Path().apply {
            moveTo(fBL.x, fBL.y)
            lineTo(fBR.x, fBR.y)
            lineTo(fTR.x, fTR.y)
            lineTo(fTL.x, fTL.y)
            close()
        }
        drawPath(frontPath, frontFill, style = Fill)
        drawPath(frontPath, strokeColor, style = Stroke(width = 2.5f))

        // Dimension lines and labels
        val paint = android.graphics.Paint().apply {
            color = onSurfaceColor.hashCode()
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Width label (below front face)
        val widthStr = "${unit.formatValue(widthMm, decimals)} ${unit.symbol}"
        drawDimensionLine(
            start = Offset(fBL.x, fBL.y + 14f),
            end = Offset(fBR.x, fBR.y + 14f),
            color = strokeColor
        )
        drawContext.canvas.nativeCanvas.drawText(
            widthStr,
            (fBL.x + fBR.x) / 2f,
            fBL.y + 36f,
            paint
        )

        // Height label (left of front face)
        val heightStr = "${unit.formatValue(heightMm, decimals)} ${unit.symbol}"
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        drawDimensionLine(
            start = Offset(fBL.x - 14f, fBL.y),
            end = Offset(fTL.x - 14f, fTL.y),
            color = strokeColor
        )
        drawContext.canvas.nativeCanvas.drawText(
            heightStr,
            fBL.x - 20f,
            (fBL.y + fTL.y) / 2f + 10f,
            paint
        )

        // Depth label (along side bottom edge)
        val depthStr = "${unit.formatValue(depthMm, decimals)} ${unit.symbol}"
        paint.textAlign = android.graphics.Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText(
            depthStr,
            (fBR.x + bBR.x) / 2f + 10f,
            (fBR.y + bBR.y) / 2f + 18f,
            paint
        )

        // Center title inside front face if there's room
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.textSize = 24f
        paint.color = strokeColor.hashCode()
        drawContext.canvas.nativeCanvas.drawText(
            title,
            (fBL.x + fBR.x) / 2f,
            (fBL.y + fTL.y) / 2f + 8f,
            paint
        )
    }
}

private fun DrawScope.drawDimensionLine(start: Offset, end: Offset, color: Color) {
    drawLine(color = color, start = start, end = end, strokeWidth = 2f)
    // Draw tick marks at ends
    val tickLength = 6f
    if (start.y == end.y) {
        // Horizontal
        drawLine(color = color, start = Offset(start.x, start.y - tickLength), end = Offset(start.x, start.y + tickLength), strokeWidth = 2f)
        drawLine(color = color, start = Offset(end.x, end.y - tickLength), end = Offset(end.x, end.y + tickLength), strokeWidth = 2f)
    } else {
        // Vertical
        drawLine(color = color, start = Offset(start.x - tickLength, start.y), end = Offset(start.x + tickLength, start.y), strokeWidth = 2f)
        drawLine(color = color, start = Offset(end.x - tickLength, end.y), end = Offset(end.x + tickLength, end.y), strokeWidth = 2f)
    }
}

private fun DrawScope.drawInvalidPlaceholder(color: Color, message: String) {
    val paint = android.graphics.Paint().apply {
        this.color = color.copy(alpha = 0.5f).hashCode()
        textSize = 30f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(
        message,
        size.width / 2f,
        size.height / 2f,
        paint
    )
}
