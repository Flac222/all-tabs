package com.uade.alltabs.data.remote

import com.uade.alltabs.data.remote.dto.CoverArtResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface CoverArtArchiveApi {
    @GET("release/{mbid}")
    suspend fun getReleaseCoverArt(
        @Path("mbid") mbid: String
    ): CoverArtResponse

    @GET("release-group/{mbid}")
    suspend fun getReleaseGroupCoverArt(
        @Path("mbid") mbid: String
    ): CoverArtResponse
}
