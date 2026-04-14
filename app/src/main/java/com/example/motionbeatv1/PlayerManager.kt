package com.example.motionbeatv1

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

object PlayerManager {

    data class TrackMetadata(
        val title: String,
        val artist: String
    )

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex: Int = 0
    private var volume: Float = 0.5f

    fun getCurrentSongIndex(): Int {
        if (MusicRepository.songs.isEmpty()) return -1
        return currentSongIndex.coerceIn(0, MusicRepository.songs.lastIndex)
    }

    fun getCurrentSong(): Song? {
        val i = getCurrentSongIndex()
        return if (i >= 0) MusicRepository.songs[i] else null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position.coerceAtLeast(0))
    }

    fun getVolumePercent(): Int = (volume * 100f).toInt()

    fun setVolumePercent(percent: Int) {
        volume = (percent.coerceIn(0, 100) / 100f)
        mediaPlayer?.setVolume(volume, volume)
    }

    fun playSong(context: Context, index: Int, autoPlay: Boolean) {
        if (MusicRepository.songs.isEmpty()) return

        val safeIndex = index.coerceIn(0, MusicRepository.songs.lastIndex)
        currentSongIndex = safeIndex
        val song = MusicRepository.songs[safeIndex]

        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = try {
            if (song.uriString != null) {
                MediaPlayer().apply {
                    setDataSource(context.applicationContext, Uri.parse(song.uriString))
                    prepare()
                }
            } else if (song.rawName != null) {
                val resId = context.resources.getIdentifier(song.rawName, "raw", context.packageName)
                if (resId != 0) MediaPlayer.create(context.applicationContext, resId) else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Erro a abrir música: ${song.uriString}", e)
            null
        }

        mediaPlayer?.setVolume(volume, volume)
        mediaPlayer?.setOnCompletionListener { nextSong(context, true) }

        if (autoPlay) mediaPlayer?.start()
    }

    fun playPause(context: Context) {
        if (mediaPlayer == null) {
            if (MusicRepository.songs.isNotEmpty()) {
                playSong(context, getCurrentSongIndex().coerceAtLeast(0), true)
            }
            return
        }

        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
        }
    }

    fun nextSong(context: Context, autoPlay: Boolean) {
        if (MusicRepository.songs.isEmpty()) return
        val next = (getCurrentSongIndex() + 1) % MusicRepository.songs.size
        playSong(context, next, autoPlay)
    }

    fun previousSong(context: Context, autoPlay: Boolean) {
        if (MusicRepository.songs.isEmpty()) return
        val current = getCurrentSongIndex().coerceAtLeast(0)
        val prev = if (current == 0) MusicRepository.songs.lastIndex else current - 1
        playSong(context, prev, autoPlay)
    }

    fun readMetadata(context: Context, song: Song?): TrackMetadata {
        if (song == null) return TrackMetadata("Sem nome", "Sem artista")

        val fallbackTitle = song.title.ifBlank { "Sem nome" }
        val fallbackArtist = song.artist.ifBlank { "Sem artista" }
        val uri = songToUri(context, song) ?: return TrackMetadata(fallbackTitle, fallbackArtist)

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: fallbackArtist
            TrackMetadata(title, artist)
        } catch (_: Exception) {
            TrackMetadata(fallbackTitle, fallbackArtist)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun getArtwork(context: Context, song: Song?): Bitmap? {
        if (song == null) return null
        val uri = songToUri(context, song) ?: return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun songToUri(context: Context, song: Song): Uri? {
        return when {
            song.uriString != null -> Uri.parse(song.uriString)
            song.rawName != null -> {
                val resId = context.resources.getIdentifier(song.rawName, "raw", context.packageName)
                if (resId == 0) null
                else Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId")
            }
            else -> null
        }
    }
}
