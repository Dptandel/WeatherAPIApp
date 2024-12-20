package com.app.weatherapiapp

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.weatherapiapp.databinding.ActivityMainBinding
import com.app.weatherapiapp.models.WeatherApi
import com.app.weatherapiapp.retrofit.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// API - 720a39146e8c3e1464390248a6e9e8ab

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            fetchCurrentLocationWeather()
        } else {
            requestLocationPermission()
        }

        searchCity()
    }

    private fun searchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    fetchWeatherData(query)
                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }

        })
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationWeather() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val cityName = getCityName(location.latitude, location.longitude)
                if (cityName != null) {
                    fetchWeatherData(cityName)
                } else {
                    Toast.makeText(this, "Unable to determine city name.", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Unable to fetch location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = RetrofitClient.getService(this)

        val response =
            retrofit.getWeatherData(cityName, "720a39146e8c3e1464390248a6e9e8ab", "metric")

        response.enqueue(object : Callback<WeatherApi> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<WeatherApi>, response: Response<WeatherApi>) {
                response.body()?.let {
                    val temperature = it.main.temp.roundToInt()
                    val humidity = it.main.humidity
                    val windSpeed = it.wind.speed
                    val sunRise = it.sys.sunrise.toLong()
                    val sunSet = it.sys.sunset.toLong()
                    val seaLevel = it.main.pressure
                    val condition = it.weather.firstOrNull()?.main ?: "unknown"
                    val maxTemp = it.main.temp_max
                    val minTemp = it.main.temp_min

                    binding.temp.text = "$temperature"
                    binding.weather.text = condition
                    binding.maxTemp.text = "Max : $maxTemp °C"
                    binding.minTemp.text = "Min : $minTemp °C"
                    binding.humidity.text = "$humidity %"
                    binding.windSpeed.text = "$windSpeed m/s"
                    binding.sunRise.text = time(sunRise)
                    binding.sunSet.text = time(sunSet)
                    binding.sea.text = "$seaLevel hPa"
                    binding.condition.text = condition

                    binding.day.text = dayName(System.currentTimeMillis())
                    binding.date.text = date()
                    binding.cityName.text = cityName

                    changeBackgroundAccordingToWeather(condition)
                }
            }

            override fun onFailure(call: Call<WeatherApi>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error!!!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun changeBackgroundAccordingToWeather(condition: String) {
        when (condition) {
            "Clear Sky", "Sunny", "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
            }

            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy", "Smoke" -> {
                binding.root.setBackgroundResource(R.drawable.colud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
            }

            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
            }

            else -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun date(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    fun dayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCityName(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses?.isNotEmpty() == true) {
                addresses?.get(0)?.locality // City name
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocationWeather()
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}