package com.liuli.hanzirandom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.liuli.hanzirandom.ui.theme.HanziColors

@Composable
fun TianZiGe(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val inset = 24.dp.toPx()
        val side = minOf(size.width, size.height) - inset * 2
        if (side <= 0f) return@Canvas

        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val color = HanziColors.Accent.copy(alpha = 0.18f)
        val stroke = 1.5.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 7.dp.toPx()))

        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(side, side),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(left + side / 2, top),
            end = Offset(left + side / 2, top + side),
            strokeWidth = stroke,
            pathEffect = dash
        )
        drawLine(
            color = color,
            start = Offset(left, top + side / 2),
            end = Offset(left + side, top + side / 2),
            strokeWidth = stroke,
            pathEffect = dash
        )
    }
}
