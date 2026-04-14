package com.example.motionbeatv1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.forEach
import kotlin.ranges.until
import kotlin.text.format
import kotlin.text.isNotBlank

object SongStorage {
    private const val PREF_NAME = "motionbeat_prefs"
    private const val KEY_SONGS = "saved_songs"

    fun saveSongs(context: Context, songs: List<Song>) {
        val arr = JSONArray()
        songs.forEach { song ->
            if (song.uriString != null) {
                val obj = JSONObject()
                obj.put("title", song.title)
                obj.put("artist", song.artist)
                obj.put("uri", song.uriString)
                arr.put(obj)
            }
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SONGS, arr.toString())
            .apply()
    }

    fun loadSongs(context: Context): List<Song> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SONGS, "[]")
            ?: "[]"

        val result = mutableListOf<Song>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val title = obj.optString("title", "MUSICA DISPOSITIVO")
            val artist = obj.optString("artist", "Sem artista")
            val uri = obj.optString("uri", "")
            if (uri.isNotBlank()) {
                result.add(
                    Song(
                        id = i + 1,
                        title = title,
                        subtitle = String.format("%02d / DISPOSITIVO", i + 1),
                        artist = artist,
                        uriString = uri
                    )
                )
            }
        }
        return result
    }
}
