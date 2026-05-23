package com.uade.alltabs.data.remote

import com.uade.alltabs.data.remote.dto.MusicBrainzResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface MusicBrainzApi {
    @Headers("User-Agent: AllTabs/1.0.0 ( contact@alltabs.uade.edu.ar )")
    @GET("recording")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("fmt") format: String = "json"
    ): MusicBrainzResponse
}
