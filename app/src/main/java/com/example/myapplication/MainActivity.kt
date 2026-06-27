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

    // offsets для калибровки
    private var pitchOffset = 0f
    private var rollOffset = 0f
    private var calibrated = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // связываем приборы
        airspeed = findViewById(R.id.airspeed)
        horizon = findViewById(R.id.horizon)
        altimeter = findViewById(R.id.altimeter)
        heading = findViewById(R.id.heading)
        vsi = findViewById(R.id.vsi)
        turn = findViewById(R.id.turn)

        // менеджер датчиков
        sensorManager = FlightSensorManager(this)

        // обработка сенсоров
        sensorManager.listener = { pitch, roll, azimuth ->

            // первая позиция считается нулевой
            if (!calibrated) {
                pitchOffset = pitch
                rollOffset = roll
                calibrated = true
            }

            val correctedPitch = pitch - pitchOffset
            val correctedRoll = roll - rollOffset

            // авиагоризонт
            horizon.pitch = -correctedRoll
            horizon.roll = correctedPitch

            // компас → север справа телефона
            heading.heading = ((azimuth + 90f) % 360f + 360f) % 360f

            // координатор разворота
            turn.turnRate = (-correctedPitch / 45f).coerceIn(-1f, 1f)
            turn.slip = (correctedRoll / 45f).coerceIn(-1f, 1f)

            // вертикальная скорость (пример)
            vsi.verticalSpeed = correctedPitch * 2f

            // тестовые данные
            airspeed.speed = 120f
            altimeter.altitude = 1500f

            // обновляем приборы
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
