package com.example.weathermonitor.api

import com.example.weathermonitor.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getWeatherData(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"  // Default to metric (Celsius)
    ): Response<WeatherResponse>
}
