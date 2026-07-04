package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.instruments.*
import com.example.myapplication.sensors.FlightSensorManager
import com.google.android.gms.location.*
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    private lateinit var sensorManager: FlightSensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var airspeed: AirspeedView
    private lateinit var horizon: AttitudeView
    private lateinit var altimeter: AltimeterView
    private lateinit var heading: HeadingView
    private lateinit var vsi: VerticalSpeedView
    private lateinit var turn: TurnCoordinatorView

    private lateinit var mapView: MapView

    private var pitchOffset = 0f
    private var rollOffset = 0f
    private var calibrated = false

    // Симуляция высоты
    private var fakeAltitude = 0f
    private var climb = true

    // Сглаживание VSI
    private var filteredVsi = 0f

    private val handler = Handler(Looper.getMainLooper())

    // Самолёт
    private var planePlacemark: PlacemarkMapObject? = null

    // Маршрут
    private var routePolyline: PolylineMapObject? = null
    private val routePoints = mutableListOf<Point>()

    // Длина маршрута
    private var routeLengthMeters = 0.0

    // Симуляция altimeter
    private val simulationRunnable = object : Runnable {
        override fun run() {
            val step = 20f

            fakeAltitude += if (climb) step else -step

            if (fakeAltitude > 10000f) {
                fakeAltitude = 10000f
                climb = false
            }

            if (fakeAltitude < 0f) {
                fakeAltitude = 0f
                climb = true
            }

            altimeter.altitude = fakeAltitude
            altimeter.invalidate()

            handler.postDelayed(this, 200L)
        }
    }

    // GPS callback
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return

            val speedMps = location.speed
            val speedKnots = speedMps * 1.94384f

            val userPoint = Point(location.latitude, location.longitude)

            runOnUiThread {
                // AIRSPEED
                airspeed.speed = speedKnots.coerceIn(0f, 240f)
                airspeed.invalidate()

                // Самолёт
                updatePlaneMarker(userPoint, location.bearing)

                // Маршрут
                updateRoute(userPoint)

                // Камера
                mapView.map.move(
                    CameraPosition(
                        userPoint,
                        15f,
                        0f,
                        0f
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)

        // Страница приборов
        val instrumentsView =
            LayoutInflater.from(this).inflate(R.layout.page_instruments, null)

        // Страница карты
        val mapPage =
            LayoutInflater.from(this).inflate(R.layout.page_map, null)

        mapView = mapPage.findViewById(R.id.mapview)

        // ViewPager
        val pages = listOf(instrumentsView, mapPage)

        viewPager.adapter =
            object : androidx.recyclerview.widget.RecyclerView.Adapter<PageViewHolder>() {

                override fun onCreateViewHolder(
                    parent: android.view.ViewGroup,
                    viewType: Int
                ): PageViewHolder {
                    val frame = FrameLayout(parent.context)
                    frame.layoutParams =
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    return PageViewHolder(frame)
                }

                override fun getItemCount(): Int = pages.size

                override fun onBindViewHolder(
                    holder: PageViewHolder,
                    position: Int
                ) {
                    holder.container.removeAllViews()

                    if (pages[position].parent != null) {
                        (pages[position].parent as android.view.ViewGroup)
                            .removeView(pages[position])
                    }

                    holder.container.addView(pages[position])
                }
            }

        // Instruments
        airspeed = instrumentsView.findViewById(R.id.airspeed)
        horizon = instrumentsView.findViewById(R.id.horizon)
        altimeter = instrumentsView.findViewById(R.id.altimeter)
        heading = instrumentsView.findViewById(R.id.heading)
        vsi = instrumentsView.findViewById(R.id.vsi)
        turn = instrumentsView.findViewById(R.id.turn)

        // Sensors
        sensorManager = FlightSensorManager(this)

        sensorManager.listener = { pitch, roll, azimuth ->
            if (!calibrated) {
                pitchOffset = pitch
                rollOffset = roll
                calibrated = true
            }

            val correctedPitch = pitch - pitchOffset
            val correctedRoll = roll - rollOffset

            runOnUiThread {
                // Horizon
                horizon.pitch = -correctedRoll
                horizon.roll = correctedPitch
                horizon.invalidate()

                // Heading
                heading.heading =
                    ((azimuth + 90f) % 360f + 360f) % 360f
                heading.invalidate()

                // Turn Coordinator
                turn.turnRate = (-correctedPitch / 45f).coerceIn(-1f, 1f)
                turn.slip = (correctedRoll / 30f).coerceIn(-1f, 1f)
                turn.invalidate()

                // VSI
                var targetVsi =
                    (-correctedPitch * 60f).coerceIn(-1500f, 1500f)

                if (abs(targetVsi) < 80f) {
                    targetVsi = 0f
                }

                filteredVsi = filteredVsi * 0.985f + targetVsi * 0.015f
                vsi.verticalSpeed = filteredVsi
                vsi.invalidate()
            }
        }

        // GPS
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    // Самолёт
    private fun updatePlaneMarker(
        position: Point,
        bearing: Float
    ) {
        val mapObjects = mapView.map.mapObjects

        if (planePlacemark == null) {
            val planeImage = ImageProvider.fromResource(
                this,
                R.drawable.ic_plane
            )

            planePlacemark =
                mapObjects.addPlacemark(position, planeImage)

            planePlacemark?.setIconStyle(
                IconStyle().apply {
                    // Центр картинки
                    anchor = PointF(0.5f, 0.5f)
                    // Размер
                    scale = 0.4f
                    // Поворот
                    rotationType = RotationType.ROTATE
                }
            )
        } else {
            planePlacemark?.geometry = position
        }

        // Курс самолёта
        planePlacemark?.direction = bearing
    }

    // Маршрут + линейка
    private fun updateRoute(position: Point) {
        routePoints.add(position)

        val mapObjects = mapView.map.mapObjects

        if (routePolyline == null) {
            routePolyline =
                mapObjects.addPolyline(Polyline(routePoints))

            // Цвет и толщина линии — через методы
            routePolyline?.setStrokeColor(0xFFFFA500.toInt()) // оранжевый
            routePolyline?.setStrokeWidth(5f)
        } else {
            routePolyline?.geometry = Polyline(routePoints)
        }

        // Линейка
        if (routePoints.size >= 2) {
            val lastIndex = routePoints.size - 1

            val p1 = routePoints[lastIndex - 1]
            val p2 = routePoints[lastIndex]

            val segment = distanceBetween(p1, p2)
            routeLengthMeters += segment

            title =
                "Маршрут: ${
                    String.format(
                        "%.2f",
                        routeLengthMeters / 1000.0
                    )
                } км"
        }
    }

    // Расстояние между точками
    private fun distanceBetween(
        p1: Point,
        p2: Point
    ): Double {
        val R = 6371000.0

        val lat1 = Math.toRadians(p1.latitude)
        val lon1 = Math.toRadians(p1.longitude)

        val lat2 = Math.toRadians(p2.latitude)
        val lon2 = Math.toRadians(p2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a =
            sin(dLat / 2).pow(2.0) +
                    cos(lat1) * cos(lat2) *
                    sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    // GPS START
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        val request =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            )
                .setMinUpdateDistanceMeters(1f)
                .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Lifecycle
    override fun onResume() {
        super.onResume()

        sensorManager.start()
        handler.post(simulationRunnable)
        startLocationUpdates()

        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onPause() {
        sensorManager.stop()
        handler.removeCallbacks(simulationRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        mapView.onStop()
        MapKitFactory.getInstance().onStop()

        super.onPause()
    }
}

// ViewHolder
class PageViewHolder(
    val container: FrameLayout
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(container)