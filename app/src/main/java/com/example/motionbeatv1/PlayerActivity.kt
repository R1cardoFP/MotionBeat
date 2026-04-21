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

    // --- Referencias dos elementos de interface do leitor
    private lateinit var imgCover: ImageView
    private lateinit var txtSongTitle: TextView
    private lateinit var txtSongSubtitle: TextView
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView

    // --- Botoes principais de controlo da reproducao
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrevious: ImageButton

    // --- Barras de progresso da musica e volume
    private lateinit var progressSong: SeekBar
    private lateinit var seekVolume: SeekBar

    // --- Estado de atualizacao periodica do ecran
    private val handler = Handler(Looper.getMainLooper())
    // --- Guarda ultimo indice mostrado para evitar trabalho repetido quando a musica nao mudou
    private var lastSongIndex = -1

    // --- Sensores e estado associado a gestos
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
    // --- Janela de ignorar leituras de luz imediatamente apos abrir o ecran
    private val lightIgnoreMs = 1500L
    // --- Tempo minimo em escuro continuo antes de assumir que o sensor foi tapado
    private val lightCoverMs = 3000L

    // --- Atualiza informacao visual do leitor de forma recorrente
    private val progressRunnable = object : Runnable {
        override fun run() {
            // --- Se a musica mudou desde o ultimo ciclo, atualiza capa/metadados e limites de progresso
            val currentIndex = PlayerManager.getCurrentSongIndex()
            if (currentIndex != lastSongIndex) {
                lastSongIndex = currentIndex
                updateSongInfo()
                updateProgressMax()
            }

            // --- Atualiza tempo corrente e duracao no ecra em intervalos curtos para fluidez
            val pos = PlayerManager.getCurrentPosition()
            val dur = PlayerManager.getDuration()
            progressSong.progress = pos
            txtCurrentTime.text = formatTime(pos)
            txtTotalTime.text = formatTime(dur)

            updatePlayButton()
            // --- Agenda novo ciclo de atualizacao
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // --- Liga variaveis Kotlin aos componentes do layout
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

        // --- Garante que existe uma musica inicial pronta a tocar
        if (PlayerManager.getCurrentSong() == null && MusicRepository.songs.isNotEmpty()) {
            PlayerManager.playSong(this, 0, false)
        }

        // --- Controlos de reproducao manual
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

        // --- Enquanto arrasta a barra, mostra preview do tempo; ao largar, reposiciona audio
        progressSong.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) txtCurrentTime.text = formatTime(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                PlayerManager.seekTo(seekBar?.progress ?: 0)
            }
        })

        // --- Barra de volume manual continua disponivel mesmo com controlo por inclinacao
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

        // --- Inicializa os sensores usados por gestos
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
        // --- Retoma ciclos de UI e sensores quando o utilizador volta ao ecran
        handler.post(progressRunnable)
        // --- Ignora leituras iniciais de luz para evitar pausas falsas ao abrir
        ignoreLightUntil = System.currentTimeMillis() + lightIgnoreMs
        firstLightSample = true
        darkSinceMs = 0L
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        orientationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        // --- Interrompe callbacks e sensores para evitar consumo de bateria em background
        handler.removeCallbacks(progressRunnable)
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // --- Garantia extra para remover callbacks caso a activity seja destruida
        handler.removeCallbacks(progressRunnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // --- Encaminha cada evento para o respetivo tratamento
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleShake(event.values[0], event.values[1], event.values[2])
            }
            Sensor.TYPE_ORIENTATION -> {
                // --- values[2] corresponde ao angulo de roll
                handleTiltFromOrientation(event.values[2])
            }
            Sensor.TYPE_LIGHT -> {
                handleLight(event.values[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- Deteccao de abanar para trocar para a proxima musica
    private fun handleShake(x: Float, y: Float, z: Float) {
        // --- Calcula aceleracao total e compara com gravidade para medir variacao brusca
        val currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        lastAcceleration = currentAcceleration
        // --- Filtro simples para reduzir ruido e evitar trocas por movimentos pequenos
        filteredAcceleration = filteredAcceleration * 0.9f + delta

        val now = System.currentTimeMillis()
        // --- Threshold + cooldown para impedir mudancas repetidas em poucos milissegundos
        if (filteredAcceleration > 12f && now - lastShakeTime > 1000) {
            lastShakeTime = now
            PlayerManager.nextSong(this, true)
            updateSongInfo()
            updateProgressMax()
            updatePlayButton()
        }
    }

    // --- Inclinacao: direita aumenta volume, esquerda diminui volume
    private fun handleTiltFromOrientation(roll: Float) {
        val now = System.currentTimeMillis()
        // --- Cooldown curto para evitar varios passos de volume no mesmo gesto
        if (now - lastTiltTime < 300) return

        // --- Direita ~45 graus -> aumenta volume
        if (roll < -35f) {
            increaseVolumeStep()
            lastTiltTime = now
        }
        // --- Esquerda ~45 graus -> diminui volume
        else if (roll > 35f) {
            decreaseVolumeStep()
            lastTiltTime = now
        }
    }

    // --- Sensor de luz: tapar 3 segundos pausa, destapar retoma
    private fun handleLight(lux: Float) {
        val now = System.currentTimeMillis()
        // --- Ignora leituras logo apos abrir o ecran para evitar pausas falsas
        if (now < ignoreLightUntil) return

        // --- Considera "escuro total" apenas quando lux chega a zero
        val nowDark = lux <= 0.0f

        // --- Primeira amostra apenas sincroniza estado atual sem acionar eventos
        if (firstLightSample) {
            isDark = nowDark
            darkSinceMs = if (nowDark) now else 0L
            firstLightSample = false
            return
        }

        if (nowDark) {
            if (darkSinceMs == 0L) darkSinceMs = now
            // --- So confirma "tapado" se ficar totalmente escuro durante 3 segundos seguidos
            val coveredConfirmed = now - darkSinceMs >= lightCoverMs
            if (coveredConfirmed && !isDark) {
                isDark = true
                // --- Guarda se estava a tocar para decidir retoma automatica ao destapar
                resumeAfterUncover = PlayerManager.isPlaying()
                if (resumeAfterUncover) {
                    PlayerManager.playPause(this)
                    updatePlayButton()
                }
            }
        } else {
            // --- Qualquer luz cancela contagem de escuro continuo
            darkSinceMs = 0L
            if (isDark) {
                isDark = false
                // --- Retoma apenas se a pausa anterior foi causada pelo sensor de luz
                if (resumeAfterUncover && !PlayerManager.isPlaying()) {
                    PlayerManager.playPause(this)
                }
                resumeAfterUncover = false
                updatePlayButton()
            }
        }
    }

    // --- Incrementa volume em passos fixos
    private fun increaseVolumeStep() {
        val newVolume = (PlayerManager.getVolumePercent() + 4).coerceAtMost(100)
        PlayerManager.setVolumePercent(newVolume)
        seekVolume.progress = newVolume
    }

    // --- Decrementa volume em passos fixos
    private fun decreaseVolumeStep() {
        val newVolume = (PlayerManager.getVolumePercent() - 4).coerceAtLeast(0)
        PlayerManager.setVolumePercent(newVolume)
        seekVolume.progress = newVolume
    }

    // --- Atualiza titulo, artista e capa da musica atual
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

    // --- Atualiza duracao total e limite da barra de progresso
    private fun updateProgressMax() {
        val dur = PlayerManager.getDuration()
        progressSong.max = if (dur > 0) dur else 1
        txtTotalTime.text = formatTime(dur)
    }

    // --- Atualiza icone de play/pause conforme o estado atual
    private fun updatePlayButton() {
        btnPlayPause.setImageResource(
            if (PlayerManager.isPlaying()) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    // --- Converte milissegundos para o formato mm:ss
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


