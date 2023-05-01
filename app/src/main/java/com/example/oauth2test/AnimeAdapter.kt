package com.example.oauth2test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.oauth2test.databinding.AnimeItemBinding
import com.squareup.picasso.Picasso

class AnimeAdapter(private var animeItems: List<Anime>): RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {
    inner class AnimeViewHolder(val binding: AnimeItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeViewHolder {
        val layout = LayoutInflater.from(parent.context)
        val binding = AnimeItemBinding.inflate(layout, parent, false)
        return AnimeViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return animeItems.size
    }

    override fun onBindViewHolder(holder: AnimeViewHolder, position: Int) {
        // More info: https://square.github.io/picasso/
        Picasso.get()
            .load(animeItems[position].url)
            .into(holder.binding.imageView);
    }
}