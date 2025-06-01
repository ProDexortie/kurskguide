package com.example.kurskguide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlaceDetailActivity : AppCompatActivity() {
    private lateinit var place: Place
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        val placeId = intent.getIntExtra("place_id", -1)
        val allPlaces = KurskPlacesData.places + KurskPlacesData.getUserPlaces(this)
        place = allPlaces.find { it.id == placeId } ?: return finish()

        initViews()
        loadPlaceData()
    }

    private fun initViews() {
        title = place.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadPlaceData() {
        findViewById<TextView>(R.id.tvPlaceName).text = place.name
        findViewById<TextView>(R.id.tvPlaceDescription).text = place.fullDescription
        findViewById<TextView>(R.id.tvPlaceAddress).text = "📍 ${place.address}"
        findViewById<TextView>(R.id.tvPlacePhone).text = if (place.phone.isNotEmpty()) "📞 ${place.phone}" else ""
        findViewById<TextView>(R.id.tvPlaceHours).text = if (place.workingHours.isNotEmpty()) "🕒 ${place.workingHours}" else ""
        findViewById<TextView>(R.id.tvPlaceRating).text = "⭐ ${place.rating}/5.0"

        // Показываем кнопку удаления для пользовательских мест
        val btnDelete = findViewById<View>(R.id.btnDeletePlace)
        if (place.category == "Пользовательские места") {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                deleteUserPlace()
            }
        } else {
            btnDelete.visibility = View.GONE
        }

        // Кнопки действий
        findViewById<View>(R.id.btnShowOnMap).setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("place_id", place.id)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnAddToFavorites).setOnClickListener {
            toggleFavorite()
        }

        findViewById<View>(R.id.btnShare).setOnClickListener {
            sharePlace()
        }
    }

    private fun deleteUserPlace() {
        if (KurskPlacesData.deleteUserPlace(this, place.id)) {
            finish()
        }
    }

    private fun toggleFavorite() {
        if (isFavorite(place.id)) {
            removeFavorite(place.id)
            isFavorite = false
        } else {
            saveFavorite(place.id)
            isFavorite = true
        }
        val button = findViewById<TextView>(R.id.btnAddToFavorites)
        button.text = if (isFavorite) "❤️ Убрать из избранного" else "🤍 Добавить в избранное"
    }

    private fun sharePlace() {
        val shareText = "${place.name}\n${place.description}\n${place.address}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться местом"))
    }

    // Сохранить в избранное
    private fun saveFavorite(placeId: Int) {
        val prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet("favorite_places", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        favorites.add(placeId.toString())
        prefs.edit().putStringSet("favorite_places", favorites).apply()
    }

    // Удалить из избранного
    private fun removeFavorite(placeId: Int) {
        val prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet("favorite_places", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        favorites.remove(placeId.toString())
        prefs.edit().putStringSet("favorite_places", favorites).apply()
    }

    // Проверить, в избранном ли место
    private fun isFavorite(placeId: Int): Boolean {
        val prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet("favorite_places", mutableSetOf()) ?: mutableSetOf()
        return favorites.contains(placeId.toString())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}