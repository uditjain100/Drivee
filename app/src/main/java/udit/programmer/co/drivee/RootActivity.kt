package udit.programmer.co.drivee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import kotlinx.android.synthetic.main.activity_root.*

class RootActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    var mapboxMap: MapboxMap? = null
    private lateinit var home: CarmenFeature
    private lateinit var work: CarmenFeature
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId"
    private val REQUEST_CODE_AUTOCOMPLETE = 1

    private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    private var hoveringMarker: ImageView? = null
    private var droppedMarkerLayer: Layer? = null

    var currentLat = 0.0
    var currentLng = 0.0

    var lat: Double = 0.0
    var lng: Double = 0.0

    private lateinit var permissionsManager: PermissionsManager

    private var locationEngine: LocationEngine? = null
    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    private lateinit var request: LocationEngineRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_root)

        val navController = findNavController(R.id.nav_host_fragment)
        home_nav_view.setupWithNavController(navController)

        mapView = findViewById(R.id.mapView_000)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) {

        }
    }


}