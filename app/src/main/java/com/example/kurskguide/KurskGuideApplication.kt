package com.example.kurskguide

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class KurskGuideApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Получаем API ключ из BuildConfig
            val apiKey = BuildConfig.YANDEX_MAPS_API_KEY

            if (apiKey.isNotEmpty() && apiKey != "PUT_YOUR_API_KEY_HERE") {
                MapKitFactory.setApiKey(apiKey)
                MapKitFactory.initialize(this)
                println("✅ MapKit успешно инициализирован")
            } else {
                println("❌ ОШИБКА: API ключ не найден! Добавьте YANDEX_MAPS_API_KEY в local.properties")
            }

        } catch (e: Exception) {
            println("❌ Ошибка инициализации MapKit: ${e.message}")
        }
    }
}