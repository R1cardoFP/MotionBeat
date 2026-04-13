package com.example.motionbeatv1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MusicListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

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

            val name = extractDisplayName(uri) ?: "Sem nome"
            MusicRepository.addDeviceSong(
                displayName = name,
                uriString = uri.toString(),
                artist = "Sem artista"
            )
        }
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

        listView = findViewById(R.id.listSongs)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        findViewById<View>(R.id.btnAddSongs).setOnClickListener {
            pickSongsLauncher.launch(arrayOf("audio/*"))
        }

        // BOTÃO DE NAVEGAÇÃO PARA O PLAYER
        findViewById<View>(R.id.btnGoPlayer).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (MusicRepository.removeSongAt(position)) {
                refreshList()
                Toast.makeText(this, "Música removida", Toast.LENGTH_SHORT).show()
            }
            true
        }

        refreshList()
    }

    private fun refreshList() {
        val names = MusicRepository.songs.map { it.title }
        adapter.clear()
        adapter.addAll(names)
        adapter.notifyDataSetChanged()
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
}
