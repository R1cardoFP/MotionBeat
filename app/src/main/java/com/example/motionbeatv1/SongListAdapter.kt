package com.example.motionbeatv1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class SongListAdapter(
    private val context: Context,
    private val songs: List<Song>,
    private val selectedIndex: Int
) : BaseAdapter() {

    // --- Numero total de elementos na lista
    override fun getCount(): Int = songs.size

    // --- Devolve item por posicao
    override fun getItem(position: Int): Any = songs[position]

    // --- ID estavel para o item da lista
    override fun getItemId(position: Int): Long = songs[position].id.toLong()

    // --- Cria/recicla cada linha e aplica estado visual
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_song, parent, false)
        val song = songs[position]

        val imgSong = view.findViewById<ImageView>(R.id.imgSong)
        val txtSubtitle = view.findViewById<TextView>(R.id.txtSongSubtitle)
        val txtTitle = view.findViewById<TextView>(R.id.txtSongTitle)
        val iconEnd = view.findViewById<ImageView>(R.id.iconEnd)

        val isCurrent = position == selectedIndex

        txtSubtitle.text = if (isCurrent) {
            String.format("%02d / ATUAL", position + 1)
        } else {
            String.format("%02d / ARQUIVO", position + 1)
        }

        txtTitle.text = song.title

        // --- Carrega capa da musica para a lista
        val artwork = PlayerManager.getArtwork(context, song)
        if (artwork != null) {
            imgSong.setImageBitmap(artwork)
        } else {
            imgSong.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        if (isCurrent && PlayerManager.isPlaying()) {
            iconEnd.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
        } else {
            iconEnd.setImageResource(android.R.drawable.ic_media_next)
        }

        return view
    }
}


