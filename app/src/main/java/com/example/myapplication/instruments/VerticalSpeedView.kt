package com.example.myapplication.instruments

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

class VerticalSpeedView(context: Context, attrs: AttributeSet?) :
    BaseInstrumentView(context, attrs) {

    var verticalSpeed = 0f

    override fun drawInstrument(canvas: Canvas, size: Float) {

        val center = size/2
        val angle = verticalSpeed * 0.1f

        canvas.save()
        canvas.rotate(angle, center, center)

        paint.color = android.graphics.Color.WHITE
        paint.strokeWidth = size*0.01f

        canvas.drawLine(
            center,
            center,
            center,
            center-size*0.35f,
            paint
        )

        canvas.restore()
    }
}