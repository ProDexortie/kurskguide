package com.example.kurskguide

import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Утилита для геокодирования адресов по координатам
 */
object GeocodingHelper {

    /**
     * Результат геокодирования
     */
    data class GeocodingResult(
        val address: String,
        val isSuccess: Boolean,
        val error: String? = null
    )

    /**
     * Определить адрес по координатам
     */
    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): GeocodingResult {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем наличие интернета
                if (!isNetworkAvailable(context)) {
                    return@withContext GeocodingResult(
                        address = "",
                        isSuccess = false,
                        error = "Нет подключения к интернету"
                    )
                }

                // Сначала пробуем Yandex Geocoder API (более точный для России)
                val yandexResult = getAddressFromYandex(latitude, longitude)
                if (yandexResult.isSuccess && yandexResult.address.isNotEmpty()) {
                    return@withContext yandexResult
                }

                // Если Yandex не сработал, пробуем Android Geocoder
                val androidResult = getAddressFromAndroidGeocoder(context, latitude, longitude)
                if (androidResult.isSuccess && androidResult.address.isNotEmpty()) {
                    return@withContext androidResult
                }

                // Если ничего не сработало
                return@withContext GeocodingResult(
                    address = "",
                    isSuccess = false,
                    error = "Не удалось определить адрес для данных координат"
                )

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext GeocodingResult(
                    address = "",
                    isSuccess = false,
                    error = "Ошибка при определении адреса: ${e.message}"
                )
            }
        }
    }

    /**
     * Получить адрес через Yandex Geocoder API
     */
    private suspend fun getAddressFromYandex(latitude: Double, longitude: Double): GeocodingResult {
        return withContext(Dispatchers.IO) {
            try {
                // Используем бесплатный Yandex Geocoder
                val url = "https://geocode-maps.yandex.ru/1.x/?format=json&geocode=$longitude,$latitude&results=1&lang=ru_RU"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val address = parseYandexGeocoderResponse(response)

                    if (address.isNotEmpty()) {
                        return@withContext GeocodingResult(
                            address = address,
                            isSuccess = true
                        )
                    }
                }

                return@withContext GeocodingResult(
                    address = "",
                    isSuccess = false,
                    error = "Yandex Geocoder не вернул результатов"
                )

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext GeocodingResult(
                    address = "",
                    isSuccess = false,
                    error = "Ошибка Yandex Geocoder: ${e.message}"
                )
            }
        }
    }

    /**
     * Парсинг ответа от Yandex Geocoder
     */
    private fun parseYandexGeocoderResponse(response: String): String {
        return try {
            val jsonObject = JSONObject(response)
            val featureCollection = jsonObject.getJSONObject("response")
                .getJSONObject("GeoObjectCollection")

            val featureMember = featureCollection.getJSONArray("featureMember")

            if (featureMember.length() > 0) {
                val firstFeature = featureMember.getJSONObject(0)
                val geoObject = firstFeature.getJSONObject("GeoObject")

                // Пробуем получить отформатированный адрес
                val metaData = geoObject.optJSONObject("metaDataProperty")
                    ?.optJSONObject("GeocoderMetaData")

                val formattedAddress = metaData?.optString("text", "")
                if (!formattedAddress.isNullOrEmpty()) {
                    return formatYandexAddress(formattedAddress)
                }

                // Если отформатированный адрес не найден, берем название
                val name = geoObject.optString("name", "")
                val description = geoObject.optString("description", "")

                return when {
                    name.isNotEmpty() && description.isNotEmpty() -> "$description, $name"
                    name.isNotEmpty() -> name
                    description.isNotEmpty() -> description
                    else -> ""
                }
            }

            ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Форматирование адреса от Yandex для лучшей читаемости
     */
    private fun formatYandexAddress(address: String): String {
        // Убираем "Россия, " в начале адреса для российских адресов
        val cleanAddress = address.removePrefix("Россия, ")

        // Если адрес содержит "Курская область", можно его упростить
        return if (cleanAddress.contains("Курская область")) {
            cleanAddress.replace("Курская область, ", "")
        } else {
            cleanAddress
        }
    }

    /**
     * Получить адрес через Android Geocoder
     */
    private suspend fun getAddressFromAndroidGeocoder(
        context: Context,
        latitude: Double,
        longitude: Double
    ): GeocodingResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext GeocodingResult(
                        address = "",
                        isSuccess = false,
                        error = "Geocoder недоступен на устройстве"
                    )
                }

                val geocoder = Geocoder(context, Locale("ru", "RU"))
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val formattedAddress = formatAndroidAddress(address)

                    if (formattedAddress.isNotEmpty()) {
                        return@withContext GeocodingResult(
                            address = formattedAddress,
                            isSuccess = true
                        )
                    }
                }

                return@withContext GeocodingResult(
                    address = "",
                    isSuccess = false,
                    error = "Android Geocoder не вернул результатов"
                )

            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext GeocodingResult(
                    address = "",
                    isSuccess = false,
                    error = "Ошибка Android Geocoder: ${e.message}"
                )
            }
        }
    }

    /**
     * Форматирование адреса от Android Geocoder
     */
    private fun formatAndroidAddress(address: android.location.Address): String {
        val addressParts = mutableListOf<String>()

        // Добавляем компоненты адреса в нужном порядке
        address.thoroughfare?.let { street ->
            address.subThoroughfare?.let { houseNumber ->
                addressParts.add("$street, $houseNumber")
            } ?: addressParts.add(street)
        }

        address.locality?.let { city ->
            if (city != "Курск" || addressParts.isEmpty()) {
                addressParts.add(city)
            }
        }

        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            // Если компоненты не найдены, используем полный адрес
            address.getAddressLine(0) ?: ""
        }
    }

    /**
     * Проверка доступности сети
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}