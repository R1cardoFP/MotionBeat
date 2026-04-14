package com.example.motionbeatv1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var txtSongTitle: TextView
    private lateinit var txtSongSubtitle: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrevious: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        txtSongTitle = findViewById(R.id.txtSongTitle)
        txtSongSubtitle = findViewById(R.id.txtSongSubtitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)

        // Se ainda não houver player ativo, prepara a primeira música (sem autoplay)
        if (PlayerManager.getCurrentSong() == null && MusicRepository.songs.isNotEmpty()) {
            PlayerManager.playSong(this, 0, false)
        }

        btnPlayPause.setOnClickListener {
            PlayerManager.playPause()
            updatePlayButton()
        }

        btnNext.setOnClickListener {
            PlayerManager.nextSong(this, true)
            updateSongInfo()
            updatePlayButton()
        }

        btnPrevious.setOnClickListener {
            PlayerManager.previousSong(this, true)
            updateSongInfo()
            updatePlayButton()
        }

        findViewById<android.view.View>(R.id.btnGoLibrary).setOnClickListener {
            startActivity(Intent(this, MusicListActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        updateSongInfo()
        updatePlayButton()
    }

    override fun onResume() {
        super.onResume()
        updateSongInfo()
        updatePlayButton()
    }

    private fun updateSongInfo() {
        val song = PlayerManager.getCurrentSong()
        txtSongTitle.text = song?.title ?: "Sem música"
        txtSongSubtitle.text = song?.artist ?: "Sem artista"
    }

    private fun updatePlayButton() {
        val icon = if (PlayerManager.isPlaying()) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        btnPlayPause.setImageResource(icon)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
