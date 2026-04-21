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

    // --- Lista visual de musicas no ecra de biblioteca
    private lateinit var listView: ListView

    // --- Lancador do seletor de documentos para importar multiplos audios
    private val pickSongsLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        // --- Percorre cada URI selecionado e tenta manter permissao de leitura apos reiniciar app
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            // --- Se nao houver metadados no ficheiro, usa o nome apresentado pelo sistema
            val fallbackName = extractDisplayName(uri) ?: "Sem nome"
            val metadata = PlayerManager.readMetadata(
                this,
                Song(
                    id = -1,
                    title = fallbackName,
                    subtitle = "",
                    artist = "Sem artista",
                    uriString = uri.toString()
                )
            )

            MusicRepository.addDeviceSong(
                displayName = metadata.title,
                uriString = uri.toString(),
                artist = metadata.artist
            )
        }

        SongStorage.saveSongs(this, MusicRepository.songs)
        refreshList()
        Toast.makeText(this, "Músicas adicionadas", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_list)

        // --- Aplica insets para evitar sobreposicao com barras do sistema
        val root = findViewById<View>(R.id.rootList)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(root.paddingLeft, bars.top, root.paddingRight, bars.bottom)
            insets
        }

        if (MusicRepository.songs.isEmpty()) {
            MusicRepository.replaceWith(SongStorage.loadSongs(this))
        }

        // --- Reescreve os dados visiveis com metadados reais lidos dos ficheiros
        enrichRepositoryMetadata()

        listView = findViewById(R.id.listSongs)

        listView.setOnItemClickListener { _, _, position, _ ->
            // --- Ao tocar num item, inicia essa faixa e abre o ecra do leitor
            PlayerManager.playSong(this, position, true)
            startActivity(Intent(this, PlayerActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            // --- Pressao longa remove musica da lista e persiste a alteracao
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

    // --- Rele metadados de cada musica e atualiza o repositorio
    private fun enrichRepositoryMetadata() {
        val updated = MusicRepository.songs.map { song ->
            val meta = PlayerManager.readMetadata(this, song)
            song.copy(
                title = meta.title,
                artist = meta.artist
            )
        }
        MusicRepository.replaceWith(updated)
        SongStorage.saveSongs(this, MusicRepository.songs)
    }

    // --- Reconstroi a lista visual com a musica atualmente selecionada
    private fun refreshList() {
        listView.adapter = SongListAdapter(
            this,
            MusicRepository.songs,
            PlayerManager.getCurrentSongIndex()
        )
    }

    // --- Extrai o nome apresentado pelo fornecedor de ficheiros
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


