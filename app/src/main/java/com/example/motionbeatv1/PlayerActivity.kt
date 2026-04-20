package com.example.motionbeatv1

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class PlayerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var imgCover: ImageView
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

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var orientationSensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var lastTiltTime = 0L
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var filteredAcceleration = SensorManager.GRAVITY_EARTH
    private var lastShakeTime = 0L
    private var isDark = false
    private var resumeAfterUncover = false
    private var darkSinceMs = 0L
    private var ignoreLightUntil = 0L
    private var firstLightSample = true
    private val lightIgnoreMs = 1500L
    private val lightCoverMs = 3000L

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

        imgCover = findViewById(R.id.imgCover)
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
                if (fromUser) txtCurrentTime.text = formatTime(progress)
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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        updateSongInfo()
        updateProgressMax()
        updatePlayButton()
    }

    override fun onResume() {
        super.onResume()
        handler.post(progressRunnable)
        ignoreLightUntil = System.currentTimeMillis() + lightIgnoreMs
        firstLightSample = true
        darkSinceMs = 0L
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        orientationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(progressRunnable)
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleShake(event.values[0], event.values[1], event.values[2])
            }
            Sensor.TYPE_ORIENTATION -> {
                // values[2] = roll
                handleTiltFromOrientation(event.values[2])
            }
            Sensor.TYPE_LIGHT -> {
                handleLight(event.values[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun handleShake(x: Float, y: Float, z: Float) {
        val currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        lastAcceleration = currentAcceleration
        filteredAcceleration = filteredAcceleration * 0.9f + delta

        val now = System.currentTimeMillis()
        if (filteredAcceleration > 12f && now - lastShakeTime > 1000) {
            lastShakeTime = now
            PlayerManager.nextSong(this, true)
            updateSongInfo()
            updateProgressMax()
            updatePlayButton()
        }
    }

    private fun handleTiltFromOrientation(roll: Float) {
        val now = System.currentTimeMillis()
        if (now - lastTiltTime < 300) return

        // Direita ~45° -> aumenta
        if (roll < -35f) {
            increaseVolumeStep()
            lastTiltTime = now
        }
        // Esquerda ~45° -> diminui
        else if (roll > 35f) {
            decreaseVolumeStep()
            lastTiltTime = now
        }
    }

    private fun handleLight(lux: Float) {
        val now = System.currentTimeMillis()
        if (now < ignoreLightUntil) return

        val nowDark = lux <= 0.0f

        if (firstLightSample) {
            isDark = nowDark
            darkSinceMs = if (nowDark) now else 0L
            firstLightSample = false
            return
        }

        if (nowDark) {
            if (darkSinceMs == 0L) darkSinceMs = now
            val coveredConfirmed = now - darkSinceMs >= lightCoverMs
            if (coveredConfirmed && !isDark) {
                isDark = true
                resumeAfterUncover = PlayerManager.isPlaying()
                if (resumeAfterUncover) {
                    PlayerManager.playPause(this)
                    updatePlayButton()
                }
            }
        } else {
            darkSinceMs = 0L
            if (isDark) {
                isDark = false
                if (resumeAfterUncover && !PlayerManager.isPlaying()) {
                    PlayerManager.playPause(this)
                }
                resumeAfterUncover = false
                updatePlayButton()
            }
        }
    }

    private fun increaseVolumeStep() {
        val newVolume = (PlayerManager.getVolumePercent() + 4).coerceAtMost(100)
        PlayerManager.setVolumePercent(newVolume)
        seekVolume.progress = newVolume
    }

    private fun decreaseVolumeStep() {
        val newVolume = (PlayerManager.getVolumePercent() - 4).coerceAtLeast(0)
        PlayerManager.setVolumePercent(newVolume)
        seekVolume.progress = newVolume
    }

    private fun updateSongInfo() {
        val song = PlayerManager.getCurrentSong()
        val metadata = PlayerManager.readMetadata(this, song)

        txtSongTitle.text = metadata.title
        txtSongSubtitle.text = metadata.artist

        val artwork = PlayerManager.getArtwork(this, song)
        if (artwork != null) {
            imgCover.setImageBitmap(artwork)
        } else {
            imgCover.setImageResource(android.R.drawable.ic_menu_gallery)
        }
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
