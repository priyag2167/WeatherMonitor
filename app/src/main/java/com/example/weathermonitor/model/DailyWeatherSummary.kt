package com.example.weathermonitor.model

data class DailyWeatherSummary(
    val city: String,
    val averageTemperature: Double,
    val maxTemperature: Double,
    val minTemperature: Double,
    val dominantWeather: String
)
