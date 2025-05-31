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

class SearchResultsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var placesAdapter: PlacesAdapter
    private lateinit var query: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        query = intent.getStringExtra("query") ?: ""
        title = "Поиск: $query"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        performSearch()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewSearchResults)

        placesAdapter = PlacesAdapter(emptyList()) { place ->
            val intent = Intent(this, PlaceDetailActivity::class.java)
            intent.putExtra("place_id", place.id)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchResultsActivity)
            adapter = placesAdapter
        }
    }

    private fun performSearch() {
        val results = KurskPlacesData.searchPlaces(query)
        placesAdapter.updatePlaces(results)

        findViewById<TextView>(R.id.tvSearchResultsCount).text =
            "Найдено результатов: ${results.size}"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}