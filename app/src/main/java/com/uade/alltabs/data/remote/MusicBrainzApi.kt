package com.uade.alltabs.data.remote

import com.uade.alltabs.data.remote.dto.MusicBrainzResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

import retrofit2.http.Path
import com.uade.alltabs.data.remote.dto.RecordingDetailDto

interface MusicBrainzApi {
    @Headers("User-Agent: AllTabs/1.0.0 ( contact@alltabs.uade.edu.ar )")
    @GET("recording")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("fmt") format: String = "json"
    ): MusicBrainzResponse

    @Headers("User-Agent: AllTabs/1.0.0 ( contact@alltabs.uade.edu.ar )")
    @GET("recording/{mbid}?inc=artist-credits+releases+tags+genres+aliases+release-groups")
    suspend fun getRecordingDetail(
        @Path("mbid") mbid: String,
        @Query("fmt") format: String = "json"
    ): RecordingDetailDto
}
