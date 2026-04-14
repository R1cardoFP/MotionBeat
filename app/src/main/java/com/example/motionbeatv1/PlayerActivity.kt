package com.example.motionbeatv1


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var txtSongTitle: TextView
    private lateinit var txtSongSubtitle: TextView
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView

    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrevious: ImageButton

    private lateinit var progressSong: SeekBar
    private lateinit var seekVolume: SeekBar

    private val handler = Handler(Looper.getMainLooper())
    private var lastSongIndex = -1

    private val progressRunnable = object : Runnable {
        override fun run() {
            val currentIndex = PlayerManager.getCurrentSongIndex()
            if (currentIndex != lastSongIndex) {
                lastSongIndex = currentIndex
                updateSongInfo()
                updateProgressMax()
            }

            val pos = PlayerManager.getCurrentPosition()
            val dur = PlayerManager.getDuration()
            progressSong.progress = pos
            txtCurrentTime.text = formatTime(pos)
            txtTotalTime.text = formatTime(dur)

            updatePlayButton()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        txtSongTitle = findViewById(R.id.txtSongTitle)
        txtSongSubtitle = findViewById(R.id.txtSongSubtitle)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtTotalTime = findViewById(R.id.txtTotalTime)

        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)

        progressSong = findViewById(R.id.progressSong)
        seekVolume = findViewById(R.id.seekVolume)

        if (PlayerManager.getCurrentSong() == null && MusicRepository.songs.isNotEmpty()) {
            PlayerManager.playSong(this, 0, false)
        }

        btnPlayPause.setOnClickListener {
            PlayerManager.playPause(this)
            updatePlayButton()
        }

        btnNext.setOnClickListener {
            PlayerManager.nextSong(this, true)
            updateSongInfo()
            updateProgressMax()
            updatePlayButton()
        }

        btnPrevious.setOnClickListener {
            PlayerManager.previousSong(this, true)
            updateSongInfo()
            updateProgressMax()
            updatePlayButton()
        }

        progressSong.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    txtCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                PlayerManager.seekTo(seekBar?.progress ?: 0)
            }
        })

        seekVolume.progress = PlayerManager.getVolumePercent()
        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PlayerManager.setVolumePercent(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<View>(R.id.btnGoLibrary).setOnClickListener {
            startActivity(Intent(this, MusicListActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        updateSongInfo()
        updateProgressMax()
        updatePlayButton()
    }

    override fun onResume() {
        super.onResume()
        handler.post(progressRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(progressRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
    }

    private fun updateSongInfo() {
        val song = PlayerManager.getCurrentSong()
        txtSongTitle.text = song?.title ?: "Sem música"
        txtSongSubtitle.text = song?.artist ?: "Sem artista"
    }

    private fun updateProgressMax() {
        val dur = PlayerManager.getDuration()
        progressSong.max = if (dur > 0) dur else 1
        txtTotalTime.text = formatTime(dur)
    }

    private fun updatePlayButton() {
        btnPlayPause.setImageResource(
            if (PlayerManager.isPlaying()) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun formatTime(ms: Int): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
