package com.example.spotfinder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Displays a Google Map centered on a single (lat, lon),
 * with a marker titled by the address passed via Intent extras.
 *
 * Notes:
 * - We use MapView (not SupportMapFragment), so we must forward lifecycle calls.
 * - Requires a valid API key in AndroidManifest (or switch to osmdroid to avoid Google keys).
 */
class MapActivity : AppCompatActivity() {

    // MapView is a View; we manage its lifecycle manually
    private lateinit var mapView: MapView

    // GoogleMap reference (becomes non-null in getMapAsync callback)
    private var gmap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the XML layout containing <MapView/>
        setContentView(R.layout.activity_map)

        // Read extras from the Intent (defaults to downtown Toronto if missing)
        val address = intent.getStringExtra("address") ?: "Location"
        val lat = intent.getDoubleExtra("lat", 43.6532)
        val lon = intent.getDoubleExtra("lon", -79.3832)

        // Find the MapView and pass savedInstanceState (important for rotation)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // Asynchronously get the map and configure it once ready
        mapView.getMapAsync { map ->
            gmap = map

            val pos = LatLng(lat, lon)
            // Add a single marker at the provided coordinates
            map.addMarker(MarkerOptions().position(pos).title(address))
            // Move camera to the marker with a reasonable city zoom level
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f))
        }
    }

    // Forward lifecycle events to MapView to avoid memory leaks / blank maps
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
