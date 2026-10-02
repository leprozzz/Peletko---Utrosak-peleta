package com.example.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WeatherResult(
    val avgTemp: Double,
    val t08: Double? = null,
    val t13: Double? = null,
    val t20: Double? = null,
    val cityName: String = ""
)

object WeatherService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val knownCities = mapOf(
        "sarajevo" to Pair(43.8563, 18.4131),
        "banja luka" to Pair(44.7722, 17.1910),
        "tuzla" to Pair(44.5384, 18.6671),
        "mostar" to Pair(43.3438, 17.8078),
        "zenica" to Pair(44.2017, 17.9040),
        "bihać" to Pair(44.8169, 15.8708),
        "bihac" to Pair(44.8169, 15.8708),
        "bijeljina" to Pair(44.7587, 19.2144),
        "brčko" to Pair(44.8728, 18.8106),
        "brcko" to Pair(44.8728, 18.8106),
        "trebinje" to Pair(42.7119, 18.3436),
        "doboj" to Pair(44.7333, 18.0833),
        "prijedor" to Pair(44.9797, 16.7140),
        "beograd" to Pair(44.7866, 20.4489),
        "novi sad" to Pair(45.2671, 19.8335),
        "niš" to Pair(43.3209, 21.8958),
        "nis" to Pair(43.3209, 21.8958),
        "kragujevac" to Pair(44.0128, 20.9114),
        "zagreb" to Pair(45.8150, 15.9819),
        "split" to Pair(43.5147, 16.4435),
        "rijeka" to Pair(45.3271, 14.4422),
        "osijek" to Pair(45.5550, 18.6955),
        "podgorica" to Pair(42.4304, 19.2594),
        "nikšić" to Pair(42.7731, 18.9445),
        "niksic" to Pair(42.7731, 18.9445),
        "ljubljana" to Pair(46.0569, 14.5058),
        "skopje" to Pair(41.9981, 21.4254)
    )

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, WeatherResult>>()

    suspend fun fetchWeather(cityName: String, dateISO: String, forceFresh: Boolean = false): WeatherResult? = withContext(Dispatchers.IO) {
        val targetCity = cityName.ifBlank { "Sarajevo" }
        val cacheKey = "${targetCity.lowercase().trim()}_$dateISO"
        val now = System.currentTimeMillis()

        if (!forceFresh) {
            val cached = cache[cacheKey]
            if (cached != null) {
                // Za datume iz prošlosti podatak je trajan; za danas keširaj 15 minuta
                val isPastDate = dateISO < com.example.util.PelletCalculator.getTodayISO()
                if (isPastDate || (now - cached.first < 15 * 60 * 1000L)) {
                    return@withContext cached.second
                }
            }
        }

        val coords = resolveCoordinates(targetCity) ?: Pair(43.8563, 18.4131)
        val lat = coords.first
        val lon = coords.second

        // 1. Pokušaj prvo Open-Meteo Forecast API
        val forecastUrl = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&hourly=temperature_2m&daily=temperature_2m_mean" +
                "&timezone=auto&start_date=$dateISO&end_date=$dateISO"

        val res = executeWeatherQuery(forecastUrl, targetCity)
        if (res != null) {
            cache[cacheKey] = Pair(now, res)
            return@withContext res
        }

        // 2. Ako je datum iz prošlosti ili forecast nije vratio podatak, pozovi Open-Meteo Archive API
        val archiveUrl = "https://archive-api.open-meteo.com/v1/archive?" +
                "latitude=$lat&longitude=$lon" +
                "&hourly=temperature_2m&daily=temperature_2m_mean" +
                "&timezone=auto&start_date=$dateISO&end_date=$dateISO"

        val archiveRes = executeWeatherQuery(archiveUrl, targetCity)
        if (archiveRes != null) {
            cache[cacheKey] = Pair(now, archiveRes)
        }
        return@withContext archiveRes
    }

    private fun executeWeatherQuery(url: String, cityName: String): WeatherResult? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UtrosakPeletaApp/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)

                // 1. Dnevna prosječna temperatura iz daily sekcije
                var avgTemp: Double? = null
                if (json.has("daily")) {
                    val daily = json.getJSONObject("daily")
                    if (daily.has("temperature_2m_mean")) {
                        val arr = daily.getJSONArray("temperature_2m_mean")
                        if (arr.length() > 0 && !arr.isNull(0)) {
                            avgTemp = arr.getDouble(0)
                        }
                    }
                }

                // 2. Satne temperature (08h, 13h, 20h i cjelodnevni prosjek)
                var t08: Double? = null
                var t13: Double? = null
                var t20: Double? = null
                val allHourlyTemps = mutableListOf<Double>()

                if (json.has("hourly")) {
                    val hourly = json.getJSONObject("hourly")
                    val times = hourly.getJSONArray("time")
                    val temps = hourly.getJSONArray("temperature_2m")

                    for (i in 0 until times.length()) {
                        val timeStr = times.getString(i)
                        val tempVal = if (!temps.isNull(i)) temps.getDouble(i) else null
                        if (tempVal != null) {
                            allHourlyTemps.add(tempVal)
                            if (timeStr.endsWith("T08:00")) t08 = tempVal
                            if (timeStr.endsWith("T13:00")) t13 = tempVal
                            if (timeStr.endsWith("T20:00")) t20 = tempVal
                        }
                    }
                }

                val hourlyAverage = if (allHourlyTemps.isNotEmpty()) allHourlyTemps.average() else null
                val finalAvg = avgTemp ?: hourlyAverage ?: listOfNotNull(t08, t13, t20).average().takeIf { !it.isNaN() } ?: return null

                WeatherResult(
                    avgTemp = (finalAvg * 10).toInt() / 10.0,
                    t08 = t08?.let { (it * 10).toInt() / 10.0 },
                    t13 = t13?.let { (it * 10).toInt() / 10.0 },
                    t20 = t20?.let { (it * 10).toInt() / 10.0 },
                    cityName = cityName
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveCoordinates(city: String): Pair<Double, Double>? {
        val clean = city.trim().lowercase()
        knownCities[clean]?.let { return it }

        // Pokušaj pronaći dio naziva u poznatim gradovima
        for ((name, coords) in knownCities) {
            if (clean.contains(name) || name.contains(clean)) {
                return coords
            }
        }

        // Ako nije u listi, pozovi Open-Meteo Geocoding
        return try {
            val encoded = URLEncoder.encode(city, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return knownCities["sarajevo"] ?: Pair(43.8563, 18.4131)
                val body = response.body?.string() ?: return knownCities["sarajevo"] ?: Pair(43.8563, 18.4131)
                val json = JSONObject(body)
                if (json.has("results")) {
                    val res = json.getJSONArray("results")
                    if (res.length() > 0) {
                        val first = res.getJSONObject(0)
                        val lat = first.getDouble("latitude")
                        val lon = first.getDouble("longitude")
                        return Pair(lat, lon)
                    }
                }
                knownCities["sarajevo"] ?: Pair(43.8563, 18.4131)
            }
        } catch (_: Exception) {
            // Default na Sarajevo kao pouzdan fallback
            knownCities["sarajevo"] ?: Pair(43.8563, 18.4131)
        }
    }
}
