package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.instruments.*
import com.example.myapplication.sensors.FlightSensorManager
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    private lateinit var sensorManager: FlightSensorManager

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

    private var fakeAltitude = 0f
    private var climb = true

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)

        val instrumentsView = LayoutInflater.from(this)
            .inflate(R.layout.page_instruments, null)

        val mapPage = LayoutInflater.from(this)
            .inflate(R.layout.page_map, null)

        mapView = mapPage.findViewById(R.id.mapview)

        val pages = listOf(instrumentsView, mapPage)

        viewPager.adapter = object :
            androidx.recyclerview.widget.RecyclerView.Adapter<PageViewHolder>() {

            override fun onCreateViewHolder(
                parent: android.view.ViewGroup,
                viewType: Int
            ): PageViewHolder {

                val frame = FrameLayout(parent.context)

                frame.layoutParams = android.view.ViewGroup.LayoutParams(
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

        airspeed = instrumentsView.findViewById(R.id.airspeed)
        horizon = instrumentsView.findViewById(R.id.horizon)
        altimeter = instrumentsView.findViewById(R.id.altimeter)
        heading = instrumentsView.findViewById(R.id.heading)
        vsi = instrumentsView.findViewById(R.id.vsi)
        turn = instrumentsView.findViewById(R.id.turn)

        sensorManager = FlightSensorManager(this)

        sensorManager.listener = { pitch, roll, azimuth ->

            if (!calibrated) {
                pitchOffset = pitch
                rollOffset = roll
                calibrated = true
            }

            val correctedPitch = pitch - pitchOffset
            val correctedRoll = roll - rollOffset

            horizon.pitch = -correctedRoll
            horizon.roll = correctedPitch

            heading.heading =
                ((azimuth + 90f) % 360f + 360f) % 360f

            turn.turnRate =
                (-correctedPitch / 45f).coerceIn(-1f, 1f)

            turn.slip =
                (correctedRoll / 45f).coerceIn(-1f, 1f)

            vsi.verticalSpeed = correctedPitch * 2f

            horizon.invalidate()
            heading.invalidate()
            turn.invalidate()
            vsi.invalidate()
        }

        startAltitudeSimulation()

        moveToGPS()
    }

    private fun startAltitudeSimulation() {

        handler.post(object : Runnable {

            override fun run() {

                if (climb) {

                    fakeAltitude += 20f

                    if (fakeAltitude > 10000f)
                        climb = false

                } else {

                    fakeAltitude -= 20f

                    if (fakeAltitude < 0f)
                        climb = true
                }

                altimeter.altitude = fakeAltitude
                altimeter.invalidate()

                handler.postDelayed(this, 100)
            }
        })
    }

    private fun moveToGPS() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationManager =
            getSystemService(LOCATION_SERVICE)
                    as android.location.LocationManager

        val location: Location? =
            locationManager.getLastKnownLocation(
                android.location.LocationManager.GPS_PROVIDER
            )

        if (location != null) {

            val point = Point(
                location.latitude,
                location.longitude
            )

            mapView.map.move(
                CameraPosition(
                    point,
                    15f,
                    0f,
                    0f
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()

        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {

        mapView.onStop()
        MapKitFactory.getInstance().onStop()

        super.onStop()
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

class PageViewHolder(
    val container: FrameLayout
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(container)