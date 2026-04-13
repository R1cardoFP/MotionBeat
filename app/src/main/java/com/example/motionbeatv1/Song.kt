package com.example.motionbeatv1

data class Song(
    val id: Int,
    val title: String,
    val subtitle: String,
    val artist: String = "Sem artista",
    val rawName: String? = null,
    val uriString: String? = null
)

