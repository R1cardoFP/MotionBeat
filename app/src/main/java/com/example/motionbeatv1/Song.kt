package com.example.motionbeatv1

// --- Modelo base de dados de uma musica na aplicacao
data class Song(
    val id: Int,
    val title: String,
    val subtitle: String,
    val artist: String = "Sem artista",
    val rawName: String? = null,
    val uriString: String? = null
)


