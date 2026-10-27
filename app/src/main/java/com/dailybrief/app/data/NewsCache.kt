package com.dailybrief.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CacheSnapshot(
    val fetchedAt: Long,
    val articles: List<Article>
)

/** Persists the last successful aggregation so the app works offline. */
class NewsCache(context: Context) {

    private val cacheFile = File(context.applicationContext.filesDir, "news_cache.json")

    @Synchronized
    fun load(): CacheSnapshot? {
        if (!cacheFile.exists()) return null
        return runCatching {
            val root = JSONObject(cacheFile.readText())
            val fetchedAt = root.optLong("fetchedAt", 0L)
            val array = root.optJSONArray("articles") ?: JSONArray()
            val articles = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    item.toArticle()?.let(::add)
                }
            }
            if (articles.isEmpty()) null else CacheSnapshot(fetchedAt, articles)
        }.getOrNull()
    }

    @Synchronized
    fun save(snapshot: CacheSnapshot) {
        runCatching {
            val array = JSONArray()
            snapshot.articles.forEach { array.put(it.toJson()) }
            val root = JSONObject()
                .put("version", 1)
                .put("fetchedAt", snapshot.fetchedAt)
                .put("articles", array)
            val temp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
            temp.writeText(root.toString())
            if (!temp.renameTo(cacheFile)) {
                cacheFile.writeText(root.toString())
                temp.delete()
            }
        }
    }

    @Synchronized
    fun clear() {
        cacheFile.delete()
    }

    private fun Article.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("link", link)
        .put("summary", summary)
        .put("content", content)
        .put("imageUrl", imageUrl)
        .put("sourceId", sourceId)
        .put("sourceName", sourceName)
        .put("category", category)
        .put("publishedAt", publishedAt)
        .put("author", author)

    private fun JSONObject.toArticle(): Article? {
        val title = optString("title").takeIf { it.isNotBlank() } ?: return null
        val link = optString("link").takeIf { it.isNotBlank() } ?: return null
        return Article(
            id = optString("id").ifBlank { link },
            title = title,
            link = link,
            summary = optString("summary"),
            content = optStringOrNull("content"),
            imageUrl = optStringOrNull("imageUrl"),
            sourceId = optString("sourceId"),
            sourceName = optString("sourceName"),
            category = optString("category"),
            publishedAt = optLong("publishedAt", 0L),
            author = optStringOrNull("author")
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
