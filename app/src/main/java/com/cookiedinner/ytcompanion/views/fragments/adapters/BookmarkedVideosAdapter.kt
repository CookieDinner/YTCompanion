package com.cookiedinner.ytcompanion.views.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cookiedinner.ytcompanion.databinding.BookmarkedVideoBinding
import com.cookiedinner.ytcompanion.models.BookmarkedVideo

class BookmarkedVideosAdapter(var list: ArrayList<BookmarkedVideo>): RecyclerView.Adapter<BookmarkedVideosAdapter.ViewHolder>() {

    class ViewHolder(private val binding: BookmarkedVideoBinding): RecyclerView.ViewHolder(binding.root) {
        lateinit var textView: TextView

        fun bind(item: BookmarkedVideo) {
            binding.videoTitle.text = item.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BookmarkedVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}