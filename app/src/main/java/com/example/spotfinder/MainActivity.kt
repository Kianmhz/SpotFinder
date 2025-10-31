package com.example.spotfinder

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Main screen for SpotFinder:
 * - Displays all saved locations from SQLite in a RecyclerView
 * - Lets users search by address (partial match)
 * - Supports Add/Replace (upsert), Update, and Delete
 * - Opens MapActivity via Intent to show a marker for a location
 */
class MainActivity : AppCompatActivity() {

    // Database helper (manages table + CRUD)
    private lateinit var db: DbHelper

    // RecyclerView + Adapter for showing rows
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: LocationAdapter

    // Top: search controls
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnRefresh: Button

    // Bottom: add/replace controls
    private lateinit var etId: EditText
    private lateinit var etAddress: EditText
    private lateinit var etLat: EditText
    private lateinit var etLon: EditText
    private lateinit var btnAddReplace: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the XML layout for the main screen
        setContentView(R.layout.activity_main)

        // Initialize the database (creates table + seeds CSV on first run)
        db = DbHelper(this)

        // --- Wire up Views (findViewById) ---
        recycler = findViewById(R.id.recycler)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnRefresh = findViewById(R.id.btnRefresh)

        etId = findViewById(R.id.etId)
        etAddress = findViewById(R.id.etAddress)
        etLat = findViewById(R.id.etLat)
        etLon = findViewById(R.id.etLon)
        btnAddReplace = findViewById(R.id.btnAddReplace)

        // --- RecyclerView setup ---
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = LocationAdapter(
            // Update callback: persist changes and refresh list
            onUpdate = { item ->
                db.update(item)
                loadAll()
                Toast.makeText(this, "Updated #${item.id}", Toast.LENGTH_SHORT).show()
            },
            // Delete callback: remove from DB and refresh list
            onDelete = { item ->
                db.deleteById(item.id)
                loadAll()
                Toast.makeText(this, "Deleted #${item.id}", Toast.LENGTH_SHORT).show()
            },
            // Show on Map: open MapActivity and pass coordinate extras via Intent
            onShowMap = { item ->
                openMap(item)
            }
        )
        recycler.adapter = adapter

        // --- Button clicks ---
        btnSearch.setOnClickListener { doSearch() }
        btnRefresh.setOnClickListener { loadAll() }
        btnAddReplace.setOnClickListener { addReplace() }

        // On first open, show everything
        loadAll()
    }

    /** Runs a partial address search and (optionally) opens the map */
    private fun doSearch() {
        val q = etSearch.text.toString().trim()
        val hit = db.findByAddressLike(q)
        if (hit == null) {
            Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show()
        } else {
            // Show just the single result in the list
            adapter.submitList(listOf(hit))
            // Optionally open the map immediately (remove this if you prefer manual)
            openMap(hit)
        }
    }

    /** Refreshes the RecyclerView with all rows in the DB */
    private fun loadAll() {
        val all = db.getAll()
        adapter.submitList(all)
    }

    /**
     * Adds or replaces a record based on ID:
     * - If the ID exists, it's replaced (upsert)
     * - If the ID is new, a new row is inserted
     */
    private fun addReplace() {
        val id = etId.text.toString().toIntOrNull()
        val addr = etAddress.text.toString().trim()
        val lat = etLat.text.toString().toDoubleOrNull()
        val lon = etLon.text.toString().toDoubleOrNull()

        // Simple validation to avoid crashes and bad data
        if (id == null || addr.isBlank() || lat == null || lon == null) {
            Toast.makeText(this, "Fill ID, address, lat, lon", Toast.LENGTH_SHORT).show()
            return
        }

        db.upsert(Location(id, addr, lat, lon))
        clearAddForm()
        loadAll()
        Toast.makeText(this, "Saved #$id", Toast.LENGTH_SHORT).show()
    }

    /** Clears the "Add/Replace" input fields after saving */
    private fun clearAddForm() {
        etId.setText("")
        etAddress.setText("")
        etLat.setText("")
        etLon.setText("")
    }

    /**
     * Launches MapActivity using an explicit Intent.
     * Passes address + coordinates as extras.
     */
    private fun openMap(item: Location) {
        val it = Intent(this, MapActivity::class.java).apply {
            putExtra("address", item.address)
            putExtra("lat", item.latitude)
            putExtra("lon", item.longitude)
        }
        startActivity(it)
    }
}
