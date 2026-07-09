package com.example.myapplication.instruments

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import com.example.myapplication.style.InstrumentStyle

abstract class BaseInstrumentView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    protected val oval = RectF()

    override fun onDraw(canvas: Canvas) {

        val size = min(width, height).toFloat()

        drawCase(canvas, size)
        drawDial(canvas, size)
        setupOval(size)
        drawInstrument(canvas, size)
    }

    private fun drawCase(canvas: Canvas, size: Float) {

        paint.color = InstrumentStyle.caseColor
        paint.style = Paint.Style.FILL

        canvas.drawRoundRect(
            0f,
            0f,
            size,
            size,
            size * 0.08f,
            size * 0.08f,
            paint
        )
    }

    private fun drawDial(canvas: Canvas, size: Float) {

        paint.color = InstrumentStyle.dialColor

        canvas.drawCircle(
            size / 2,
            size / 2,
            size * 0.42f,
            paint
        )
    }

    protected fun setupOval(size: Float) {
        val radius = size * 0.42f
        val cx = size / 2
        val cy = size / 2

        oval.set(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius
        )
    }

    protected abstract fun drawInstrument(canvas: Canvas, size: Float)
}
