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

    fun DrawScope.drawTrebleClef(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(22.5f, 75.4f)
            cubicTo(19.7f, 75.4f, 17.4f, 73.2f, 17.4f, 70.4f)
            cubicTo(17.4f, 66.3f, 21.5f, 63.5f, 25.5f, 63.5f)
            cubicTo(29.5f, 63.5f, 31.6f, 66.6f, 31.6f, 69.6f)
            cubicTo(31.6f, 72.6f, 28.7f, 75.4f, 22.5f, 75.4f)
            cubicTo(16.4f, 75.4f, 12.4f, 70.3f, 12.4f, 63.2f)
            cubicTo(12.4f, 53f, 20.5f, 44.9f, 24.6f, 34.8f)
            cubicTo(26.6f, 29.7f, 27.6f, 24.6f, 25.6f, 21.6f)
            cubicTo(24.6f, 19.6f, 22.5f, 18.5f, 20.5f, 20.5f)
            cubicTo(16.4f, 23.6f, 16.4f, 30.7f, 18.5f, 35.7f)
            cubicTo(20.5f, 40.8f, 23.6f, 45.9f, 23.6f, 50.9f)
            cubicTo(23.6f, 61.1f, 15.5f, 67.1f, 11.4f, 67.1f)
            cubicTo(8.3f, 67.1f, 5.3f, 65.1f, 5.3f, 61f)
            cubicTo(5.3f, 55.9f, 9.4f, 52.9f, 13.4f, 52.9f)
            cubicTo(17.4f, 52.9f, 19.5f, 56f, 19.5f, 59f)
            cubicTo(19.5f, 62.1f, 17.5f, 64.1f, 13.4f, 64.1f)
        }
        drawPath(path, color = color)
    }

    fun DrawScope.drawBassClef(color: Color, size: Size) {
        val path = Path().apply {
            moveTo(5.1f, 10.2f)
            cubicTo(5.1f, 5.1f, 9.2f, 1.1f, 14.2f, 1.1f)
            cubicTo(20.3f, 1.1f, 25.4f, 6.2f, 25.4f, 12.3f)
            cubicTo(25.4f, 20.4f, 18.3f, 27.5f, 10.2f, 31.6f)
            cubicTo(8.2f, 32.6f, 7.1f, 34.6f, 7.1f, 36.6f)
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
        drawCircle(color, radius = 2.5f, center = Offset(28.4f, 5.1f))
        drawCircle(color, radius = 2.5f, center = Offset(28.4f, 15.2f))
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
