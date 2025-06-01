package com.example.kurskguide

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPlaceActivity : AppCompatActivity() {

    private lateinit var etPlaceName: EditText
    private lateinit var etPlaceDescription: EditText
    private lateinit var etPlaceFullDescription: EditText
    private lateinit var etPlaceAddress: EditText
    private lateinit var etPlacePhone: EditText
    private lateinit var etPlaceWebsite: EditText
    private lateinit var etPlaceWorkingHours: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var ratingBar: RatingBar
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDetectAddress: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCoordinates: TextView

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private val categories = arrayOf(
        "Пользовательские места",
        "Исторические места",
        "Парки и скверы",
        "Рестораны и кафе",
        "Торговые центры",
        "Театры и кино",
        "Отели",
        "Храмы",
        "Образование"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_place)

        // Получаем координаты
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        title = "Добавить место"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupUI()

        // Автоматически определяем адрес при открытии
        detectAddressAutomatically()
    }

    private fun initViews() {
        etPlaceName = findViewById(R.id.etPlaceName)
        etPlaceDescription = findViewById(R.id.etPlaceDescription)
        etPlaceFullDescription = findViewById(R.id.etPlaceFullDescription)
        etPlaceAddress = findViewById(R.id.etPlaceAddress)
        etPlacePhone = findViewById(R.id.etPlacePhone)
        etPlaceWebsite = findViewById(R.id.etPlaceWebsite)
        etPlaceWorkingHours = findViewById(R.id.etPlaceWorkingHours)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        ratingBar = findViewById(R.id.ratingBar)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnDetectAddress = findViewById(R.id.btnDetectAddress)
        progressBar = findViewById(R.id.progressBar)
        tvCoordinates = findViewById(R.id.tvCoordinates)
    }

    private fun setupUI() {
        // Настраиваем spinner категорий
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Показываем координаты
        tvCoordinates.text =
            "Координаты: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"

        btnSave.setOnClickListener {
            savePlace()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnDetectAddress.setOnClickListener {
            detectAddress()
        }
    }

    private fun detectAddressAutomatically() {
        // Автоматически определяем адрес при загрузке формы
        detectAddress()
    }

    private fun detectAddress() {
        showAddressLoading(true)

        lifecycleScope.launch {
            val result = GeocodingHelper.getAddressFromCoordinates(
                context = this@AddPlaceActivity,
                latitude = latitude,
                longitude = longitude
            )

            withContext(Dispatchers.Main) {
                showAddressLoading(false)

                if (result.isSuccess && result.address.isNotEmpty()) {
                    etPlaceAddress.setText(result.address)
                    Toast.makeText(this@AddPlaceActivity, "✅ Адрес определен", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = result.error ?: "Не удалось определить адрес"
                    Toast.makeText(this@AddPlaceActivity, "❌ $errorMessage", Toast.LENGTH_LONG).show()
                    etPlaceAddress.hint = "Введите адрес вручную"

                    // Показываем более детальную информацию в логах
                    println("Geocoding failed: ${result.error}")
                }
            }
        }
    }

    private fun showAddressLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnDetectAddress.isEnabled = false
            btnDetectAddress.text = "Определение..."
            etPlaceAddress.hint = "Определяется адрес..."
        } else {
            progressBar.visibility = View.GONE
            btnDetectAddress.isEnabled = true
            btnDetectAddress.text = "🎯 Определить адрес"
            if (etPlaceAddress.text.toString().isEmpty()) {
                etPlaceAddress.hint = "Адрес"
            }
        }
    }

    private fun savePlace() {
        val name = etPlaceName.text.toString().trim()
        val description = etPlaceDescription.text.toString().trim()
        val fullDescription = etPlaceFullDescription.text.toString().trim()
        val address = etPlaceAddress.text.toString().trim()
        val phone = etPlacePhone.text.toString().trim()
        val website = etPlaceWebsite.text.toString().trim()
        val workingHours = etPlaceWorkingHours.text.toString().trim()
        val category = categories[spinnerCategory.selectedItemPosition]
        val rating = ratingBar.rating

        // Валидация
        if (name.isEmpty()) {
            etPlaceName.error = "Введите название места"
            etPlaceName.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etPlaceDescription.error = "Введите краткое описание"
            etPlaceDescription.requestFocus()
            return
        }

        if (address.isEmpty()) {
            etPlaceAddress.error = "Введите адрес или нажмите 'Определить адрес'"
            etPlaceAddress.requestFocus()
            return
        }

        // Создаем новое место
        val newPlace = Place(
            id = System.currentTimeMillis().toInt(), // Уникальный ID на основе времени
            name = name,
            description = description,
            fullDescription = if (fullDescription.isNotEmpty()) fullDescription else description,
            category = category,
            address = address,
            phone = phone,
            website = website,
            workingHours = workingHours,
            imageUrl = "",
            latitude = latitude,
            longitude = longitude,
            rating = rating
        )

        // Сохраняем место
        if (KurskPlacesData.saveUserPlace(this, newPlace)) {
            Toast.makeText(this, "Место успешно добавлено!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Ошибка при сохранении места", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}