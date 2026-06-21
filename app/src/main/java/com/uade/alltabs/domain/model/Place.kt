package com.uade.alltabs.domain.model

data class Place(
    val slots: MutableList<String?> = MutableList(6) { null }, // Index 0 (High e) to 5 (Low E)
    var isBarLine: Boolean = false,
    var isPalmMute: Boolean = false,
    val tapping: MutableList<Boolean> = MutableList(6) { false }
)
