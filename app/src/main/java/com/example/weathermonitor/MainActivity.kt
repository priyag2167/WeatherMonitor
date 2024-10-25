package com.example.weathermonitor

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.weathermonitor.model.DailyWeatherSummary
import com.example.weathermonitor.repository.WeatherRepository
import com.example.weathermonitor.viewmodel.WeatherViewModel
import com.example.weathermonitor.viewmodel.WeatherViewModelFactory
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var thresholdEditText: EditText
    private lateinit var setThresholdButton: Button
    private lateinit var alertTextView: TextView
    private lateinit var humidityTextView: TextView
    private lateinit var windSpeedTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Adjust padding for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ProgressBar and SwipeRefreshLayout
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Get references for new UI elements
        humidityTextView = findViewById(R.id.humidityTextView)
        windSpeedTextView = findViewById(R.id.windSpeedTextView)

        // Initialize WeatherViewModel with the repository
        val repository = WeatherRepository(applicationContext)
        weatherViewModel = ViewModelProvider(
            this,
            WeatherViewModelFactory(repository)
        ).get(WeatherViewModel::class.java)

        // Get UI elements
        val cityEditText = findViewById<EditText>(R.id.cityEditText)
        val fetchButton = findViewById<Button>(R.id.fetchButton)
        val temperatureTextView = findViewById<TextView>(R.id.temperatureTextView)
        val conditionTextView = findViewById<TextView>(R.id.conditionTextView)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        val dailySummaryTextView = findViewById<TextView>(R.id.dailySummaryTextView)
        val defaultCityTextView = findViewById<TextView>(R.id.cityNameTextView)

        // Set default city to Delhi
        val defaultCity = "Delhi"
        defaultCityTextView.text = defaultCity
        progressBar.visibility = View.VISIBLE
        startWeatherDataFetch(defaultCity)

        // Set up the SwipeRefreshLayout listener
        swipeRefreshLayout.setOnRefreshListener {
            val cityName = cityEditText.text.toString().trim()
            if (cityName.isNotEmpty()) {
                startWeatherDataFetch(cityName)
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Set click listener on the fetch button
        fetchButton.setOnClickListener {
            val cityName = cityEditText.text.toString().trim()
            if (cityName.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                startWeatherDataFetch(cityName)
            } else {
                errorTextView.text = "Please enter a city name."
                errorTextView.visibility = TextView.VISIBLE
            }
        }

        // Observe the weather data and update the UI
        weatherViewModel.weatherData.observe(this, Observer { weatherMap ->
            weatherMap.forEach { (city, weatherResponse) ->
                weatherResponse?.let {
                    errorTextView.visibility = TextView.GONE
                    defaultCityTextView.text = city
                    val temperatureCelsius = weatherResponse.main.temp
                    val feelsLikeCelsius = weatherResponse.main.feels_like
                    val humidity = weatherResponse.main.humidity // Get humidity data
                    val windSpeed = weatherResponse.wind.speed // Get wind speed data

                    temperatureTextView.text = String.format(" %.1f °C\nFeels like: %.1f °C", temperatureCelsius, feelsLikeCelsius)
                    conditionTextView.text = "Condition: ${weatherResponse.weather[0].main}"
                    humidityTextView.text = "Humidity: $humidity%"
                    windSpeedTextView.text = "Wind Speed: $windSpeed m/s"

                    progressBar.visibility = View.GONE
                } ?: run {
                    errorTextView.text = "Could not retrieve weather data. Please try again."
                    errorTextView.visibility = TextView.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }
            swipeRefreshLayout.isRefreshing = false
        })

        // Add a new observer or UI element to show the historical summaries
        weatherViewModel.historicalDailySummaries.observe(this, Observer { summaries ->
            summaries?.forEach { (date, summary) ->
                dailySummaryTextView.append("\nSummary for $date:\n" +
                        "City: ${summary.city}\n" +
                        "Avg Temp: ${summary.averageTemperature} °C\n" +
                        "Max Temp: ${summary.maxTemperature} °C\n" +
                        "Min Temp: ${summary.minTemperature} °C\n" +
                        "Dominant Weather: ${summary.dominantWeather}\n"
                )
            }
        })


        // UI for alert thresholds
        thresholdEditText = findViewById(R.id.thresholdEditText)
        setThresholdButton = findViewById(R.id.setThresholdButton)
        alertTextView = findViewById(R.id.alertTextView)

        // Set default threshold value to 35
        thresholdEditText.setText("25")
        weatherViewModel.setAlertThresholds(mapOf(defaultCity to 25.0))
        alertTextView.text = "Alert threshold set for $defaultCity: 25 °C"

        setThresholdButton.setOnClickListener {
            val thresholdValue = thresholdEditText.text.toString().toDoubleOrNull()
            val cityName = cityEditText.text.toString().trim()
            if (thresholdValue != null && cityName.isNotEmpty()) {
                weatherViewModel.setAlertThresholds(mapOf(cityName to thresholdValue))
                alertTextView.text = "Alert threshold set for $cityName: $thresholdValue °C"
            } else {
                alertTextView.text = "Please enter a valid threshold and city name."
            }
        }

    }

    private fun startWeatherDataFetch(city: String) {
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                weatherViewModel.fetchWeatherContinuously(listOf(city), TimeUnit.MINUTES.toMillis(5))
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(5))
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
