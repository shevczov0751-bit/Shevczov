package com.example.myapplication.instruments

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min
import com.example.myapplication.style.InstrumentStyle
import com.example.myapplication.R

abstract class BaseInstrumentView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    protected val oval = RectF()

    private val boltDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.bolt_round)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()

        android.util.Log.d(
            "DRAW",
            "${this::class.java.simpleName} size=$size"
        )

        drawCase(canvas, size)
        drawDial(canvas, size)

        setupOval(size)

        drawInstrument(canvas, size)

        drawScrews(canvas, size)
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

    private fun drawScrews(canvas: Canvas, size: Float) {

        val boltSize = (size * 0.06f).toInt()
        val offset = (size * 0.05f).toInt()

        drawBolt(canvas, offset, offset, boltSize)

        drawBolt(
            canvas,
            (size - offset - boltSize).toInt(),
            offset,
            boltSize
        )

        drawBolt(
            canvas,
            offset,
            (size - offset - boltSize).toInt(),
            boltSize
        )

        drawBolt(
            canvas,
            (size - offset - boltSize).toInt(),
            (size - offset - boltSize).toInt(),
            boltSize
        )
    }

    private fun drawBolt(
        canvas: Canvas,
        x: Int,
        y: Int,
        size: Int
    ) {

        boltDrawable?.setBounds(
            x,
            y,
            x + size,
            y + size
        )

        boltDrawable?.draw(canvas)
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
