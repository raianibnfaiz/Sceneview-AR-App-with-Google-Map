package com.raian.arfoodmenu.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
interface PlacesService {

    @GET("nearbysearch/json")
    fun nearbyPlaces(
        @Query("key") apiKey: String,
        @Query("location") location: String,
        @Query("radius") radiusInMeters: Int,
        @Query("type") placeType: String
    ): Call<NearbyPlacesResponse>

    companion object {
        private const val BASE_URL = "https://maps.googleapis.com/maps/api/place/"

        fun create(): PlacesService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(PlacesService::class.java)
        }
    }
}
