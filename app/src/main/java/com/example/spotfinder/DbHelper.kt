package com.example.spotfinder

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import java.io.BufferedReader
import java.io.InputStreamReader

// DbHelper manages all database creation, upgrades, and CRUD operations
class DbHelper(private val ctx: Context) :
    SQLiteOpenHelper(ctx, "spotfinder.db", null, 1) {

    // Called only once — when the app runs for the first time
    override fun onCreate(db: SQLiteDatabase) {
        // Create the "locations" table with 4 columns
        db.execSQL(
            """
            CREATE TABLE locations(
                id INTEGER PRIMARY KEY,
                address TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL
            );
            """.trimIndent()
        )
        // Fill the table with 100 GTA locations from CSV in assets
        seedFromCsv(db)
    }

    // Called when database version changes (we can recreate the table)
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS locations")
        onCreate(db)
    }

    // Reads from locations_gta.csv in assets folder and inserts each row
    private fun seedFromCsv(db: SQLiteDatabase) {
        try {
            ctx.assets.open("locations_gta.csv").use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    reader.readLine() // skip header line
                    var line = reader.readLine()
                    db.beginTransaction()
                    try {
                        while (line != null) {
                            val p = line.split(',')
                            if (p.size >= 4) {
                                // Create a row with ContentValues
                                val cv = ContentValues().apply {
                                    put("id", p[0].trim().toInt())
                                    put("address", p[1].trim())
                                    put("latitude", p[2].trim().toDouble())
                                    put("longitude", p[3].trim().toDouble())
                                }
                                // Insert or replace existing ID
                                db.insertWithOnConflict(
                                    "locations", null, cv, SQLiteDatabase.CONFLICT_REPLACE
                                )
                            }
                            line = reader.readLine()
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore errors if file missing or already seeded
        }
    }

    // Returns all rows as a list of Location objects
    fun getAll(): List<Location> {
        val list = mutableListOf<Location>()
        readableDatabase.rawQuery("SELECT * FROM locations ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Location(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("address")),
                        c.getDouble(c.getColumnIndexOrThrow("latitude")),
                        c.getDouble(c.getColumnIndexOrThrow("longitude"))
                    )
                )
            }
        }
        return list
    }

    // Finds a single row by partial address match
    fun findByAddressLike(query: String): Location? {
        val args = arrayOf("%$query%")
        readableDatabase.rawQuery(
            "SELECT * FROM locations WHERE address LIKE ? LIMIT 1", args
        ).use { c ->
            if (c.moveToFirst()) {
                return Location(
                    c.getInt(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("address")),
                    c.getDouble(c.getColumnIndexOrThrow("latitude")),
                    c.getDouble(c.getColumnIndexOrThrow("longitude"))
                )
            }
        }
        return null
    }

    // Adds or replaces a location with the same ID
    fun upsert(l: Location) {
        val cv = ContentValues().apply {
            put("id", l.id)
            put("address", l.address)
            put("latitude", l.latitude)
            put("longitude", l.longitude)
        }
        writableDatabase.insertWithOnConflict(
            "locations", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // Updates an existing location
    fun update(l: Location) {
        val cv = ContentValues().apply {
            put("address", l.address)
            put("latitude", l.latitude)
            put("longitude", l.longitude)
        }
        writableDatabase.update("locations", cv, "id=?", arrayOf(l.id.toString()))
    }

    // Deletes a location by its ID
    fun deleteById(id: Int) {
        writableDatabase.delete("locations", "id=?", arrayOf(id.toString()))
    }
}

// Simple data class used to hold location records
data class Location(
    val id: Int,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
