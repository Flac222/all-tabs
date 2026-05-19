package com.uade.alltabs.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MusicBrainzApi {
    @GET("recording")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("fmt") format: String = "json"
    ): Any // To be typed later with proper DTO
}
