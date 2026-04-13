package com.example.motionbeatv1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MusicListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_list)

        // evita sobrepor status bar
        val root = findViewById<View>(R.id.rootList)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(root.paddingLeft, bars.top, root.paddingRight, bars.bottom)
            insets
        }

        // botão inferior: PLAYER
        findViewById<View>(R.id.btnGoPlayer).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }


    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
