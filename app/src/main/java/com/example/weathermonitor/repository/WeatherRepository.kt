package com.example.weathermonitor.repository

import android.content.Context
import com.example.weathermonitor.ApiClient
import com.example.weathermonitor.R
import com.example.weathermonitor.model.DailySummary
import com.example.weathermonitor.model.DailyWeatherSummary
import com.example.weathermonitor.model.WeatherResponse
import retrofit2.Response
import java.util.*
import kotlin.collections.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WeatherRepository(private val context: Context) {

    private val api = ApiClient.api
    private val apiKey: String = context.getString(R.string.open_weather_map_api_key)

    // In-memory cache for storing weather data
    private val weatherDataCache = mutableListOf<WeatherResponse>()
    private val dailySummaries = HashMap<String, DailySummary>()

    private val historicalDailySummaries = mutableMapOf<String, DailyWeatherSummary>()

    // Simulate multiple days of weather data
    suspend fun simulateDailyWeatherUpdates(cities: List<String>, days: Int, interval: Long) {
        for (day in 1..days) {
            for (city in cities) {
                // Fetch and store weather data
                val weatherResponse = getWeather(city)
                weatherResponse?.let {
                    storeWeatherData(it)

                    // Store daily summary at the end of the day
                    val dailySummary = getCurrentDaySummary()
                    dailySummary?.let { summary ->
                        val dailyWeatherSummary = DailyWeatherSummary(
                            city = city,
                            averageTemperature = summary.getAverageTemperature(),
                            maxTemperature = summary.maxTemperature,
                            minTemperature = summary.minTemperature,
                            dominantWeather = summary.getDominantWeather()
                        )
                        historicalDailySummaries[getCurrentDay()] = dailyWeatherSummary
                    }
                }
            }
            delay(interval)  // Simulate delay between each day's data collection
        }
    }


    suspend fun getWeather(city: String): WeatherResponse? {
        return try {
            // Make the API call using the API key
            val response: Response<WeatherResponse> = api.getWeatherData(city, apiKey)
            if (response.isSuccessful) {
                response.body()?.also {
                    // Store weather data for rollup
                    storeWeatherData(it)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Function to continuously fetch weather data for the list of cities at regular intervals
    suspend fun getWeatherContinuously(
        cities: List<String>,
        interval: Long,
        onWeatherUpdate: (String, WeatherResponse?) -> Unit
    ) {
        while (true) {
            for (city in cities) {
                val weatherResponse = getWeather(city)
                onWeatherUpdate(city, weatherResponse)
            }
            delay(interval)  // Wait for the given interval before fetching again (e.g., every 5 minutes)
        }
    }

    // Store weather data and calculate rollups
    private fun storeWeatherData(weatherResponse: WeatherResponse) {
        weatherDataCache.add(weatherResponse)
        val currentDay = getCurrentDay()
        val dailySummary = dailySummaries[currentDay] ?: DailySummary()

        // Update the daily summary with current data
        dailySummary.updateWithNewData(weatherResponse)
        dailySummaries[currentDay] = dailySummary
    }

    private fun getCurrentDay(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    // Function to get the current day's summary
    fun getCurrentDaySummary(): DailySummary? {
        val currentDay = getCurrentDay()
        return dailySummaries[currentDay]
    }
    fun getHistoricalSummaries(): Map<String, DailyWeatherSummary> {
        return historicalDailySummaries
    }
}
