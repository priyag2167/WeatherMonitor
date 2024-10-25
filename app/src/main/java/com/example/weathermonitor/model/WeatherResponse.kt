package com.example.weathermonitor.model

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind // Add this line for wind data
) {
    data class Main(
        val temp: Double,
        val feels_like: Double,
        val humidity: Int // Include humidity if not already added
    )

    data class Weather(
        val main: String
    )

    data class Wind(
        val speed: Double // Add this line for wind speed
    )
}
