package com.example.kurskguide

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

        val places = KurskPlacesData.getPlacesByCategory(categoryName)
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
        val places = KurskPlacesData.getPlacesByCategory(categoryName)
        placesAdapter.updatePlaces(places)
    }
}