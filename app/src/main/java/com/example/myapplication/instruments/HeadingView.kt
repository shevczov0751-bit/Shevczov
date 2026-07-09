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

    var heading = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size / 2f
        val radius = size * 0.42f

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.012f
        paint.style = android.graphics.Paint.Style.STROKE

        canvas.drawCircle(center, center, radius, paint)

        canvas.save()

        canvas.rotate(-heading, center, center)

        for (i in 0 until 360 step 5) {

            val angleRad = Math.toRadians(i.toDouble() - 90.0)

            val cosA = cos(angleRad)
            val sinA = sin(angleRad)

            val outer = radius
            val inner = if (i % 30 == 0) {
                radius * 0.75
            } else {
                radius * 0.82
            }

            val x1 = center + cosA * inner
            val y1 = center + sinA * inner

            val x2 = center + cosA * outer
            val y2 = center + sinA * outer

            canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)


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

        paint.textSize = size * 0.12f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.style = android.graphics.Paint.Style.FILL

        canvas.drawText("N", center, center - radius * 0.4f, paint)
        canvas.drawText("E", center + radius * 0.4f, center + size * 0.04f, paint)
        canvas.drawText("S", center, center + radius * 0.45f, paint)
        canvas.drawText("W", center - radius * 0.4f, center + size * 0.04f, paint)
        canvas.restore()

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
