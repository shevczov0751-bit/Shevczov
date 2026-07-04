package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.user_location.UserLocationLayer

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    private lateinit var userLocationLayer: UserLocationLayer

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.map_view)

        requestPermission()
    }

    private fun requestPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            enableLocation()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        }
    }

    private fun enableLocation() {

        userLocationLayer =
            MapKitFactory.getInstance()
                .createUserLocationLayer(mapView.mapWindow)

        userLocationLayer.isVisible = true

        userLocationLayer.isHeadingEnabled = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 1 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            enableLocation()
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
}



