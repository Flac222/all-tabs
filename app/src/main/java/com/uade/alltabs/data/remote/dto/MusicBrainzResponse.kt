package com.uade.alltabs.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MusicBrainzResponse(
    val created: String,
    val count: Int,
    val offset: Int,
    val recordings: List<RecordingDto>
)

data class RecordingDto(
    val id: String,
    val title: String,
    @SerializedName("artist-credit")
    val artistCredit: List<ArtistCreditDto>?,
    val releases: List<ReleaseDto>?
)

data class ArtistCreditDto(
    val name: String,
    val artist: ArtistDto?
)

data class ArtistDto(
    val id: String
)
