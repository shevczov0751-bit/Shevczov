package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.instruments.*
import com.example.myapplication.sensors.FlightSensorManager

class MainActivity : AppCompatActivity(), LocationListener {

    // менеджер датчиков ориентации
    private lateinit var sensorManager: FlightSensorManager

    // GPS
    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST = 1001

    // приборы
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

    // симуляция высоты
    private var fakeAltitude = 0f
    private var climb = true

    private val handler = Handler(Looper.getMainLooper())

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

        // менеджер датчиков ориентации
        sensorManager = FlightSensorManager(this)

        sensorManager.listener = { pitch, roll, azimuth ->

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

            // компас (azimuth + 90 для имитации course)
            heading.heading = ((azimuth + 90f) % 360f + 360f) % 360f

            // координатор разворота
            turn.turnRate = (-correctedPitch / 45f).coerceIn(-1f, 1f)
            turn.slip = (correctedRoll / 45f).coerceIn(-1f, 1f)

            // вариометр (упрощённо от pitch)
            vsi.verticalSpeed = correctedPitch * 2f

            horizon.invalidate()
            heading.invalidate()
            turn.invalidate()
            vsi.invalidate()
            // airspeed перерисовывается при изменении скорости в onLocationChanged
        }

        // запуск симуляции высоты (можно выключить, если будет реальная высота из GPS/барометра)
        startAltitudeSimulation()

        // GPS менеджер
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        checkLocationPermission()
    }

    // ---------- GPS / AIRSPEED ----------

    private fun checkLocationPermission() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val fineGranted = ContextCompat.checkSelfPermission(this, fine) ==
                PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, coarse) ==
                PackageManager.PERMISSION_GRANTED

        if (!fineGranted || !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(fine, coarse),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // GPS_PROVIDER — основная скорость;
        // при желании можешь добавить NETWORK_PROVIDER
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,   // минимум 1 сек
            0f,      // любое расстояние
            this
        )
    }

    override fun onLocationChanged(location: Location) {
        // скорость в м/с
        val speedMs = location.speed

        // перевод в узлы (knots) или км/ч
        val speedKnots = speedMs * 1.94384f
        // val speedKmh = speedMs * 3.6f

        // ограничиваем шкалой 0..240
        val valueForInstrument = speedKnots.coerceIn(0f, 240f)

        // устанавливаем в прибор
        airspeed.speed = valueForInstrument
        airspeed.invalidate()
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    // ответ на запрос разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    // ---------- СИМУЛЯЦИЯ ВЫСОТЫ ----------

    private fun startAltitudeSimulation() {
        handler.post(object : Runnable {
            override fun run() {

                if (climb) {
                    fakeAltitude += 20f
                    if (fakeAltitude > 10000f) climb = false
                } else {
                    fakeAltitude -= 20f
                    if (fakeAltitude < 0f) climb = true
                }

                altimeter.altitude = fakeAltitude
                altimeter.invalidate()

                handler.postDelayed(this, 100)
            }
        })
    }

    // ---------- ЖИЗНЕННЫЙ ЦИКЛ ----------

    override fun onResume() {
        super.onResume()
        sensorManager.start()
        // возобновить GPS, если уже есть разрешения
        checkLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.stop()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
    }
}
