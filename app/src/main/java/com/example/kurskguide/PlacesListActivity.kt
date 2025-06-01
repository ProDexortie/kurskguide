package com.example.kurskguide

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlacesListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var placesAdapter: PlacesAdapter
    private lateinit var categoryName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_places_list)

        categoryName = intent.getStringExtra("category") ?: ""
        title = categoryName

        initViews()
        loadPlaces()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewPlaces)

        val places = getPlacesByCategory(categoryName)
        placesAdapter = PlacesAdapter(places) { place ->
            val intent = Intent(this, PlaceDetailActivity::class.java)
            intent.putExtra("place_id", place.id)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PlacesListActivity)
            adapter = placesAdapter
        }
    }

    private fun loadPlaces() {
        val places = getPlacesByCategory(categoryName)
        placesAdapter.updatePlaces(places)
    }

    private fun getPlacesByCategory(category: String): List<Place> {
        val allPlaces = KurskPlacesData.places + KurskPlacesData.getUserPlaces(this)
        return allPlaces.filter { it.category == category }
    }
}