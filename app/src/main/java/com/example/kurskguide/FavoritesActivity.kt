package com.example.kurskguide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var placesAdapter: PlacesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        title = "Избранное"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        loadFavorites()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewFavorites)

        // Пока загружаем все места (можно добавить логику избранного)
        placesAdapter = PlacesAdapter(emptyList()) { place ->
            val intent = Intent(this, PlaceDetailActivity::class.java)
            intent.putExtra("place_id", place.id)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = placesAdapter
        }
    }

    private fun getFavorites(): List<Place> {
        val prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favoriteIds = prefs.getStringSet("favorite_places", mutableSetOf()) ?: mutableSetOf()
        return KurskPlacesData.places.filter { favoriteIds.contains(it.id.toString()) }
    }


    private fun loadFavorites() {
        val favorites = getFavorites()
        placesAdapter.updatePlaces(favorites)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}