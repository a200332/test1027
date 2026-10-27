package com.dailybrief.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dailybrief.app.R
import com.dailybrief.app.data.Article
import com.dailybrief.app.databinding.ItemArticleBinding
import com.dailybrief.app.util.DateUtils

class ArticleAdapter(
    private val onArticleClick: (Article) -> Unit
) : ListAdapter<Article, ArticleAdapter.ArticleViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArticleViewHolder(
        private val binding: ItemArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            val context = binding.root.context
            binding.articleTitle.text = article.title
            binding.articleSource.text = context.getString(
                R.string.source_meta,
                article.sourceName,
                article.category
            )
            binding.articleTime.text = DateUtils.relativeTime(context, article.publishedAt)

            val summary = article.summary.ifBlank { article.content.orEmpty() }
            binding.articleSummary.text = summary
            binding.articleSummary.isVisible = summary.isNotBlank()

            if (article.imageUrl.isNullOrBlank()) {
                binding.articleImage.isVisible = false
                binding.articleImage.setImageDrawable(null)
            } else {
                binding.articleImage.isVisible = true
                binding.articleImage.load(article.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                }
            }

            binding.root.setOnClickListener { onArticleClick(article) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean =
            oldItem == newItem
    }
}
