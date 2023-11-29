package com.raian.arfoodmenu.api

import com.google.gson.annotations.SerializedName
import com.raian.arfoodmenu.model.Place

data class NearbyPlacesResponse(
    @SerializedName("results") val results: List<Place>
)
