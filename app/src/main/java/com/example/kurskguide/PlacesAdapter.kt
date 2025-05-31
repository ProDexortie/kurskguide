package com.example.kurskguide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlacesAdapter(
    private var places: List<Place>,
    private val onItemClick: (Place) -> Unit
) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.tvPlaceName)
        val descriptionText: TextView = view.findViewById(R.id.tvPlaceDescription)
        val addressText: TextView = view.findViewById(R.id.tvPlaceAddress)
        val ratingText: TextView = view.findViewById(R.id.tvPlaceRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.nameText.text = place.name
        holder.descriptionText.text = place.description
        holder.addressText.text = place.address
        holder.ratingText.text = "⭐ ${place.rating}"

        holder.itemView.setOnClickListener {
            onItemClick(place)
        }
    }

    override fun getItemCount() = places.size

    fun updatePlaces(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}