package com.example.motionbeatv1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MusicListActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    private val pickSongsLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val fallbackName = extractDisplayName(uri) ?: "Sem nome"

            MusicRepository.addDeviceSong(
                displayName = fallbackName,
                uriString = uri.toString(),
                artist = "Sem artista"
            )
        }

        SongStorage.saveSongs(this, MusicRepository.songs)
        refreshList()
        Toast.makeText(this, "Músicas adicionadas", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_list)

        val root = findViewById<View>(R.id.rootList)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(root.paddingLeft, bars.top, root.paddingRight, bars.bottom)
            insets
        }

        if (MusicRepository.songs.isEmpty()) {
            MusicRepository.replaceWith(SongStorage.loadSongs(this))
        }

        listView = findViewById(R.id.listSongs)

        listView.setOnItemClickListener { _, _, _, _ ->
            startActivity(Intent(this, PlayerActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (MusicRepository.removeSongAt(position)) {
                SongStorage.saveSongs(this, MusicRepository.songs)
                refreshList()
                Toast.makeText(this, "Música removida", Toast.LENGTH_SHORT).show()
            }
            true
        }

        findViewById<View>(R.id.btnAddSongs).setOnClickListener {
            pickSongsLauncher.launch(arrayOf("audio/*"))
        }

        findViewById<View>(R.id.btnGoPlayer).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        listView.adapter = SongListAdapter(
            this,
            MusicRepository.songs,
            -1
        )
    }

    private fun extractDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return null
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
