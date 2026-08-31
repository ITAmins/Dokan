package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PdfExporter
import com.example.ui.theme.*

data class ChartPoint(
    val label: String,
    val value: Double
)

data class SliceData(
    val name: String,
    val value: Double,
    val color: Color
)

@Composable
fun DokanBezierTrendChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = DokanPurplePrimary,
    fillStartColor: Color = DokanPurpleLight.copy(alpha = 0.35f),
    fillEndColor: Color = Color.Transparent
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("পর্যাপ্ত ডাটা নেই", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingHorizontal = 16.dp.toPx()
        val paddingVertical = 20.dp.toPx()

        val drawWidth = width - (paddingHorizontal * 2)
        val drawHeight = height - (paddingVertical * 2)

        val maxValue = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(100.0)
        val minValue = 0.0

        val stepX = if (points.size > 1) drawWidth / (points.size - 1) else drawWidth

        val coordinates = points.mapIndexed { index, point ->
            val x = paddingHorizontal + (index * stepX)
            val normalizedY = ((point.value - minValue) / (maxValue - minValue)).toFloat()
            val y = paddingVertical + (drawHeight * (1f - normalizedY))
            Offset(x, y)
        }

        // Draw smooth bezier path
        val strokePath = Path().apply {
            if (coordinates.isNotEmpty()) {
                moveTo(coordinates.first().x, coordinates.first().y)
                for (i in 0 until coordinates.size - 1) {
                    val p0 = coordinates[i]
                    val p1 = coordinates[i + 1]
                    val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                    val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                    cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                }
            }
        }

        // Gradient fill path
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(coordinates.last().x, height)
            lineTo(coordinates.first().x, height)
            close()
        }

        // Draw gradient area below curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillStartColor, fillEndColor),
                startY = paddingVertical,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw circle points
        coordinates.forEach { pt ->
            drawCircle(
                color = Color.White,
                radius = 4.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun DokanDonutChart(
    slices: List<SliceData>,
    modifier: Modifier = Modifier,
    holeRadiusPercent: Float = 0.60f
) {
    val total = slices.sumOf { it.value }

    if (total <= 0.0 || slices.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("কোনো ক্রয়ের ডাটা নেই", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Chart Canvas
        Canvas(modifier = Modifier.size(130.dp)) {
            val strokeWidth = (size.minDimension / 2f) * (1f - holeRadiusPercent)
            var startAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = ((slice.value / total) * 360f).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweepAngle
            }
        }

        // Legend list
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            slices.take(4).forEach { slice ->
                val percentage = (slice.value / total) * 100
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = slice.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format("%.0f", percentage)}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
