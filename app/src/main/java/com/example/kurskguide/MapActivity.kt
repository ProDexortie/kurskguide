package com.example.kurskguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class MapActivity : AppCompatActivity(), InputListener {

    private lateinit var mapView: MapView
    private lateinit var map: Map
    private lateinit var mapObjectCollection: MapObjectCollection

    // ВАЖНО: Сохраняем сильные ссылки на все объекты
    private val placeMarkers = mutableMapOf<Int, PlacemarkMapObject>()
    private val tapListeners = mutableMapOf<Int, MapObjectTapListener>() // Сохраняем слушатели
    private val bitmapCache = mutableMapOf<String, Bitmap>() // Кэш для bitmap'ов

    private var selectedPlace: Place? = null

    private lateinit var cardPlaceInfo: CardView
    private lateinit var tvInfoPlaceName: TextView
    private lateinit var tvInfoPlaceAddress: TextView
    private lateinit var btnInfoDetails: Button
    private lateinit var btnInfoRoute: Button
    private lateinit var btnCloseInfo: Button

    private var targetPlaceId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MapKit теперь инициализируется в Application классе!
        // Никаких вызовов MapKitFactory здесь больше нет

        setContentView(R.layout.activity_map)

        targetPlaceId = intent.getIntExtra("place_id", -1)

        title = "Карта Курска"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        initMap()
        setupUI()
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        cardPlaceInfo = findViewById(R.id.cardPlaceInfo)
        tvInfoPlaceName = findViewById(R.id.tvInfoPlaceName)
        tvInfoPlaceAddress = findViewById(R.id.tvInfoPlaceAddress)
        btnInfoDetails = findViewById(R.id.btnInfoDetails)
        btnInfoRoute = findViewById(R.id.btnInfoRoute)
        btnCloseInfo = findViewById(R.id.btnCloseInfo)
    }

    private fun initMap() {
        try {
            map = mapView.map
            mapObjectCollection = map.mapObjects

            // Центрируем карту на Курске
            val kurskCenter = Point(51.7373, 36.1873)
            map.move(
                CameraPosition(kurskCenter, 12.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )

            // Включаем управление картой
            map.addInputListener(this)

            // Добавляем маркеры через небольшую задержку
            Handler(Looper.getMainLooper()).postDelayed({
                addAllPlacesToMap()

                // Если передан конкретный ID места, показываем его
                if (targetPlaceId != -1) {
                    showTargetPlace(targetPlaceId)
                }
            }, 1000)


        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка инициализации карты: ${e.message}", Toast.LENGTH_LONG).show()
            println("Ошибка initMap: ${e.message}")
        }
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btnShowAllPlaces).setOnClickListener {
            showAllPlaces()
        }

        findViewById<Button>(R.id.btnMyLocation).setOnClickListener {
            requestLocationPermissionAndShow()
        }

        // Долгое нажатие для диагностики
        findViewById<Button>(R.id.btnShowAllPlaces).setOnLongClickListener {
            checkMarkersIntegrity()
            true
        }

        btnCloseInfo.setOnClickListener {
            hideInfoPanel()
        }

        btnInfoDetails.setOnClickListener {
            selectedPlace?.let { place ->
                val intent = Intent(this, PlaceDetailActivity::class.java)
                intent.putExtra("place_id", place.id)
                startActivity(intent)
            }
        }

        btnInfoRoute.setOnClickListener {
            selectedPlace?.let { place ->
                openYandexMapsForRoute(place)
            }
        }
    }

    private fun addAllPlacesToMap() {
        try {
            // Очищаем старые данные
            placeMarkers.clear()
            tapListeners.clear()
            mapObjectCollection.clear()

            var addedCount = 0

            KurskPlacesData.places.forEach { place ->
                if (place.latitude != 0.0 && place.longitude != 0.0) {
                    addPlaceMarker(place)
                    addedCount++

                    // Добавляем отладочную информацию
                    println("Добавлен маркер ${place.id}: ${place.name} (${place.category})")
                }
            }


            // Проверяем через секунду
            Handler(Looper.getMainLooper()).postDelayed({
                checkMarkersIntegrity()
            }, 1000)

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка добавления маркеров: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addPlaceMarker(place: Place) {
        try {
            val point = Point(place.latitude, place.longitude)

            // Используем кэшированный bitmap или создаем новый
            val bitmap = bitmapCache.getOrPut(place.category) {
                val markerIconResource = getMarkerIcon(place.category)
                createBitmapFromVector(markerIconResource)
            }

            val markerIcon = ImageProvider.fromBitmap(bitmap)
            val placemark = mapObjectCollection.addPlacemark(point, markerIcon)

            // ВАЖНО: Сначала сохраняем userData
            placemark.userData = place

            // Создаем и сохраняем TapListener
            val tapListener = object : MapObjectTapListener {
                override fun onMapObjectTap(mapObject: MapObject, point: Point): Boolean {
                    try {
                        println("Нажатие на маркер: ${place.name}")
                        showPlaceInfo(place)
                        return true
                    } catch (e: Exception) {
                        println("Ошибка при нажатии на маркер: ${e.message}")
                        Toast.makeText(this@MapActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
            }

            // Сохраняем сильные ссылки
            placeMarkers[place.id] = placemark
            tapListeners[place.id] = tapListener

            // Добавляем слушатель
            placemark.addTapListener(tapListener)

            println("Маркер ${place.id} (${place.name}) успешно создан")

        } catch (e: Exception) {
            println("Ошибка создания маркера для ${place.name}: ${e.message}")

            // Fallback на стандартный маркер
            try {
                val point = Point(place.latitude, place.longitude)
                val placemark = mapObjectCollection.addPlacemark(point)
                placemark.userData = place

                val tapListener = object : MapObjectTapListener {
                    override fun onMapObjectTap(mapObject: MapObject, point: Point): Boolean {
                        showPlaceInfo(place)
                        return true
                    }
                }

                placeMarkers[place.id] = placemark
                tapListeners[place.id] = tapListener
                placemark.addTapListener(tapListener)


            } catch (fallbackError: Exception) {
                Toast.makeText(this, "Критическая ошибка маркера ${place.name}: ${fallbackError.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createBitmapFromVector(vectorResId: Int): Bitmap {
        try {
            val vectorDrawable = ContextCompat.getDrawable(this, vectorResId)
                ?: throw IllegalArgumentException("Не найден ресурс $vectorResId")

            // Увеличиваем размер для лучшего взаимодействия
            val width = 56.dpToPx()
            val height = 56.dpToPx()

            vectorDrawable.setBounds(0, 0, width, height)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable.draw(canvas)

            println("Bitmap создан: ${width}x${height}")
            return bitmap

        } catch (e: Exception) {
            println("Ошибка создания bitmap: ${e.message}")
            throw e
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun getMarkerIcon(category: String): Int {
        return when (category) {
            "Исторические места" -> R.drawable.ic_marker_history
            "Храмы" -> R.drawable.ic_marker_church
            "Парки и скверы" -> R.drawable.ic_marker_park
            "Театры и кино" -> R.drawable.ic_marker_theater
            "Торговые центры" -> R.drawable.ic_marker_shopping
            "Рестораны и кафе" -> R.drawable.ic_marker_restaurant
            "Отели" -> R.drawable.ic_marker_hotel
            else -> R.drawable.ic_marker_default
        }
    }

    private fun checkMarkersIntegrity() {
        println("=== ПРОВЕРКА ЦЕЛОСТНОСТИ МАРКЕРОВ ===")
        println("Всего маркеров в коллекции: ${placeMarkers.size}")
        println("Всего слушателей: ${tapListeners.size}")

        var workingMarkers = 0
        var brokenMarkers = 0

        placeMarkers.forEach { (id, marker) ->
            try {
                val place = marker.userData as? Place
                if (place != null) {
                    println("✅ Маркер $id: ${place.name} - OK")
                    workingMarkers++
                } else {
                    println("❌ Маркер $id: НЕТ ДАННЫХ")
                    brokenMarkers++
                }
            } catch (e: Exception) {
                println("❌ Маркер $id: ОШИБКА - ${e.message}")
                brokenMarkers++
            }
        }

        println("=== КОНЕЦ ПРОВЕРКИ ===")
    }

    private fun showTargetPlace(placeId: Int) {
        val place = KurskPlacesData.places.find { it.id == placeId }
        place?.let {
            val point = Point(it.latitude, it.longitude)
            map.move(
                CameraPosition(point, 16.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1.5f),
                null
            )

            // Показываем информацию через небольшую задержку
            Handler(Looper.getMainLooper()).postDelayed({
                showPlaceInfo(it)
            }, 1500)
        }
    }

    private fun showPlaceInfo(place: Place) {
        try {
            println("Показываем информацию о: ${place.name}")

            selectedPlace = place

            runOnUiThread {
                // Обновляем UI в главном потоке
                tvInfoPlaceName.text = place.name
                tvInfoPlaceAddress.text = place.address

                cardPlaceInfo.visibility = View.VISIBLE

                // Анимация появления панели
                cardPlaceInfo.alpha = 0f
                cardPlaceInfo.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()


            }

        } catch (e: Exception) {
            println("Ошибка показа информации: ${e.message}")
            Toast.makeText(this, "Ошибка показа информации: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideInfoPanel() {
        cardPlaceInfo.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                cardPlaceInfo.visibility = View.GONE
                selectedPlace = null
            }
            .start()
    }

    private fun showAllPlaces() {
        // Возвращаемся к общему виду Курска
        val kurskCenter = Point(51.7373, 36.1873)
        map.move(
            CameraPosition(kurskCenter, 12.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )
        hideInfoPanel()
    }

    private fun requestLocationPermissionAndShow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            showUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showUserLocation() {
        try {
            val userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
            userLocationLayer.isVisible = true
            userLocationLayer.isHeadingEnabled = true

            Toast.makeText(this, "Определение местоположения...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка определения местоположения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openYandexMapsForRoute(place: Place) {
        try {
            val uri = "yandexmaps://maps.yandex.ru/?rtext=~${place.latitude},${place.longitude}&rtt=auto"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("ru.yandex.yandexmaps")
            startActivity(intent)
        } catch (e: Exception) {
            // Если Яндекс.Карты не установлены, открываем в браузере
            val uri = "https://yandex.ru/maps/?rtext=~${place.latitude},${place.longitude}&rtt=auto"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            } else {
                Toast.makeText(this, "Разрешение на геолокацию не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Реализация InputListener для обработки нажатий на карту
    override fun onMapTap(map: Map, point: Point) {
        // Только скрываем панель, не блокируем события маркеров
        hideInfoPanel()
    }

    override fun onMapLongTap(map: Map, point: Point) {
        // Долгое нажатие - пока ничего не делаем
    }

    override fun onStart() {
        super.onStart()
        try {
            MapKitFactory.getInstance().onStart()
            mapView.onStart()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска карты: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        try {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        } catch (e: Exception) {
            // Игнорируем ошибки остановки
        }
        super.onStop()
    }

    // Очистка ресурсов при закрытии активности
    override fun onDestroy() {
        try {
            placeMarkers.clear()
            tapListeners.clear()
            bitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            bitmapCache.clear()
        } catch (e: Exception) {
            println("Ошибка очистки ресурсов: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}