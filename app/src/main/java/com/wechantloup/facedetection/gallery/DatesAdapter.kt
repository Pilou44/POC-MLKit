package com.wechantloup.facedetection.gallery

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wechantloup.facedetection.R
import com.wechantloup.facedetection.databinding.ItemDateLayoutBinding
import com.wechantloup.facedetection.inflate

class DatesAdapter(
    val onDateClicked: (String) -> Unit,
) : ListAdapter<String, DatesAdapter.DateHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateHolder {
        return DateHolder(parent)
    }

    override fun onBindViewHolder(holder: DateHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DateHolder(parent: ViewGroup): RecyclerView.ViewHolder(parent.inflate(R.layout.item_date_layout)) {

        private val binding = ItemDateLayoutBinding.bind(itemView)

        fun bind(item: Any) {
            val date = item as String
            binding.tvDate.text = date
            itemView.setOnClickListener { onDateClicked(item) }
        }
    }

    companion object {

        private val diffCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return true
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return  oldItem == newItem
            }
        }
    }

}
