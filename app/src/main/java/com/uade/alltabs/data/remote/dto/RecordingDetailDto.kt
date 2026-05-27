package com.uade.alltabs.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RecordingDetailDto(
    val id: String,
    val title: String,
    @SerializedName("artist-credit")
    val artistCredit: List<ArtistCreditDto>?,
    val releases: List<ReleaseDto>?,
    val tags: List<TagDto>?,
    val genres: List<GenreDto>?,
    val aliases: List<AliasDto>?
)

data class ReleaseDto(
    val id: String,
    val title: String,
    @SerializedName("release-events")
    val releaseEvents: List<ReleaseEventDto>?,
    @SerializedName("release-group")
    val releaseGroup: ReleaseGroupDto?
)

data class ReleaseGroupDto(
    val id: String,
    val title: String,
    @SerializedName("primary-type")
    val primaryType: String?
)

data class ReleaseEventDto(
    val date: String?,
    val area: AreaDto?
)

data class AreaDto(
    val id: String,
    val name: String
)

data class TagDto(
    val count: Int,
    val name: String
)

data class GenreDto(
    val id: String,
    val name: String
)

data class AliasDto(
    val name: String,
    @SerializedName("sort-name")
    val sortName: String
)
