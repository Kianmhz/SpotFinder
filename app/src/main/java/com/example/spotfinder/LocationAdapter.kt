package com.example.spotfinder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


class LocationAdapter(
    private val onUpdate: (Location) -> Unit,
    private val onDelete: (Location) -> Unit,
    private val onShowMap: (Location) -> Unit
) : ListAdapter<Location, LocationAdapter.VH>(Diff) {

    /** Compares items so RV can animate diffs efficiently */
    object Diff : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(a: Location, b: Location) = a.id == b.id
        override fun areContentsTheSame(a: Location, b: Location) = a == b
    }

    /** ViewHolder holds references to row views */
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tvId)
        val etAddress: EditText = itemView.findViewById(R.id.etAddress)
        val etLat: EditText = itemView.findViewById(R.id.etLat)
        val etLon: EditText = itemView.findViewById(R.id.etLon)
        val btnUpdate: Button = itemView.findViewById(R.id.btnUpdate)
        val btnMap: Button = itemView.findViewById(R.id.btnMap)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Inflate the row layout (item_location.xml)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)

        // Populate fields with current values
        h.tvId.text = "#${item.id}"
        h.etAddress.setText(item.address)
        h.etLat.setText(item.latitude.toString())
        h.etLon.setText(item.longitude.toString())

        // Update button: read user-edited values and report back to activity
        h.btnUpdate.setOnClickListener {
            val newAddr = h.etAddress.text.toString()
            val newLat = h.etLat.text.toString().toDoubleOrNull() ?: item.latitude
            val newLon = h.etLon.text.toString().toDoubleOrNull() ?: item.longitude
            onUpdate(item.copy(address = newAddr, latitude = newLat, longitude = newLon))
        }

        // Map button: ask activity to open MapActivity for this item
        h.btnMap.setOnClickListener { onShowMap(item) }

        // Delete button: ask activity to remove this row from DB
        h.btnDelete.setOnClickListener { onDelete(item) }
    }
}
