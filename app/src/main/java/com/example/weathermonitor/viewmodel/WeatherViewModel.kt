package com.example.weathermonitor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermonitor.model.DailyWeatherSummary
import com.example.weathermonitor.model.WeatherResponse
import com.example.weathermonitor.repository.WeatherRepository
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private var alertThresholds: Map<String, Double> = mapOf()
    private var previousTemperatures: MutableMap<String, Double?> = mutableMapOf()
    private var alertTriggered: MutableMap<String, Boolean> = mutableMapOf()

    fun setAlertThresholds(thresholds: Map<String, Double>) {
        alertThresholds = thresholds
    }

    private fun checkForAlerts(city: String, weatherResponse: WeatherResponse) {
        val tempCelsius = weatherResponse.main.temp
        val threshold = alertThresholds[city] ?: Double.MAX_VALUE

        val previousTemp = previousTemperatures[city]

        if (previousTemp != null && previousTemp > threshold && tempCelsius > threshold) {
            if (alertTriggered[city] != true) {
                triggerAlert(city, tempCelsius)
                alertTriggered[city] = true
            }
        } else {
            alertTriggered[city] = false
        }

        previousTemperatures[city] = tempCelsius
    }

    private fun triggerAlert(city: String, temperature: Double) {
        println("ALERT: Temperature in $city has exceeded the threshold for two consecutive updates. Current temperature: $temperature °C")
    }

    private val _weatherData = MutableLiveData<Map<String, WeatherResponse?>>()
    val weatherData: LiveData<Map<String, WeatherResponse?>> = _weatherData

    private val _dailySummary = MutableLiveData<DailyWeatherSummary?>()
    val dailySummary: LiveData<DailyWeatherSummary?> = _dailySummary

    private val _historicalDailySummaries = MutableLiveData<Map<String, DailyWeatherSummary>>()
    val historicalDailySummaries: LiveData<Map<String, DailyWeatherSummary>> = _historicalDailySummaries

    private var cityWeatherData = mutableMapOf<String, MutableList<WeatherResponse>>()

    fun fetchWeatherContinuously(cities: List<String>, interval: Long) {
        viewModelScope.launch {
            repository.getWeatherContinuously(cities, interval) { city, weatherResponse ->
                weatherResponse?.let {
                    cityWeatherData[city] = cityWeatherData.getOrDefault(city, mutableListOf()).apply {
                        add(it)
                    }
                    calculateDailySummary(city)
                    checkForAlerts(city, it)
                }
                _weatherData.value = cityWeatherData.mapValues { it.value.lastOrNull() }
            }
        }
    }

    private fun calculateDailySummary(city: String) {
        val data = cityWeatherData[city] ?: return
        if (data.isEmpty()) return

        val avgTemp = data.sumOf { it.main.temp } / data.size
        val maxTemp = data.maxByOrNull { it.main.temp }?.main?.temp ?: 0.0
        val minTemp = data.minByOrNull { it.main.temp }?.main?.temp ?: 0.0
        val dominantWeather = data.groupBy { it.weather[0].main }.maxByOrNull { it.value.size }?.key ?: "Unknown"

        val summary = DailyWeatherSummary(
            city = city,
            averageTemperature = avgTemp,
            maxTemperature = maxTemp,
            minTemperature = minTemp,
            dominantWeather = dominantWeather
        )

        _dailySummary.postValue(summary)
        _historicalDailySummaries.value = _historicalDailySummaries.value?.toMutableMap()?.apply {
            this[city] = summary
        } ?: mapOf(city to summary)
    }
}
