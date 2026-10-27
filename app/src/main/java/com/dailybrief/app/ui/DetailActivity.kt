package com.dailybrief.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import coil.load
import com.dailybrief.app.R
import com.dailybrief.app.data.Article
import com.dailybrief.app.databinding.ActivityDetailBinding
import com.dailybrief.app.util.DateUtils
import com.google.android.material.snackbar.Snackbar

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var articleTitle: String = ""
    private var articleLink: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.detailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.detailToolbar.setNavigationOnClickListener { finish() }

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val link = intent.getStringExtra(EXTRA_LINK).orEmpty()
        if (title.isBlank() || link.isBlank()) {
            finish()
            return
        }
        articleTitle = title
        articleLink = link

        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty().ifBlank {
            getString(R.string.app_name)
        }
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val publishedAt = intent.getLongExtra(EXTRA_PUBLISHED_AT, 0L)
        val summary = intent.getStringExtra(EXTRA_SUMMARY).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE)

        binding.detailTitle.text = title
        val metaParts = mutableListOf(source)
        if (category.isNotBlank()) metaParts += category
        DateUtils.detailTime(publishedAt)?.let { metaParts += it }
        binding.detailMeta.text = metaParts.joinToString(" · ")

        val body = content.ifBlank { summary }.ifBlank {
            getString(R.string.detail_no_summary)
        }
        binding.detailBody.text = body

        if (imageUrl.isNullOrBlank()) {
            binding.detailImage.isVisible = false
        } else {
            binding.detailImage.isVisible = true
            binding.detailImage.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }
        }

        binding.disclaimer.text = getString(R.string.detail_disclaimer, source)
        binding.openButton.setOnClickListener { openOriginal() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_share -> {
                shareArticle()
                true
            }
            R.id.action_open -> {
                openOriginal()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openOriginal() {
        val uri = runCatching { Uri.parse(articleLink) }.getOrNull()
        if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https")) {
            Snackbar.make(binding.root, R.string.link_invalid, Snackbar.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.no_browser, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun shareArticle() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, articleTitle)
            putExtra(Intent.EXTRA_TEXT, "$articleTitle\n$articleLink")
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser)))
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_LINK = "extra_link"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_PUBLISHED_AT = "extra_published_at"
        private const val EXTRA_SUMMARY = "extra_summary"
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_IMAGE = "extra_image"

        fun createIntent(context: Context, article: Article): Intent =
            Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_TITLE, article.title)
                putExtra(EXTRA_LINK, article.link)
                putExtra(EXTRA_SOURCE, article.sourceName)
                putExtra(EXTRA_CATEGORY, article.category)
                putExtra(EXTRA_PUBLISHED_AT, article.publishedAt)
                putExtra(EXTRA_SUMMARY, article.summary)
                putExtra(EXTRA_CONTENT, article.content)
                putExtra(EXTRA_IMAGE, article.imageUrl)
            }
    }
}
