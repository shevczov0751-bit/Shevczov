package com.example.myapplication.instruments

import android.content.Context import android.graphics.Canvas import android.graphics.Color import android.graphics.Paint import android.util.AttributeSet import kotlin.math.cos import kotlin.math.sin

class TurnCoordinatorView @JvmOverloads constructor( context: Context, attrs: AttributeSet? = null ) : BaseInstrumentView(context, attrs) {

    var turnRate: Float = 0f
        set(value) {
            field = value.coerceIn(-1f, 1f)
            invalidate()
        }

    var slip: Float = 0f
        set(value) {
            field = value.coerceIn(-1f, 1f)
            invalidate()
        }

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2f
        val radius = size * 0.42f

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        paint.style = Paint.Style.STROKE

        canvas.drawCircle(center, center, radius, paint)

        canvas.drawArc(
            center - radius,
            center - radius,
            center + radius,
            center + radius,
            210f,
            120f,
            false,
            paint
        )

        for (i in -2..2) {
            val angleDeg = i * 15f
            val angle = Math.toRadians((angleDeg - 90).toDouble())

            val inner = radius * 0.85
            val outer = radius

            val x1 = center + cos(angle) * inner
            val y1 = center + sin(angle) * inner
            val x2 = center + cos(angle) * outer
            val y2 = center + sin(angle) * outer

            canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)
        }

        paint.style = Paint.Style.FILL
        paint.textSize = size * 0.07f
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText("L", center - radius * 0.7f, center - radius * 0.2f, paint)
        canvas.drawText("R", center + radius * 0.7f, center - radius * 0.2f, paint)

        canvas.save()

        val bankAngle = turnRate * 35f
        canvas.rotate(bankAngle, center, center)

        paint.strokeWidth = size * 0.02f
        paint.style = Paint.Style.STROKE

        canvas.drawLine(center - size * 0.28f, center, center + size * 0.28f, center, paint)
        canvas.drawLine(center, center - size * 0.05f, center, center + size * 0.18f, paint)
        canvas.drawLine(
            center - size * 0.06f,
            center + size * 0.12f,
            center + size * 0.06f,
            center + size * 0.12f,
            paint
        )

        canvas.restore()

        val tubeY = center + size * 0.32f

        paint.strokeWidth = size * 0.012f
        paint.style = Paint.Style.STROKE

        canvas.drawLine(center - size * 0.30f, tubeY, center + size * 0.30f, tubeY, paint)

        val ballOffset = slip * size * 0.22f

        paint.style = Paint.Style.FILL
        canvas.drawCircle(center + ballOffset, tubeY, size * 0.035f, paint)
    }
}