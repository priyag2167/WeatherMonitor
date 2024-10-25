package com.example.weathermonitor.model

data class DailySummary(
    var totalTemperature: Double = 0.0,
    var maxTemperature: Double = Double.MIN_VALUE,
    var minTemperature: Double = Double.MAX_VALUE,
    var count: Int = 0,
    private val weatherConditionCounts: MutableMap<String, Int> = mutableMapOf()
) {
    fun updateWithNewData(weatherResponse: WeatherResponse) {
        val tempCelsius = weatherResponse.main.temp
        totalTemperature += tempCelsius
        count++

        maxTemperature = maxOf(maxTemperature, tempCelsius)
        minTemperature = minOf(minTemperature, tempCelsius)

        // Track weather condition frequency for dominant condition
        val condition = weatherResponse.weather[0].main
        weatherConditionCounts[condition] = weatherConditionCounts.getOrDefault(condition, 0) + 1
    }

    fun getAverageTemperature(): Double {
        return if (count > 0) totalTemperature / count else 0.0
    }

    fun getDominantWeather(): String {
        return weatherConditionCounts.maxByOrNull { it.value }?.key ?: "Unknown"
    }
}
