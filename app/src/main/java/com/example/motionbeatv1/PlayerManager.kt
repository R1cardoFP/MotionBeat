package com.example.motionbeatv1

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

object PlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex: Int = 0

    fun getCurrentSongIndex(): Int {
        if (MusicRepository.songs.isEmpty()) return -1
        return currentSongIndex.coerceIn(0, MusicRepository.songs.lastIndex)
    }

    fun getCurrentSong(): Song? {
        val i = getCurrentSongIndex()
        return if (i >= 0) MusicRepository.songs[i] else null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

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
            } else null
        } catch (_: Exception) {
            null
        }

        mediaPlayer?.setOnCompletionListener {
            nextSong(context, true)
        }

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
}
