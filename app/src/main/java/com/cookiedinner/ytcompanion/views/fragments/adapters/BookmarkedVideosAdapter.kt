package com.cookiedinner.ytcompanion.views.fragments.adapters

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cookiedinner.ytcompanion.databinding.BookmarkedVideoBinding
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel

interface BookmarkedVideosAdapterInterface {
    fun downloadButtonPressed(id: Int)
    fun deleteButtonPressed(id: Int)
}

class BookmarkedVideosAdapter(private val list: MutableList<BookmarkedVideo>, private val buttonInferface: BookmarkedVideosAdapterInterface): RecyclerView.Adapter<BookmarkedVideosAdapter.ViewHolder>() {
    class ViewHolder(private val binding: BookmarkedVideoBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BookmarkedVideo, buttonInferface: BookmarkedVideosAdapterInterface) {
            binding.videoTitle.text = item.title
            binding.videoChannel.text = item.channelName
            binding.deleteButton.setOnClickListener {
                buttonInferface.deleteButtonPressed(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BookmarkedVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position], buttonInferface)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}