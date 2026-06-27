package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.instruments.*
import com.example.myapplication.sensors.FlightSensorManager

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: FlightSensorManager

    private lateinit var airspeed: AirspeedView
    private lateinit var horizon: AttitudeView
    private lateinit var altimeter: AltimeterView
    private lateinit var heading: HeadingView
    private lateinit var vsi: VerticalSpeedView
    private lateinit var turn: TurnCoordinatorView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // связываем приборы с layout
        airspeed = findViewById(R.id.airspeed)
        horizon = findViewById(R.id.horizon)
        altimeter = findViewById(R.id.altimeter)
        heading = findViewById(R.id.heading)
        vsi = findViewById(R.id.vsi)
        turn = findViewById(R.id.turn)

        // менеджер датчиков
        sensorManager = FlightSensorManager(this)

        // слушатель данных сенсоров
        sensorManager.listener = { pitch, roll, azimuth ->

            // авиагоризонт
            horizon.pitch = pitch
            horizon.roll = roll

            // курс
            heading.heading = azimuth

            // индикатор разворота (простая имитация)
            turn.turnRate = roll

            // имитация вертикальной скорости
            vsi.verticalSpeed = pitch * 2f

            // пример скорости
            airspeed.speed = 120f

            // пример высоты
            altimeter.altitude = 1500f

            // обновление приборов
            horizon.invalidate()
            heading.invalidate()
            turn.invalidate()
            vsi.invalidate()
            airspeed.invalidate()
            altimeter.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.stop()
    }
}
