package com.dailybrief.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RefreshOutcome(
    val articles: List<Article>,
    val failedSources: List<NewsSource>,
    val fetchedAt: Long
)

class NewsRefreshException(
    val failedSources: List<NewsSource>,
    message: String
) : IOException(message)

class NewsRepository(
    private val cache: NewsCache,
    private val settings: SettingsStore,
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun loadCached(): CacheSnapshot? = withContext(Dispatchers.IO) { cache.load() }

    fun enabledSources(): List<NewsSource> {
        val enabled = settings.enabledSourceIds
        return NewsSources.all.filter { it.id in enabled }
    }

    suspend fun refresh(): RefreshOutcome = coroutineScope {
        val sources = enabledSources()
        if (sources.isEmpty()) {
            throw NewsRefreshException(sources, "No news sources enabled")
        }

        val results = sources.map { source ->
            async(Dispatchers.IO) {
                source to try {
                    Result.success(fetchSource(source))
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Result.failure(error)
                }
            }
        }.awaitAll()

        val failed = results.filter { it.second.isFailure }.map { it.first }
        val articles = results
            .flatMap { (_, result) -> result.getOrNull().orEmpty() }
            .filter { it.title.isNotBlank() && it.link.isNotBlank() }
            .distinctBy { it.link.substringBefore('#') }
            .distinctBy { it.title.lowercase(Locale.ROOT) }
            .sortedByDescending { it.publishedAt }
            .take(MAX_ARTICLES)

        if (articles.isEmpty() && failed.size == sources.size) {
            throw NewsRefreshException(failed, "All news sources failed")
        }

        val fetchedAt = System.currentTimeMillis()
        if (articles.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                cache.save(CacheSnapshot(fetchedAt, articles))
            }
        }
        RefreshOutcome(articles, failed, fetchedAt)
    }

    private fun fetchSource(source: NewsSource): List<Article> {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for ${source.id}")
            }
            val body = response.body ?: throw IOException("Empty body for ${source.id}")
            if (body.contentLength() > MAX_RESPONSE_BYTES) {
                throw IOException("Response too large for ${source.id}")
            }
            val bytes = body.bytes()
            if (bytes.size > MAX_RESPONSE_BYTES) {
                throw IOException("Response too large for ${source.id}")
            }
            val xml = decode(bytes, body.contentType()?.charset())
            return FeedParser.parse(xml, source, MAX_ITEMS_PER_SOURCE)
        }
    }

    private fun decode(bytes: ByteArray, headerCharset: Charset?): String {
        val prefix = String(bytes, 0, minOf(bytes.size, 200), Charsets.ISO_8859_1)
        val declared = Regex(
            """encoding\s*=\s*["']([A-Za-z0-9._-]+)["']""",
            RegexOption.IGNORE_CASE
        ).find(prefix)?.groupValues?.getOrNull(1)

        val charset = sequenceOf(headerCharset?.name(), declared, "UTF-8")
            .filterNotNull()
            .mapNotNull { name -> runCatching { Charset.forName(name) }.getOrNull() }
            .firstOrNull() ?: Charsets.UTF_8

        return String(bytes, charset).removePrefix("\uFEFF")
    }

    companion object {
        private const val MAX_ARTICLES = 300
        private const val MAX_ITEMS_PER_SOURCE = 30
        private const val MAX_RESPONSE_BYTES = 5L * 1024L * 1024L
        private const val USER_AGENT = "DailyBrief/1.0 (Android; +https://github.com/a200332/test1027)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
