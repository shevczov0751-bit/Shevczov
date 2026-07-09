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

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawCircle(center, center, radius, paint)

        canvas.save()
        val path = Path()
        path.addCircle(center, center, radius, Path.Direction.CW)
        canvas.clipPath(path)

        canvas.translate(center, center)

        canvas.rotate(-roll)

        val pitchOffset = pitch * (size / 60f)
        canvas.translate(0f, pitchOffset)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = InstrumentStyle.skyColor
        canvas.drawRect(-size, -size, size, 0f, paint)

        paint.color = InstrumentStyle.groundColor
        canvas.drawRect(-size, 0f, size, size, paint)

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.01f
        canvas.drawLine(-size, 0f, size, 0f, paint)

        val step = size * 0.07f
        for (i in -4..4) {
            if (i == 0) continue
            val y = i * step
            val lineWidth =
                if (i % 2 == 0) size * 0.30f
                else size * 0.18f

            canvas.drawLine(
                -lineWidth / 2,
                y,
                lineWidth / 2,
                y,
                paint
            )
        }

        canvas.restore()

        paint.color = Color.WHITE
        paint.strokeWidth = size * 0.012f

        val wingSpan = radius * 0.9f
        val fuselage = radius * 0.3f

        canvas.drawLine(
            center - wingSpan / 2f,
            center,
            center + wingSpan / 2f,
            center,
            paint
        )

        canvas.drawLine(
            center,
            center - fuselage / 2f,
            center,
            center + fuselage / 2f,
            paint
        )
    }
}