package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import com.example.myapplication.style.InstrumentStyle

class AttitudeView(context: Context, attrs: AttributeSet?) :
    BaseInstrumentView(context, attrs) {

    var pitch = 0f
    var roll = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2f
        val radius = size * 0.45f

        // === РАМКА КАК В AirspeedView ===
        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        paint.style = android.graphics.Paint.Style.STROKE

        // круг прибора (тот самый белый круг-рамка)
        canvas.drawCircle(center, center, radius, paint)

        // === ВНУТРЕННЯЯ ЧАСТЬ АВИАГОРИЗОНТА ===
        canvas.save()

        // клип по кругу, чтобы небо/земля не выходили за рамку
        val path = Path()
        path.addCircle(center, center, radius, Path.Direction.CW)
        canvas.clipPath(path)

        // поворот всего горизонта на 90° (как ты хотел)
        canvas.rotate(90f, center, center)

        // вращение горизонта по крену
        canvas.rotate(-roll, center, center)

        // смещение по тангажу
        canvas.translate(0f, pitch * 4f)

        // небо
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = InstrumentStyle.skyColor
        canvas.drawRect(0f, 0f, size, center, paint)

        // земля
        paint.color = InstrumentStyle.groundColor
        canvas.drawRect(0f, center, size, size, paint)

        // линия горизонта
        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        canvas.drawLine(0f, center, size, center, paint)

        // линии тангажа
        val step = size * 0.07f
        for (i in -4..4) {
            if (i == 0) continue

            val y = center + i * step

            val lineWidth =
                if (i % 2 == 0) size * 0.30f
                else size * 0.18f

            canvas.drawLine(
                center - lineWidth / 2,
                y,
                center + lineWidth / 2,
                y,
                paint
            )
        }

        canvas.restore()
    }
}
