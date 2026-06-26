package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import kotlin.math.cos
import kotlin.math.sin

class HeadingView(context: Context, attrs: AttributeSet?) :
    BaseInstrumentView(context, attrs) {

    // heading в градусах: 0–360
    var heading = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2f
        val radius = size * 0.42f

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.012f
        paint.style = android.graphics.Paint.Style.STROKE

        // окружность шкалы
        canvas.drawCircle(center, center, radius, paint)

        canvas.save()

        // вращаем ВСЮ шкалу
        canvas.rotate(-heading, center, center)

        // риски круговой шкалы
        for (i in 0 until 360 step 5) {

            val angleRad = Math.toRadians(i.toDouble() - 90.0)

            val cosA = cos(angleRad)
            val sinA = sin(angleRad)

            val outer = radius
            val inner = if (i % 30 == 0) {
                radius * 0.75   // длинные риски каждые 30°
            } else {
                radius * 0.82   // короткие риски
            }

            val x1 = center + cosA * inner
            val y1 = center + sinA * inner

            val x2 = center + cosA * outer
            val y2 = center + sinA * outer

            canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)

            // подписи 30°, 60°, 90° …
            if (i % 30 == 0) {
                paint.textSize = size * 0.07f
                paint.style = android.graphics.Paint.Style.FILL

                val txtX = center + cosA * (radius * 0.55)
                val txtY = center + sinA * (radius * 0.55) + (size * 0.025f)

                canvas.drawText(
                    (i / 10).toString(),
                    txtX.toFloat(),
                    txtY.toFloat(),
                    paint
                )
            }
        }

        //------------------ Compass letters -------------------

        paint.textSize = size * 0.12f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.style = android.graphics.Paint.Style.FILL

        // Север N
        canvas.drawText("N", center, center - radius * 0.4f, paint)
        // Восток E
        canvas.drawText("E", center + radius * 0.4f, center + size * 0.04f, paint)
        // Юг S
        canvas.drawText("S", center, center + radius * 0.45f, paint)
        // Запад W
        canvas.drawText("W", center - radius * 0.4f, center + size * 0.04f, paint)

        canvas.restore()

        //------------------ TRIANGLE HEADING MARKER -------------------

        paint.color = Color.YELLOW
        paint.style = android.graphics.Paint.Style.FILL

        val markerPath = Path()
        markerPath.moveTo(center, center - radius * 0.9f)
        markerPath.lineTo(center - size * 0.04f, center - radius * 0.78f)
        markerPath.lineTo(center + size * 0.04f, center - radius * 0.78f)
        markerPath.close()

        canvas.drawPath(markerPath, paint)
    }
}
