package com.app.weatherapiapp.retrofit

import com.app.weatherapiapp.models.WeatherApi
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("weather")
    fun getWeatherData(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ) : Call<WeatherApi>
}