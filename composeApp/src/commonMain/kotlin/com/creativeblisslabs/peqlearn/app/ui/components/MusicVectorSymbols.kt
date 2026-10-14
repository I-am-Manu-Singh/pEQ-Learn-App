package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform

object MusicVectorSymbols {
    private fun DrawScope.drawTrebleClefSymbol(color: Color, topLeft: Offset, size: Size) {
        val path = Path().apply {
            moveTo(topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.95f)
            cubicTo(topLeft.x + size.width * 0.20f, topLeft.y + size.height * 0.90f, topLeft.x + size.width * 0.10f, topLeft.y + size.height * 0.70f, topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.55f)
            cubicTo(topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.45f, topLeft.x + size.width * 0.55f, topLeft.y + size.height * 0.35f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.20f)
            cubicTo(topLeft.x + size.width * 0.35f, topLeft.y + size.height * 0.05f, topLeft.x + size.width * 0.65f, topLeft.y + size.height * 0.02f, topLeft.x + size.width * 0.70f, topLeft.y + size.height * 0.25f)
            cubicTo(topLeft.x + size.width * 0.75f, topLeft.y + size.height * 0.50f, topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.70f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.95f)
            close()
        }
        drawPath(path, color = color)
    }

    private fun DrawScope.drawBassClefSymbol(color: Color, topLeft: Offset, size: Size) {
        val path = Path().apply {
            moveTo(topLeft.x + size.width * 0.20f, topLeft.y + size.height * 0.40f)
            cubicTo(topLeft.x + size.width * 0.05f, topLeft.y + size.height * 0.20f, topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.05f, topLeft.x + size.width * 0.55f, topLeft.y + size.height * 0.25f)
            cubicTo(topLeft.x + size.width * 0.75f, topLeft.y + size.height * 0.45f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.85f, topLeft.x + size.width * 0.20f, topLeft.y + size.height * 0.90f)
            lineTo(topLeft.x + size.width * 0.70f, topLeft.y + size.height * 0.90f)
            lineTo(topLeft.x + size.width * 0.70f, topLeft.y + size.height * 0.75f)
            cubicTo(topLeft.x + size.width * 0.50f, topLeft.y + size.height * 0.75f, topLeft.x + size.width * 0.40f, topLeft.y + size.height * 0.65f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.55f)
            cubicTo(topLeft.x + size.width * 0.55f, topLeft.y + size.height * 0.45f, topLeft.x + size.width * 0.50f, topLeft.y + size.height * 0.30f, topLeft.x + size.width * 0.38f, topLeft.y + size.height * 0.30f)
            cubicTo(topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.30f, topLeft.x + size.width * 0.25f, topLeft.y + size.height * 0.35f, topLeft.x + size.width * 0.28f, topLeft.y + size.height * 0.42f)
            close()
        }
        drawPath(path, color = color)
        drawCircle(color, radius = size.width * 0.08f, center = Offset(topLeft.x + size.width * 0.85f, topLeft.y + size.height * 0.35f))
        drawCircle(color, radius = size.width * 0.08f, center = Offset(topLeft.x + size.width * 0.85f, topLeft.y + size.height * 0.65f))
    }

    fun DrawScope.drawSharp(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(4f, 2f); lineTo(3f, 8f)
            moveTo(12f, 2f); lineTo(11f, 8f)
            moveTo(2f, 10f); lineTo(14f, 8f)
            moveTo(2f, 20f); lineTo(14f, 18f)
            moveTo(5f, 22f); lineTo(4f, 28f)
            moveTo(13f, 22f); lineTo(12f, 28f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.5f))
    }

    fun DrawScope.drawFlat(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(2f, 2f)
            lineTo(2f, 28f)
            cubicTo(2f, 28f, 8f, 25f, 8f, 20f)
            cubicTo(8f, 15f, 2f, 18f, 2f, 18f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.5f))
    }

    fun DrawScope.drawNatural(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(3f, 5f); lineTo(3f, 25f)
            moveTo(7f, 5f); lineTo(7f, 25f)
            moveTo(3f, 18f); lineTo(7f, 15f)
            moveTo(3f, 15f); lineTo(7f, 12f)
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
    }

    fun DrawScope.drawNoteHead(color: Color, size: Size, isHollow: Boolean) {
        withTransform({
            rotate(-20f, pivot = Offset(size.width / 2f, size.height / 2f))
        }) {
            if (isHollow) {
                drawOval(color, topLeft = Offset.Zero, size = size, style = Stroke(width = 2f))
            } else {
                drawOval(color, topLeft = Offset.Zero, size = size, style = Fill)
            }
        }
    }

    fun DrawScope.drawRestWhole(color: Color, size: Size) {
        drawRect(color, topLeft = Offset(size.width * 0.1f, 0f), size = Size(size.width * 0.8f, size.height * 0.25f))
    }

    fun DrawScope.drawRestHalf(color: Color, size: Size) {
        drawRect(color, topLeft = Offset(size.width * 0.1f, size.height * 0.25f), size = Size(size.width * 0.8f, size.height * 0.25f))
    }

    fun DrawScope.drawRestQuarter(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(4f, 5f); lineTo(11f, 12f); lineTo(5f, 18f); lineTo(10f, 23f)
            cubicTo(10f, 23f, 11f, 25f, 9f, 28f)
            cubicTo(7f, 31f, 4f, 30f, 4f, 30f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.5f))
    }
    
    fun DrawScope.drawRestEighth(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(7f, 10f)
            cubicTo(12f, 10f, 12f, 15f, 8f, 25f)
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
    }

    fun DrawScope.drawSegno(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.8f)
            cubicTo(size.width * 0.1f, size.height * 0.5f, size.width * 0.9f, size.height * 0.5f, size.width * 0.8f, size.height * 0.2f)
            moveTo(size.width * 0.5f, size.height * 0.1f)
            lineTo(size.width * 0.5f, size.height * 0.9f)
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
        drawCircle(color, radius = size.width * 0.1f, center = Offset(size.width * 0.3f, size.height * 0.5f))
        drawCircle(color, radius = size.width * 0.1f, center = Offset(size.width * 0.7f, size.height * 0.5f))
    }

    fun DrawScope.drawCoda(color: Color, size: Size) {
        drawCircle(color, radius = size.width * 0.4f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = 2f))
        drawLine(color, Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height), strokeWidth = 2f)
        drawLine(color, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = 2f)
    }

    fun DrawScope.drawEighthFlag(color: Color, stemX: Float, stemY: Float, isUp: Boolean) {
        val path = Path().apply {
            moveTo(stemX, stemY)
            if (isUp) {
                cubicTo(stemX, stemY + 5f, stemX + 10f, stemY + 10f, stemX + 8f, stemY + 20f)
            } else {
                cubicTo(stemX, stemY - 5f, stemX + 10f, stemY - 10f, stemX + 8f, stemY - 20f)
            }
        }
        drawPath(path, color = color, style = Stroke(width = 1.5f))
    }
}
