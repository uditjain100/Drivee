package udit.programmer.co.drivee

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.material.navigation.NavigationView
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
import kotlinx.android.synthetic.main.home_app_bar_main.*
import kotlinx.android.synthetic.main.home_content_main.*
import kotlinx.android.synthetic.main.nav_header_layout.*
import udit.programmer.co.drivee.Models.Customer
import java.lang.Exception
import java.lang.ref.WeakReference

class RootActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration : AppBarConfiguration

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
        setSupportActionBar(home_toolbar)

        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_Home , R.id.nav_sign_out
        ), root_layout)

        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController, appBarConfiguration)
        home_nav_view.setupWithNavController(navController)

//        retrievingWork()

        mapView = findViewById(R.id.mapView_000)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        databaseReference = FirebaseDatabase.getInstance().getReference("DRIVER_LOCATION_REFERENCE")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineReference = FirebaseDatabase.getInstance().getReference("DRIVER_LOCATION_REFERENCE")
        geoFire = GeoFire(databaseReference)

        onlineReference.addValueEventListener(onlineValueEventListener)

        fab_navigate_pick_btn.setOnClickListener {
            locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
        }

    }

    private fun retrievingWork() {
        FirebaseDatabase.getInstance()
            .getReference(FirebaseAuth.getInstance().currentUser!!.phoneNumber.toString())
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val customer = snapshot.getValue(Customer::class.java)
                    nav_customer_name.text = customer!!.firstName + " " + customer.lastName
                    nav_customer_number.text = customer.number
                }
            })
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
        onlineReference.addValueEventListener(onlineValueEventListener)
        mapView.onResume()
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_sign_out -> {
                dialogWork()
            }
        }
        return true
    }

    private fun dialogWork() {
        AlertDialog.Builder(this).setCancelable(false).setMessage("Finally Signing Out")
            .setTitle("Sign Out").setPositiveButton(
                "SIGN OUT"
            ) { dialog, which ->
                FirebaseAuth.getInstance().signOut()
                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_logout -> { dialogWork() }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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
