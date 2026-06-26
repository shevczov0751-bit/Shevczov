package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import kotlin.math.cos
import kotlin.math.sin

class AltimeterView(context: Context, attrs: AttributeSet?) :
    BaseInstrumentView(context, attrs) {

    var altitude = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2f
        val radius = size * 0.45f

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        paint.style = android.graphics.Paint.Style.STROKE

        // внешний круг
        canvas.drawCircle(center, center, radius, paint)

        // деления шкалы
        for (i in 0 until 360 step 10) {

            val angle = Math.toRadians((i - 90).toDouble())

            val inner =
                if (i % 30 == 0) radius * 0.75
                else radius * 0.85

            val x1 = center + cos(angle) * inner
            val y1 = center + sin(angle) * inner

            val x2 = center + cos(angle) * radius
            val y2 = center + sin(angle) * radius

            canvas.drawLine(
                x1.toFloat(),
                y1.toFloat(),
                x2.toFloat(),
                y2.toFloat(),
                paint
            )
        }

        // цифры 0‑9 (тысячи футов)
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = size * 0.07f
        paint.textAlign = android.graphics.Paint.Align.CENTER

        for (i in 0..9) {

            val angle = Math.toRadians((i * 36 - 90).toDouble())

            val x = center + cos(angle) * radius * 0.6
            val y = center + sin(angle) * radius * 0.6 + paint.textSize / 3

            canvas.drawText(
                i.toString(),
                x.toFloat(),
                y.toFloat(),
                paint
            )
        }

        // ===== стрелки =====

        val hundredsAngle = altitude % 1000 / 1000f * 360f
        val thousandsAngle = altitude % 10000 / 10000f * 360f
        val tenThousandsAngle = altitude / 100000f * 360f

        // сотни футов (длинная)
        canvas.save()
        canvas.rotate(hundredsAngle, center, center)

        paint.strokeWidth = size * 0.01f

        canvas.drawLine(
            center,
            center,
            center,
            center - size * 0.40f,
            paint
        )

        canvas.restore()

        // тысячи футов
        canvas.save()
        canvas.rotate(thousandsAngle, center, center)

        paint.strokeWidth = size * 0.015f

        canvas.drawLine(
            center,
            center,
            center,
            center - size * 0.30f,
            paint
        )

        canvas.restore()

        // десятки тысяч футов
        canvas.save()
        canvas.rotate(tenThousandsAngle, center, center)

        paint.strokeWidth = size * 0.02f

        canvas.drawLine(
            center,
            center,
            center,
            center - size * 0.22f,
            paint
        )

        canvas.restore()
    }
}
