package udit.programmer.co.drivee

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.traffic.TrafficPlugin
import com.mapbox.mapboxsdk.style.layers.Layer
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.home_content_main.*
import java.lang.Exception
import java.lang.ref.WeakReference

class RootActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private lateinit var mapView: MapView
    var mapboxMap: MapboxMap? = null

    var currentLat = 0.0
    var currentLng = 0.0

    private lateinit var permissionsManager: PermissionsManager

    private var locationEngine: LocationEngine? = null
    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    private lateinit var request: LocationEngineRequest
    private var callback = SearchPickActivityLocationCallback(this)

    lateinit var geoFire: GeoFire
    private lateinit var databaseReference: DatabaseReference
    lateinit var onlineReference: DatabaseReference

    var onlineValueEventListener = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {}
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists())
                onlineReference.onDisconnect().removeValue()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_root)

        val navController = findNavController(R.id.nav_host_fragment)
        home_nav_view.setupWithNavController(navController)

        mapView = findViewById(R.id.mapView_000)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        databaseReference = FirebaseDatabase.getInstance().getReference("DRIVER_LOCATION_REFERENCE")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineReference = FirebaseDatabase.getInstance().getReference("DRIVER_LOCATION_REFERENCE")
        geoFire = GeoFire(databaseReference)

        registerOnlineUser()

        fab_navigate_pick_btn.setOnClickListener {
            locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
        }

    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) { style ->
            TrafficPlugin(mapView, mapboxMap, style).setVisibility(true)
            enableLocationComponent(style)
        }
    }

    @SuppressLint("LogNotTimber", "MissingPermission")
    private fun enableLocationComponent(it: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val locationComponent = mapboxMap!!.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, it).build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
            initLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine!!.requestLocationUpdates(request, callback, mainLooper);
        locationEngine!!.getLastLocation(callback)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "Explanation Needed", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) mapboxMap!!.getStyle { enableLocationComponent(it) }
        else Toast.makeText(this, "Permissions Not Granted", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineUser()
        mapView.onResume()
    }

    private fun registerOnlineUser() {
        onlineReference.addValueEventListener(onlineValueEventListener)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        locationEngine!!.removeLocationUpdates(callback)
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        locationEngine!!.removeLocationUpdates(callback)
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        locationEngine!!.removeLocationUpdates(callback)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}

class SearchPickActivityLocationCallback(activity: RootActivity?) :
    LocationEngineCallback<LocationEngineResult?> {
    private val activityWeakReference: WeakReference<RootActivity?>?

    init {
        activityWeakReference = WeakReference(activity)
    }

    override fun onSuccess(result: LocationEngineResult?) {
        val activity: RootActivity = activityWeakReference!!.get()!!
        if (activity != null) {
            val location = result!!.lastLocation ?: return
            activity.currentLat = location.latitude
            activity.currentLng = location.longitude
            Toast.makeText(
                activity, "lat : ${activity.currentLat} , lng : ${activity.currentLng}",
                Toast.LENGTH_SHORT
            ).show()
            if (activity.mapboxMap != null && result.lastLocation != null) {
                activity.mapboxMap!!.locationComponent
                    .forceLocationUpdate(result.lastLocation)
                activity.mapboxMap!!.animateCamera(
                    com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(activity.currentLat, activity.currentLng)).zoom(14.0)
                            .build()
                    ), 4000
                )
                activity.geoFire.setLocation(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    GeoLocation(activity.currentLat, activity.currentLng)
                ) { _, error ->
                    if (error != null) {
                        Toast.makeText(activity, error.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(activity, "You are Online", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onFailure(exception: Exception) {
        val activity: RootActivity = activityWeakReference!!.get()!!
        if (activity != null) {
            Toast.makeText(
                activity, exception.localizedMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
