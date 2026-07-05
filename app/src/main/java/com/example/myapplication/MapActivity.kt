package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.RemoteError
import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.map.Map
import android.view.MotionEvent
import com.yandex.mapkit.ScreenPoint
import android.view.GestureDetector













class MapActivity : AppCompatActivity(), UserLocationObjectListener, Session.SearchListener {

    private lateinit var rulerButton: ImageButton
    private var rulerMode = false
    private var firstPoint: Point? = null
    private var secondPoint: Point? = null
    private var rulerPlacemarks = mutableListOf<PlacemarkMapObject>()
    private var rulerPolyline: PolylineMapObject? = null

    // Отдельная коллекция для результатов поиска (чтобы не очищать объекты линейки)
    private var searchCollection: MapObjectCollection? = null
    private lateinit var mapView: MapView
    private lateinit var gestureDetector: GestureDetector
    lateinit var locationmapkit: UserLocationLayer
    lateinit var searchEdit: EditText
    lateinit var searchManager: SearchManager
    lateinit var searchSession: Session
    private fun submitQuery(query: String) {
        searchSession = searchManager.submit(
            query, VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
            SearchOptions(), this)
    }
    fun Point.distanceTo(other: Point): Double {
        return Geo.distance(this, other)

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.map_view)

        mapView.map.move(
            CameraPosition(
                Point(53.345416, 83.732080),
                10.0f,
                0.0f,
                0.0f
            )
        )
        var mapKit: MapKit = MapKitFactory.getInstance()
        requestLocationPermission()
        var locationonmapkit = mapKit.createUserLocationLayer(mapView.mapWindow)
        locationonmapkit.isVisible = true
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        searchEdit = findViewById(R.id.Esearch)
        searchEdit.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitQuery(searchEdit.text.toString())
            }
            false

        }
        // Кнопка линейки
        rulerButton = findViewById(R.id.ruler_button)
        rulerButton.setOnClickListener { toggleRulerMode() }


        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (rulerMode) {
                    val point = mapView.mapWindow.screenToWorld(
                        ScreenPoint(e.x.toFloat(), e.y.toFloat())
                    )
                    point?.let { handleMapClick(it) }
                    return true
                }
                return false
            }
        })




        // Обработка кликов по карте через onTouch
        @Suppress("ClickableViewAccessibility")
        mapView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // не блокируем обработку, чтобы карта продолжала работать
        }
        
    }





    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )
            return
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


    override fun onObjectAdded(userLocationView: UserLocationView) {
        locationmapkit.setAnchor(
            PointF((mapView.width * 0.5).toFloat(), (mapView.height * 0.5).toFloat()),
            PointF((mapView.width * 0.83).toFloat(), (mapView.height * 0.83).toFloat())
        )
        userLocationView.arrow.setIcon(ImageProvider.fromResource(this, R.drawable.samolet))
        val picIcon = userLocationView.pin.useCompositeIcon()
        picIcon.setIcon(
            "icon", ImageProvider.fromResource(this, R.drawable.metka), IconStyle().setAnchor(
                PointF(0f, 0f)
            )
                .setRotationType(RotationType.ROTATE).setZIndex(0f).setScale(1f)
        )
        picIcon.setIcon(
            "pin", ImageProvider.fromResource(this, R.drawable.metka),
            IconStyle().setAnchor(PointF(0.5f, 0.5f)).setRotationType(RotationType.ROTATE)
                .setZIndex(1f).setScale(0.5f)
        )
        userLocationView.accuracyCircle.fillColor = Color.argb(0x66, 0, 0, 255)
    }

    override fun onObjectRemoved(p0: UserLocationView) {

    }

    override fun onObjectUpdated(
        p0: UserLocationView,
        p1: ObjectEvent
    ) {

    }

    override fun onSearchResponse(response: Response) {
        val mapObjects = mapView.map.mapObjects
        mapObjects.clear() // если используете общую коллекцию, лучше заменить на отдельную

        response.collection.children.forEach { searchResult ->
            searchResult.obj
                ?.geometry
                ?.firstOrNull()
                ?.point
                ?.let { point ->
                    mapObjects.addPlacemark(point, ImageProvider.fromResource(this, R.drawable.metka))
                }
        }
    }

    override fun onSearchError(error: Error) {
        val errorMessage = when (error) {
            is RemoteError -> "Проверьте интернет-соединение"
            else -> "Ошибка поиска: ${error.toString()}"
        }
        runOnUiThread {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            Log.e("SearchError", errorMessage)

        }
    }

    //функции линейки

    private fun toggleRulerMode() {
        rulerMode = !rulerMode
        if (rulerMode) {
            // Включаем режим – очищаем старые объекты
            clearRulerObjects()
            firstPoint = null
            secondPoint = null
            Toast.makeText(this, "Нажмите на карту для первой точки", Toast.LENGTH_SHORT).show()
            rulerButton.setBackgroundColor(Color.GREEN) // визуальный индикатор
        } else {
            // Выключаем режим
            clearRulerObjects()
            rulerButton.setBackgroundResource(android.R.drawable.btn_default)
        }
    }

    private fun handleMapClick(point: Point) {
        if (firstPoint == null) {
            // Первая точка
            firstPoint = point
            val placemark = mapView.map.mapObjects.addPlacemark(
                point,
                ImageProvider.fromResource(this, R.drawable.metka) // можно использовать свою метку
            )
            rulerPlacemarks.add(placemark)
            Toast.makeText(this, "Первая точка выбрана", Toast.LENGTH_SHORT).show()
        } else {
            // Вторая точка
            secondPoint = point
            val placemark = mapView.map.mapObjects.addPlacemark(
                point,
                ImageProvider.fromResource(this, R.drawable.metka)
            )
            rulerPlacemarks.add(placemark)

            // Рисуем линию
            val polyline = Polyline(listOf(firstPoint!!, secondPoint!!))
            rulerPolyline = mapView.map.mapObjects.addPolyline(polyline).apply {
               setStrokeColor(Color.BLUE)
               strokeWidth = 5f

            }

            // Вычисляем расстояние
            val distanceMeters = firstPoint!!.distanceTo(secondPoint!!)
            val distanceText = if (distanceMeters >= 1000) {
                "%.2f км".format(distanceMeters / 1000.0)
            } else {
                "%.0f м".format(distanceMeters)
            }
            Toast.makeText(this, "Расстояние: $distanceText", Toast.LENGTH_LONG).show()

            // Завершаем режим
            rulerMode = false
            rulerButton.setBackgroundResource(android.R.drawable.btn_default)
            // Очищаем временные точки, но объекты остаются на карте
            firstPoint = null
            secondPoint = null
        }
    }

    private fun clearRulerObjects() {
        rulerPlacemarks.forEach { mapView.map.mapObjects.remove(it) }
        rulerPlacemarks.clear()
        rulerPolyline?.let { mapView.map.mapObjects.remove(it) }
        rulerPolyline = null
    }

}


