package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import kotlin.math.cos
import kotlin.math.sin

class AirspeedView(context: Context, attrs: AttributeSet?) :
    BaseInstrumentView(context, attrs) {

    var speed = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2
        val radius = size * 0.45f

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        paint.style = android.graphics.Paint.Style.STROKE

        // круг прибора
        canvas.drawCircle(center, center, radius, paint)

        // деления шкалы
        for (i in 0..240 step 20) {

            val angle = Math.toRadians((i * 270f / 240f - 135).toDouble())

            val x1 = center + cos(angle) * radius * 0.85
            val y1 = center + sin(angle) * radius * 0.85

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

        // цифры
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = size * 0.06f
        paint.textAlign = android.graphics.Paint.Align.CENTER

        for (i in 0..240 step 40) {

            val angle = Math.toRadians((i * 270f / 240f - 135).toDouble())

            val x = center + cos(angle) * radius * 0.65
            val y = center + sin(angle) * radius * 0.65 + paint.textSize/3

            canvas.drawText(
                i.toString(),
                x.toFloat(),
                y.toFloat(),
                paint
            )
        }

        // стрелка
        val angle = -135f + speed * 270f / 240f

        canvas.save()
        canvas.rotate(angle, center, center)

        paint.strokeWidth = size * 0.015f

        canvas.drawLine(
            center,
            center,
            center + size * 0.35f,
            center,
            paint
        )

        canvas.restore()
    }
}
