package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.instruments.AirspeedView
import com.example.myapplication.instruments.AltimeterView
import com.example.myapplication.instruments.AttitudeView
import com.example.myapplication.instruments.HeadingView
import com.example.myapplication.instruments.TurnCoordinatorView
import com.example.myapplication.instruments.VerticalSpeedView
import com.example.myapplication.sensors.FlightSensorManager
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: FlightSensorManager

    private lateinit var airspeed: AirspeedView
    private lateinit var horizon: AttitudeView
    private lateinit var altimeter: AltimeterView
    private lateinit var heading: HeadingView
    private lateinit var vsi: VerticalSpeedView
    private lateinit var turn: TurnCoordinatorView

    private lateinit var gestureDetector: GestureDetector

    private var pitchOffset = 0f
    private var rollOffset = 0f
    private var calibrated = false

    private var fakeAltitude = 0f
    private var climb = true

    private val handler = Handler(Looper.getMainLooper())

    // Настройки свайпа
    private val SWIPE_THRESHOLD = 120
    private val SWIPE_VELOCITY_THRESHOLD = 120f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initViews()
        initSensors()
        setupSwipeToMap()

        startAltitudeSimulation()
    }

    private fun initViews() {

        airspeed = findViewById(R.id.airspeed)
        horizon = findViewById(R.id.horizon)
        altimeter = findViewById(R.id.altimeter)
        heading = findViewById(R.id.heading)
        vsi = findViewById(R.id.vsi)
        turn = findViewById(R.id.turnCoordinator)
    }

    private fun initSensors() {

        sensorManager = FlightSensorManager(this)

        sensorManager.listener = { pitch, roll, azimuth ->

            // Калибровка
            if (!calibrated) {
                pitchOffset = pitch
                rollOffset = roll
                calibrated = true
            }

            val correctedPitch = pitch - pitchOffset
            val correctedRoll = roll - rollOffset

            // Авиагоризонт
            horizon.pitch = -correctedRoll
            horizon.roll = correctedPitch

            // Курс
            heading.heading =
                ((azimuth + 90f) % 360f + 360f) % 360f

            // Координатор разворота
            turn.turnRate =
                (-correctedPitch / 45f).coerceIn(-1f, 1f)

            turn.slip =
                (correctedRoll / 45f).coerceIn(-1f, 1f)

            // Вертикальная скорость
            vsi.verticalSpeed = correctedPitch * 2f

            // Обновление приборов
            horizon.invalidate()
            heading.invalidate()
            turn.invalidate()
            vsi.invalidate()
        }
    }

    private fun setupSwipeToMap() {

        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {

                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y

                    // Только горизонтальный свайп
                    if (abs(diffX) > abs(diffY)) {

                        if (
                            abs(diffX) > SWIPE_THRESHOLD &&
                            abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                        ) {

                            if (diffX < 0) {

                                // Свайп влево → карта
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        MapActivity::class.java
                                    )
                                )

                                overridePendingTransition(
                                    android.R.anim.slide_in_left,
                                    android.R.anim.slide_out_right
                                )
                            }

                            return true
                        }
                    }

                    return false
                }
            }
        )
    }

    // Ловим touch поверх всех приборов
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        gestureDetector.onTouchEvent(ev)

        return super.dispatchTouchEvent(ev)
    }

    private fun startAltitudeSimulation() {

        handler.post(object : Runnable {

            override fun run() {

                if (climb) {
                    fakeAltitude += 20f

                    if (fakeAltitude >= 10000f) {
                        climb = false
                    }

                } else {

                    fakeAltitude -= 20f

                    if (fakeAltitude <= 0f) {
                        climb = true
                    }
                }

                // Высотомер
                altimeter.altitude = fakeAltitude
                altimeter.invalidate()



                handler.postDelayed(this, 100)
            }
        })
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