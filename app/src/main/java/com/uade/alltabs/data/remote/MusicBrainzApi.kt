package com.uade.alltabs.data.remote

import com.uade.alltabs.data.remote.dto.MusicBrainzResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicBrainzApi {
    @GET("recording")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("fmt") format: String = "json"
    ): MusicBrainzResponse
}
