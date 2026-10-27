package com.example.boxpack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationResult
import kotlin.math.max
import kotlin.math.min

enum class PackingViewType {
    TOP,
    FRONT,
    SIDE
}

@Composable
fun PackingViewDiagram(
    viewType: PackingViewType,
    result: OrientationResult,
    containerDimX: Double,
    containerDimY: Double,
    boxDimX: Double,
    boxDimY: Double,
    countX: Int,
    countY: Int,
    remainingX: Double,
    remainingY: Double,
    labelX: String,
    labelY: String,
    unit: LengthUnit,
    decimals: Int = 1,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(260.dp)
) {
    val totalInView = countX.toLong() * countY.toLong()
    val maxVisualBoxes = 400
    val isCapped = totalInView > maxVisualBoxes

    val containerBorderColor = Color(0xFF2563EB)
    val boxFillColor = Color(0xFFFDE68A)
    val boxBorderColor = Color(0xFFD97706)
    val remainderColor = Color(0x33EF4444)
    val dimensionTextColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = modifier) {
            val canvasW = size.width
            val canvasH = size.height
            val padX = 55f
            val padY = 45f

            val availW = canvasW - padX * 2
            val availH = canvasH - padY * 2

            if (containerDimX <= 0.0 || containerDimY <= 0.0 || availW <= 0f || availH <= 0f) {
                return@Canvas
            }

            // Maintain exact uniform aspect ratio
            val scale = min(availW / containerDimX.toFloat(), availH / containerDimY.toFloat())
            val drawContainerW = (containerDimX * scale).toFloat()
            val drawContainerH = (containerDimY * scale).toFloat()

            val originX = padX + (availW - drawContainerW) / 2f
            val originY = padY + (availH - drawContainerH) / 2f

            // 1. Draw Container Boundary Background
            drawRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(originX, originY),
                size = Size(drawContainerW, drawContainerH),
                style = Fill
            )

            // 2. Draw boxes grid (or representative capped grid)
            val drawCountX = if (isCapped) min(countX, 20) else countX
            val drawCountY = if (isCapped) min(countY, 20) else countY

            val drawBoxW = (boxDimX * scale).toFloat()
            val drawBoxH = (boxDimY * scale).toFloat()

            val totalPackedW = drawBoxW * countX
            val totalPackedH = drawBoxH * countY

            // Draw remaining unused area strips
            if (remainingX > 0.1) {
                val remDrawW = (remainingX * scale).toFloat()
                drawRect(
                    color = remainderColor,
                    topLeft = Offset(originX + totalPackedW, originY),
                    size = Size(remDrawW, drawContainerH),
                    style = Fill
                )
            }

            if (remainingY > 0.1) {
                val remDrawH = (remainingY * scale).toFloat()
                drawRect(
                    color = remainderColor,
                    topLeft = Offset(originX, originY + totalPackedH),
                    size = Size(drawContainerW, remDrawH),
                    style = Fill
                )
            }

            // Draw individual boxes
            val paint = android.graphics.Paint().apply {
                color = boxBorderColor.hashCode()
                textSize = 22f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val showNumbers = totalInView <= 36 && viewType == PackingViewType.TOP

            for (row in 0 until drawCountY) {
                for (col in 0 until drawCountX) {
                    val boxLeft = originX + col * drawBoxW
                    val boxTop = originY + row * drawBoxH

                    drawRect(
                        color = boxFillColor,
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(drawBoxW, drawBoxH),
                        style = Fill
                    )

                    drawRect(
                        color = boxBorderColor,
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(drawBoxW, drawBoxH),
                        style = Stroke(width = 1.5f)
                    )

                    if (showNumbers) {
                        val num = row * countX + col + 1
                        drawContext.canvas.nativeCanvas.drawText(
                            "$num",
                            boxLeft + drawBoxW / 2f,
                            boxTop + drawBoxH / 2f + 8f,
                            paint
                        )
                    }
                }
            }

            // 3. Draw Container Outer Stroke
            drawRect(
                color = containerBorderColor,
                topLeft = Offset(originX, originY),
                size = Size(drawContainerW, drawContainerH),
                style = Stroke(width = 3.5f)
            )

            // 4. Dimension markings
            val dimPaint = android.graphics.Paint().apply {
                color = dimensionTextColor.hashCode()
                textSize = 24f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // Top dimension (Width / Length)
            val topText = "$labelX: ${unit.formatValue(containerDimX, decimals)} ${unit.symbol}"
            drawContext.canvas.nativeCanvas.drawText(
                topText,
                originX + drawContainerW / 2f,
                originY - 12f,
                dimPaint
            )

            // Left dimension (Depth / Height)
            dimPaint.textAlign = android.graphics.Paint.Align.RIGHT
            val leftText = "$labelY: ${unit.formatValue(containerDimY, decimals)} ${unit.symbol}"
            drawContext.canvas.nativeCanvas.drawText(
                leftText,
                originX - 12f,
                originY + drawContainerH / 2f + 8f,
                dimPaint
            )

            // Remaining annotations if applicable
            if (remainingX > 0.1) {
                val remText = "+${unit.formatValue(remainingX, decimals)} ${unit.symbol} free"
                dimPaint.textSize = 20f
                dimPaint.color = Color(0xFFDC2626).hashCode()
                drawContext.canvas.nativeCanvas.drawText(
                    remText,
                    originX + drawContainerW + 8f,
                    originY + drawContainerH / 2f,
                    dimPaint.apply { textAlign = android.graphics.Paint.Align.LEFT }
                )
            }
        }

        // Capped warning if exceeding 400 boxes
        if (isCapped) {
            val hiddenCount = totalInView - (min(countX, 20) * min(countY, 20))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Showing representative grid (${min(countX, 20)}×${min(countY, 20)}) • +$hiddenCount additional boxes in view",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Arrangement description below diagram
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$countX $labelX × $countY $labelY = $totalInView boxes",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (remainingX > 0.1 || remainingY > 0.1) {
                Text(
                    text = " • Space remaining",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
