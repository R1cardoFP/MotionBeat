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
    private val selectedIndex: Int = -1
) : BaseAdapter() {

    override fun getCount(): Int = songs.size

    override fun getItem(position: Int): Any = songs[position]

    override fun getItemId(position: Int): Long = songs[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_song, parent, false)
        val song = songs[position]

        val imgSong = view.findViewById<ImageView>(R.id.imgSong)
        val txtSubtitle = view.findViewById<TextView>(R.id.txtSongSubtitle)
        val txtTitle = view.findViewById<TextView>(R.id.txtSongTitle)
        val iconEnd = view.findViewById<ImageView>(R.id.iconEnd)

        txtSubtitle.text = song.subtitle
        txtTitle.text = song.title

        // Sem PlayerManager por agora
        imgSong.setImageResource(android.R.drawable.ic_media_play)

        if (position == selectedIndex) {
            iconEnd.setImageResource(android.R.drawable.ic_media_play)
        } else {
            iconEnd.setImageResource(android.R.drawable.ic_media_next)
        }

        return view
    }
}
