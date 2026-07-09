package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.RotationType
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
import kotlin.math.abs

class MapActivity : AppCompatActivity(),
    UserLocationObjectListener,
    Session.SearchListener {

    private lateinit var mapView: MapView
    private lateinit var rulerButton: ImageButton
    private lateinit var myLocationButton: ImageButton
    private lateinit var searchEdit: EditText
    private lateinit var gestureOverlay: View

    private lateinit var searchManager: SearchManager
    private var searchSession: Session? = null
    private var searchCollection: MapObjectCollection? = null


    private lateinit var locationLayer: UserLocationLayer


    private var rulerMode = false
    private var firstPoint: Point? = null
    private var secondPoint: Point? = null
    private lateinit var rulerCollection: MapObjectCollection
    private val rulerPlacemarks = mutableListOf<PlacemarkMapObject>()
    private var rulerPolyline: PolylineMapObject? = null

    private val passThroughTapListener = MapObjectTapListener { _, _ -> false }

    private val mapInputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            hideKeyboardAndClearFocus()
            Log.d("RULER", "onMapTap: $point rulerMode=$rulerMode")
            if (!rulerMode) return
            handleRulerTap(point)
        }

        override fun onMapLongTap(map: Map, point: Point) {
            hideKeyboardAndClearFocus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.map_view)
        rulerButton = findViewById(R.id.ruler_button)
        myLocationButton = findViewById(R.id.my_location_button)
        searchEdit = findViewById(R.id.Esearch)
        gestureOverlay = findViewById(R.id.gesture_overlay)

        requestLocationPermission()

        rulerCollection = mapView.map.mapObjects.addCollection()

        locationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow).apply {
            isVisible = true
            isHeadingEnabled = true
            isAutoZoomEnabled = false
            setObjectListener(this@MapActivity)
        }

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                submitQuery(searchEdit.text.toString())
                hideKeyboardAndClearFocus()
                true
            } else false
        }

        rulerButton.setOnClickListener { toggleRulerMode() }
        myLocationButton.setOnClickListener { moveToMyLocation() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (rulerMode) {
                    rulerMode = false
                    clearRuler()
                    Toast.makeText(this@MapActivity, "Линейка: ВЫКЛ", Toast.LENGTH_SHORT).show()
                    return
                }
                finish()
            }
        })

        setupLeftBackSwipe()
    }

    private fun setupLeftBackSwipe() {
        val triggerDx = dpToPx(72)
        val maxDy = dpToPx(48)

        gestureOverlay.isClickable = true

        gestureOverlay.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var fired = false

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        fired = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.x - startX
                        val dy = abs(e.y - startY)

                        if (!fired && dx > triggerDx && dy < maxDy) {
                            fired = true
                            onBackPressedDispatcher.onBackPressed()
                            return true
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun dpToPx(dp: Int): Float =
        dp * resources.displayMetrics.density

    private fun toggleRulerMode() {
        rulerMode = !rulerMode
        if (rulerMode) {
            searchCollection?.clear()
            Toast.makeText(this, "Линейка: ВКЛ", Toast.LENGTH_SHORT).show()
        } else {
            clearRuler()
            Toast.makeText(this, "Линейка: ВЫКЛ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRulerTap(point: Point) {
        if (firstPoint == null) {
            firstPoint = point
            addRulerPlacemark(point)
            Toast.makeText(this, "Выберите вторую точку", Toast.LENGTH_SHORT).show()
            return
        }

        if (secondPoint == null) {
            secondPoint = point
            addRulerPlacemark(point)
            drawRulerLine()
            showDistance()
            return
        }

        clearRuler()
        firstPoint = point
        addRulerPlacemark(point)
    }

    private fun addRulerPlacemark(point: Point) {
        val pm = rulerCollection.addPlacemark(point)
        pm.setIcon(ImageProvider.fromResource(this, android.R.drawable.presence_online))
        rulerPlacemarks.add(pm)
    }

    private fun drawRulerLine() {
        val p1 = firstPoint ?: return
        val p2 = secondPoint ?: return

        rulerPolyline?.let { rulerCollection.remove(it) }
        rulerPolyline = rulerCollection.addPolyline(Polyline(listOf(p1, p2)))
        rulerPolyline?.setStrokeColor(Color.RED)
        rulerPolyline?.strokeWidth = 4f
    }

    private fun showDistance() {
        val p1 = firstPoint ?: return
        val p2 = secondPoint ?: return
        val meters = Geo.distance(p1, p2)
        Toast.makeText(this, "Расстояние: %.2f км".format(meters / 1000.0), Toast.LENGTH_LONG).show()
    }

    private fun clearRuler() {
        firstPoint = null
        secondPoint = null

        rulerPlacemarks.forEach { rulerCollection.remove(it) }
        rulerPlacemarks.clear()

        rulerPolyline?.let { rulerCollection.remove(it) }
        rulerPolyline = null
    }
    private fun submitQuery(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return

        searchCollection?.clear()
        searchCollection = mapView.map.mapObjects.addCollection()

        val vr = mapView.map.visibleRegion
        val bbox = BoundingBox(vr.bottomLeft, vr.topRight)
        val geometry = Geometry.fromBoundingBox(bbox)

        searchSession = searchManager.submit(q, geometry, SearchOptions(), this)
    }

    override fun onSearchResponse(response: Response) {
        val collection = searchCollection ?: return
        val points = response.collection.children
            .mapNotNull { it.obj?.geometry?.firstOrNull()?.point }

        points.forEach { collection.addPlacemark(it) }
        points.firstOrNull()?.let { mapView.map.move(CameraPosition(it, 15f, 0f, 0f)) }
    }

    override fun onSearchError(error: Error) {
        Toast.makeText(this, "Ошибка поиска", Toast.LENGTH_SHORT).show()
        Log.e("SEARCH", error.toString())
    }

    private fun moveToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(this)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(this, "Локация недоступна", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            mapView.map.move(CameraPosition(Point(loc.latitude, loc.longitude), 16f, 0f, 0f))
        }
    }
    private fun hideKeyboardAndClearFocus() {
        currentFocus?.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mapView.windowToken, 0)
    }
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }
    override fun onObjectAdded(userLocationView: UserLocationView) {
        userLocationView.arrow.setIcon(ImageProvider.fromResource(this, R.drawable.plane))
        userLocationView.arrow.setIconStyle(
            IconStyle().apply {
                anchor = PointF(0.5f, 0.5f)
                rotationType = RotationType.ROTATE
                zIndex = 1f
                scale = 0.7f
            }
        )

        userLocationView.accuracyCircle.fillColor = Color.TRANSPARENT

        userLocationView.arrow.addTapListener(passThroughTapListener)
        userLocationView.pin.addTapListener(passThroughTapListener)
        userLocationView.accuracyCircle.addTapListener(passThroughTapListener)
    }

    override fun onObjectRemoved(view: UserLocationView) {}
    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {}

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        mapView.map.addInputListener(mapInputListener)
    }

    override fun onStop() {
        mapView.map.removeInputListener(mapInputListener)
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}