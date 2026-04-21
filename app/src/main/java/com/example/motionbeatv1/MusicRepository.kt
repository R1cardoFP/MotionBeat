package com.example.motionbeatv1

import kotlin.collections.any
import kotlin.collections.indices
import kotlin.collections.maxOfOrNull
import kotlin.text.format

object MusicRepository {
    // --- Lista mutavel interna com as musicas disponiveis
    private val _songs = mutableListOf<Song>()

    // --- Exposicao publica da lista em modo de leitura
    val songs: List<Song>
        get() = _songs

    // --- Substitui o conteudo atual do repositorio
    fun replaceWith(songs: List<Song>) {
        _songs.clear()
        _songs.addAll(songs)
    }

    // --- Adiciona musica do dispositivo evitando duplicados por URI
    fun addDeviceSong(displayName: String, uriString: String, artist: String) {
        if (_songs.any { it.uriString == uriString }) return

        val newId = (_songs.maxOfOrNull { it.id } ?: 0) + 1
        val subtitle = String.format("%02d / DISPOSITIVO", _songs.size + 1)
        _songs.add(
            Song(
                id = newId,
                title = displayName,
                subtitle = subtitle,
                artist = artist,
                uriString = uriString
            )
        )
    }

    // --- Remove musica por indice e atualiza subtitulos
    fun removeSongAt(index: Int): Boolean {
        if (index !in _songs.indices) return false
        _songs.removeAt(index)
        renumberSubtitles()
        return true
    }

    // --- Recalcula numeracao dos subtitulos apos alteracoes
    private fun renumberSubtitles() {
        for (i in _songs.indices) {
            val current = _songs[i]
            _songs[i] = current.copy(subtitle = String.format("%02d / DISPOSITIVO", i + 1))
        }
    }
}


