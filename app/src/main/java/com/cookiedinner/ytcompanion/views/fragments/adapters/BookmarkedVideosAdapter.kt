package com.cookiedinner.ytcompanion.views.fragments.adapters

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.BookmarkedVideoBinding
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator

interface BookmarkedVideosAdapterInterface {
    fun downloadButtonPressed(bookmarkedVideo: BookmarkedVideo, pressedButton: MaterialButton)
    fun cardPressed(bookmarkedVideo: BookmarkedVideo)
}

class BookmarkedVideosAdapter(private val list: MutableList<BookmarkedVideo>, private val buttonInterface: BookmarkedVideosAdapterInterface, private val viewModel: MainActivityViewModel, private val lifecycleOwner: LifecycleOwner): RecyclerView.Adapter<BookmarkedVideosAdapter.ViewHolder>() {
    class ViewHolder(private val binding: BookmarkedVideoBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BookmarkedVideo, buttonInterface: BookmarkedVideosAdapterInterface, viewModel: MainActivityViewModel, lifecycleOwner: LifecycleOwner) {
            binding.videoTitle.text = item.title
            binding.videoChannel.text = item.channelName
            binding.downloadButton.setOnClickListener {
                buttonInterface.downloadButtonPressed(item, binding.downloadButton,)
            }
            binding.cardView.setOnClickListener {
                buttonInterface.cardPressed(item)
            }
            viewModel.liveDataUpdateProgressBar.observe(lifecycleOwner) {
                if (it.first.id == item.id) {
                    binding.progressBar.progress = it.second
                }
            }
            val imageByteArray = Base64.decode(item.thumbnail, Base64.DEFAULT)
            Glide.with(binding.thumbnailImageView.context)
                .load(imageByteArray)
                .placeholder(R.drawable.ic_baseline_image_24)
                .into(binding.thumbnailImageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BookmarkedVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position], buttonInterface, viewModel, lifecycleOwner)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}