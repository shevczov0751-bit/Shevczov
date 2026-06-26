package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import kotlin.math.cos
import kotlin.math.sin

class TurnCoordinatorView(context: Context, attrs: AttributeSet?) :
    BaseInstrumentView(context, attrs) {

    /** -1..1 – стандартный разворот (наклон самолёта) */
    var turnRate = 0f

    /** -1..1 – положение шарика (скольжение / увод) */
    var slip = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2f
        val radius = size * 0.42f    // тот же, что в BaseInstrumentView.drawDial

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        paint.style = android.graphics.Paint.Style.STROKE

        // внешняя окружность циферблата (подчёркиваем круг)
        canvas.drawCircle(center, center, radius, paint)

        // дуга шкалы разворота
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

        // метки стандартного разворота (как на реальном приборе)
        for (i in -2..2) {
            val angleDeg = i * 15f
            val angle = Math.toRadians((angleDeg - 90).toDouble())

            val inner = radius * 0.85
            val outer = radius

            val x1 = center + cos(angle) * inner
            val y1 = center + sin(angle) * inner

            val x2 = center + cos(angle) * outer
            val y2 = center + sin(angle) * outer

            canvas.drawLine(
                x1.toFloat(),
                y1.toFloat(),
                x2.toFloat(),
                y2.toFloat(),
                paint
            )
        }

        // надписи L / R
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = size * 0.07f
        paint.textAlign = android.graphics.Paint.Align.CENTER

        canvas.drawText(
            "L",
            center - radius * 0.7f,
            center - radius * 0.2f,
            paint
        )

        canvas.drawText(
            "R",
            center + radius * 0.7f,
            center - radius * 0.2f,
            paint
        )

        // ===== самолёт =====
        canvas.save()

        val bankAngle = (turnRate.coerceIn(-1f, 1f)) * 35f
        canvas.rotate(bankAngle, center, center)

        paint.strokeWidth = size * 0.02f
        paint.style = android.graphics.Paint.Style.STROKE

        // крылья
        canvas.drawLine(
            center - size * 0.28f,
            center,
            center + size * 0.28f,
            center,
            paint
        )

        // фюзеляж
        canvas.drawLine(
            center,
            center - size * 0.05f,
            center,
            center + size * 0.18f,
            paint
        )

        // хвост
        canvas.drawLine(
            center - size * 0.06f,
            center + size * 0.12f,
            center + size * 0.06f,
            center + size * 0.12f,
            paint
        )

        canvas.restore()

        // ===== инклинометр (шарик) =====

        val tubeY = center + size * 0.32f

        paint.strokeWidth = size * 0.012f
        paint.style = android.graphics.Paint.Style.STROKE

        // трубка
        canvas.drawLine(
            center - size * 0.30f,
            tubeY,
            center + size * 0.30f,
            tubeY,
            paint
        )

        // шарик
        val ballOffset = (slip.coerceIn(-1f, 1f)) * size * 0.22f

        paint.style = android.graphics.Paint.Style.FILL

        canvas.drawCircle(
            center + ballOffset,
            tubeY,
            size * 0.035f,
            paint
        )
    }
}