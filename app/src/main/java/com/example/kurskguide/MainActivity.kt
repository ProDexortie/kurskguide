package com.example.kurskguide

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var searchView: SearchView
    private lateinit var bottomNavigation: BottomNavigationView

    private val categories = listOf(
        Category("🏛️", "Исторические места", "Памятники и музеи города"),
        Category("🌳", "Парки и скверы", "Места для отдыха и прогулок"),
        Category("🍽️", "Рестораны и кафе", "Лучшие места для питания"),
        Category("🛍️", "Торговые центры", "Шоппинг и развлечения"),
        Category("🎭", "Театры и кино", "Культурные заведения"),
        Category("🏨", "Отели", "Размещение в городе"),
        Category("⛪", "Храмы", "Религиозные места"),
        Category("🎓", "Образование", "ВУЗы и школы"),
        Category("📍", "Пользовательские места", "Добавленные пользователями")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем категорию пользовательских мест при возврате в активность
        updateUserPlacesCategory()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewCategories)
        searchView = findViewById(R.id.searchView)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        val fabMap: FloatingActionButton = findViewById(R.id.fabMap)
        fabMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(getUpdatedCategories()) { category ->
            val intent = Intent(this, PlacesListActivity::class.java)
            intent.putExtra("category", category.name)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Уже здесь
                    true
                }
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, MapActivity::class.java))
                    true
                }
                R.id.nav_info -> {
                    startActivity(Intent(this, AboutCityActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    val intent = Intent(this@MainActivity, SearchResultsActivity::class.java)
                    intent.putExtra("query", it)
                    startActivity(intent)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun updateUserPlacesCategory() {
        val updatedCategories = getUpdatedCategories()
        categoryAdapter = CategoryAdapter(updatedCategories) { category ->
            val intent = Intent(this, PlacesListActivity::class.java)
            intent.putExtra("category", category.name)
            startActivity(intent)
        }
        recyclerView.adapter = categoryAdapter
    }

    private fun getUpdatedCategories(): List<Category> {
        val userPlacesCount = KurskPlacesData.getUserPlaces(this).size
        return categories.map { category ->
            if (category.name == "Пользовательские места") {
                if (userPlacesCount > 0) {
                    category.copy(description = "Добавленные пользователями ($userPlacesCount)")
                } else {
                    category.copy(description = "Добавьте свои места на карте")
                }
            } else {
                category
            }
        }
    }
}

// Модели данных
data class Category(
    val icon: String,
    val name: String,
    val description: String
)

data class Place(
    val id: Int,
    val name: String,
    val description: String,
    val fullDescription: String,
    val category: String,
    val address: String,
    val phone: String = "",
    val website: String = "",
    val workingHours: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Float = 0f
)

// Объект с данными о достопримечательностях Курска
object KurskPlacesData {
    val places = listOf(
        Place(
            1, "Курская крепость",
            "Историческое место основания города",
            "Курская крепость была основана в 1032 году. Здесь находился деревянный кремль, который защищал город от набегов кочевников. Сегодня на месте крепости находится мемориальный комплекс.",
            "Исторические места",
            "ул. Сонина, 1",
            "+7 (4712) 70-22-33",
            "http://kursk-museum.ru",
            "9:00-18:00",
            "",
            51.724124, 36.191233,
            4.5f
        ),
        Place(
            2, "Знаменский собор",
            "Главный православный храм города",
            "Знаменский кафедральный собор — один из красивейших храмов Курска. Построен в XVII веке в честь чудотворной иконы Божией Матери \"Знамение\" Курской-Коренной.",
            "Храмы",
            "ул. Луначарского, 4",
            "+7 (4712) 70-24-44",
            "https://курская-епархия.рф/",
            "7:00-19:00",
            "",
            51.727619, 36.192287,
            4.8f
        ),
        Place(
            3, "Мемориальный комплекс \"Курская дуга\"",
            "Памятник героям Курской битвы",
            "Грандиозный мемориальный комплекс посвящён одному из крупнейших сражений Великой Отечественной войны. Включает музей, Триумфальную арку и Вечный огонь.",
            "Исторические места",
            "Курская область, муниципальное образование Поныри",
            "+7 (4712) 35-35-75",
            "https://gokursk.ru/",
            "10:00-18:00",
            "",
            52.297568, 36.309885,
            4.9f
        ),
        Place(
            4, "Парк Героев Гражданской войны",
            "Главный парк города для семейного отдыха",
            "Большой парк с деревьями, аллеями для прогулок, детскими площадками и памятником. Идеальное место для отдыха всей семьей.",
            "Парки и скверы",
            "улица Радищева",
            "+7 (4712) 35-28-91",
            "",
            "6:00-23:00",
            "",
            51.745214, 36.188631,
            4.3f
        ),
        Place(
            5, "Театр драмы имени А.С. Пушкина",
            "Старейший театр Курска",
            "Курский государственный драматический театр имени А.С. Пушкина основан в 1792 году. Один из старейших провинциальных театров России.",
            "Театры и кино",
            "ул. Ленина, 26",
            "+7 (4712) 51-42-44",
            "http://kurskdrama.ru",
            "Касса: 10:00-19:00",
            "",
            51.738991, 36.191853,
            4.6f
        ),
        Place(
            6, "ТРЦ \"Пушкинский\"",
            "Крупнейший торговый центр города",
            "Современный торгово-развлекательный центр с магазинами, кинотеатром, фуд-кортом и развлечениями для всей семьи.",
            "Торговые центры",
            "ул. Ленина, 30",
            "+7 (4712) 777-777",
            "http://pushkinsky-kursk.ru",
            "10:00-22:00",
            "",
            51.737894, 36.192326,
            4.4f
        ),
        Place(
            7, "Ресторан \"Старый город\"",
            "Лучший ресторан русской кухни",
            "Уютный ресторан с традиционной русской кухней и домашней атмосферой. Специализируется на блюдах курской кухни.",
            "Рестораны и кафе",
            "ул. Ленина, 45",
            "+7 (4712) 55-66-77",
            "",
            "12:00-24:00",
            "",
            51.727800, 36.188900,
            4.7f
        ),
        Place(
            8, "Отель \"Курск\"",
            "Комфортабельная гостиница в центре",
            "Современный отель в самом центре города с комфортабельными номерами и отличным сервисом.",
            "Отели",
            "ул. Ленина, 24",
            "+7 (4712) 70-70-70",
            "http://hotel-kursk.ru",
            "24/7",
            "",
            51.736665, 36.191790,
            4.2f
        )
    )

    /**
     * Получить места по категории, включая пользовательские места
     */
    fun getPlacesByCategory(category: String, context: android.content.Context? = null): List<Place> {
        val allPlaces = if (context != null) {
            places + getUserPlaces(context)
        } else {
            places
        }
        return allPlaces.filter { it.category == category }
    }

    /**
     * Поиск мест по запросу, включая пользовательские места
     */
    fun searchPlaces(query: String, context: android.content.Context? = null): List<Place> {
        val allPlaces = if (context != null) {
            places + getUserPlaces(context)
        } else {
            places
        }
        return allPlaces.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
        }
    }

    /**
     * Получить все места (стандартные + пользовательские)
     */
    fun getAllPlaces(context: android.content.Context? = null): List<Place> {
        return if (context != null) {
            places + getUserPlaces(context)
        } else {
            places
        }
    }

    /**
     * Сохранить пользовательское место
     */
    fun saveUserPlace(context: android.content.Context, place: Place): Boolean {
        return try {
            val prefs = context.getSharedPreferences("user_places", android.content.Context.MODE_PRIVATE)
            val existingPlaces = getUserPlaces(context).toMutableList()

            // Проверяем, не существует ли уже место с таким ID
            val existingIndex = existingPlaces.indexOfFirst { it.id == place.id }
            if (existingIndex != -1) {
                existingPlaces[existingIndex] = place // Обновляем существующее
            } else {
                existingPlaces.add(place) // Добавляем новое
            }

            // Сохраняем список в JSON формате
            val gson = Gson()
            val json = gson.toJson(existingPlaces)
            prefs.edit().putString("places_json", json).apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Получить все пользовательские места
     */
    fun getUserPlaces(context: android.content.Context?): List<Place> {
        if (context == null) return emptyList()

        return try {
            val prefs = context.getSharedPreferences("user_places", android.content.Context.MODE_PRIVATE)
            val json = prefs.getString("places_json", null)

            if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<List<Place>>() {}.type
                gson.fromJson<List<Place>>(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Удалить пользовательское место
     */
    fun deleteUserPlace(context: android.content.Context, placeId: Int): Boolean {
        return try {
            val existingPlaces = getUserPlaces(context).toMutableList()
            val removed = existingPlaces.removeAll { it.id == placeId }

            if (removed) {
                val prefs = context.getSharedPreferences("user_places", android.content.Context.MODE_PRIVATE)
                val gson = Gson()
                val json = gson.toJson(existingPlaces)
                prefs.edit().putString("places_json", json).apply()
            }

            removed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Получить место по ID (ищет среди всех мест)
     */
    fun getPlaceById(context: android.content.Context?, id: Int): Place? {
        val allPlaces = getAllPlaces(context)
        return allPlaces.find { it.id == id }
    }

    /**
     * Проверить, является ли место пользовательским
     */
    fun isUserPlace(context: android.content.Context?, placeId: Int): Boolean {
        return getUserPlaces(context).any { it.id == placeId }
    }
}