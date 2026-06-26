package com.example.myapplication

import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.instruments.*
import com.example.myapplication.sensors.FlightSensorManager

class MainActivity : AppCompatActivity() {

    lateinit var sensorManager: FlightSensorManager

    lateinit var airspeed: AirspeedView
    lateinit var horizon: AttitudeView
    lateinit var altimeter: AltimeterView
    lateinit var heading: HeadingView
    lateinit var vsi: VerticalSpeedView
    lateinit var turn: TurnCoordinatorView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        airspeed = findViewById(R.id.airspeed)
        horizon = findViewById(R.id.horizon)
        altimeter = findViewById(R.id.altimeter)
        heading = findViewById(R.id.heading)
        vsi = findViewById(R.id.vsi)
        turn = findViewById(R.id.turn)

        sensorManager = FlightSensorManager(this)

        sensorManager.listener = { pitch, roll, azimuth ->

            horizon.pitch = pitch
            horizon.roll = roll
            heading.heading = azimuth

            horizon.invalidate()
            heading.invalidate()
        }

        sensorManager.sensorManager.registerListener(
            sensorManager,
            sensorManager.rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }
}