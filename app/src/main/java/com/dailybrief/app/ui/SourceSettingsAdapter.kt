package com.dailybrief.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dailybrief.app.data.NewsSource
import com.dailybrief.app.databinding.ItemSourceBinding

data class SourceRow(
    val source: NewsSource,
    val enabled: Boolean
)

class SourceSettingsAdapter(
    private val onToggle: (NewsSource) -> Unit
) : ListAdapter<SourceRow, SourceSettingsAdapter.SourceViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding = ItemSourceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SourceViewHolder(
        private val binding: ItemSourceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: SourceRow) {
            binding.sourceName.text = row.source.name
            binding.sourceCategory.text = row.source.category
            binding.sourceCheckbox.isChecked = row.enabled
            binding.root.setOnClickListener { onToggle(row.source) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SourceRow>() {
        override fun areItemsTheSame(oldItem: SourceRow, newItem: SourceRow): Boolean =
            oldItem.source.id == newItem.source.id

        override fun areContentsTheSame(oldItem: SourceRow, newItem: SourceRow): Boolean =
            oldItem == newItem
    }
}
