package com.wechantloup.facedetection.album

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wechantloup.facedetection.R
import com.wechantloup.facedetection.databinding.ItemAlbumLayoutBinding
import com.wechantloup.facedetection.inflate
import com.wechantloup.facedetection.provider.PhotoAlbum

class AlbumsAdapter(
    val onAlbumClicked: (String, String) -> Unit
): ListAdapter<PhotoAlbum, AlbumsAdapter.AlbumHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(parent)
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlbumHolder(parent: ViewGroup): RecyclerView.ViewHolder(parent.inflate(R.layout.item_album_layout)) {

        private val binding = ItemAlbumLayoutBinding.bind(itemView)

        fun bind(item: PhotoAlbum) {
            binding.tvTitle.text = item.title

            Glide.with(itemView.context)
                .load(item.coverPhotoPath.first())
                .into(binding.ivCover)

            if (item.coverPhotoPath.size > 1) {
                Glide.with(itemView.context)
                    .load(item.coverPhotoPath[1])
                    .into(binding.ivThumbnail1)
            }

            if (item.coverPhotoPath.size > 2) {
                Glide.with(itemView.context)
                    .load(item.coverPhotoPath[2])
                    .into(binding.ivThumbnail2)
            }

            if (item.coverPhotoPath.size > 3) {
                Glide.with(itemView.context)
                    .load(item.coverPhotoPath[3])
                    .into(binding.ivThumbnail3)
            }

            itemView.setOnClickListener { onAlbumClicked(item.id, item.title) }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<PhotoAlbum>() {
            override fun areItemsTheSame(oldItem: PhotoAlbum, newItem: PhotoAlbum): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PhotoAlbum, newItem: PhotoAlbum): Boolean {
                return oldItem.coverPhotoPath == newItem.coverPhotoPath &&
                        oldItem.photoCount == newItem.photoCount &&
                        oldItem.title == newItem.title
            }
        }
    }
}