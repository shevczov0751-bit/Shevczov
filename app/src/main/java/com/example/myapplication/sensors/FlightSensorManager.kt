package com.example.myapplication.sensors

import android.content.Context
import android.hardware.*

class FlightSensorManager(context: Context) : SensorEventListener {

    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    var listener: ((Float,Float,Float)->Unit)? = null

    override fun onSensorChanged(event: SensorEvent) {

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            event.values
        )

        val orientation = FloatArray(3)

        SensorManager.getOrientation(rotationMatrix,orientation)

        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        listener?.invoke(pitch,roll,azimuth)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}