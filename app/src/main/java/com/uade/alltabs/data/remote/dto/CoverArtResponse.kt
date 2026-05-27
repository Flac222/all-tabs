package com.uade.alltabs.data.remote.dto

data class CoverArtResponse(
    val images: List<CoverArtImageDto>
)

data class CoverArtImageDto(
    val image: String,
    val thumbnails: ThumbnailsDto,
    val front: Boolean,
    val primary: Boolean
)

data class ThumbnailsDto(
    val large: String,
    val small: String,
    val middle: String? = null
)
