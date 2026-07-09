package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import kotlin.math.cos
import kotlin.math.sin

class VerticalSpeedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseInstrumentView(context, attrs) {

    var verticalSpeed: Float = 0f
        set(value) {
            field = value.coerceIn(-10f, 10f)
            invalidate()
        }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    override fun drawInstrument(canvas: Canvas, size: Float) {
        drawScale(canvas, size)
        drawLabels(canvas, size)
        drawNeedle(canvas, size)
    }

    private fun drawScale(canvas: Canvas, size: Float) {
        val cx = size / 2f
        val cy = size / 2f
        val radius = size * 0.36f

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.01f

        for (i in -10..10) {
            val angle = valueToAngle(i.toFloat())

            val inner = if (i % 2 == 0) radius * 0.75f else radius * 0.85f
            val outer = radius

            val sx = cx + inner * cos(angle)
            val sy = cy + inner * sin(angle)

            val ex = cx + outer * cos(angle)
            val ey = cy + outer * sin(angle)

            canvas.drawLine(sx, sy, ex, ey, paint)
        }
    }
    private fun drawLabels(canvas: Canvas, size: Float) {
        val cx = size / 2f
        val cy = size / 2f
        val radius = size * 0.46f

        textPaint.textSize = size * 0.07f

        for (i in -10..10 step 2) {
            if (i == 0) continue

            val angle = valueToAngle(i.toFloat())
            val tx = cx + radius * cos(angle)
            val ty = cy + radius * sin(angle)
            val offset = size * 0.015f
            val txAdj = tx
            val tyAdj = ty + offset

            canvas.drawText(i.toString(), txAdj, tyAdj, textPaint)
        }
        textPaint.textSize = size * 0.06f
        canvas.drawText("m/s", cx, cy + radius * 0.7f, textPaint)
    }
    private fun drawNeedle(canvas: Canvas, size: Float) {
        val cx = size / 2f
        val cy = size / 2f
        val radius = size * 0.32f

        val angle = valueToAngle(verticalSpeed)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.015f

        val ex = cx + radius * cos(angle)
        val ey = cy + radius * sin(angle)

        canvas.drawLine(cx, cy, ex, ey, paint)

        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, size * 0.02f, paint)
    }
    private fun valueToAngle(value: Float): Float {
        val maxAngle = 120f
        val angle = (value / 10f) * maxAngle
        return Math.toRadians(angle.toDouble()).toFloat()
    }
}
