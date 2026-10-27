package com.dailybrief.app.data

import com.dailybrief.app.util.DateUtils
import com.dailybrief.app.util.HtmlUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Small, dependency-free RSS 2.0 / Atom parser. It uses XmlPullParser so large feeds do
 * not need to be materialized as a DOM.
 */
object FeedParser {

    private const val MAX_SUMMARY_LENGTH = 500
    private const val MAX_CONTENT_LENGTH = 6_000
    private val whitespace = Regex("\\s+")
    private const val GOOGLE_NEWS_PROMO = "在Google 新闻上查看更多头条新闻和观点"

    fun parse(xml: String, source: NewsSource, maxItems: Int = 30): List<Article> {
        if (xml.isBlank() || maxItems <= 0) return emptyList()
        val articles = mutableListOf<Article>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && articles.size < maxItems) {
                if (event == XmlPullParser.START_TAG &&
                    (parser.name == "item" || parser.name == "entry")
                ) {
                    parseItem(parser, source)?.let { articles.add(it) }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            // Keep the items parsed so far. Feeds occasionally contain broken entities.
        }
        return articles
    }

    private fun parseItem(parser: XmlPullParser, source: NewsSource): Article? {
        var title = ""
        var link = ""
        var description = ""
        var encodedContent = ""
        var publishedRaw = ""
        var author = ""
        var itemSource = ""
        var image = ""
        val itemDepth = parser.depth

        loop@ while (true) {
            when (parser.next()) {
                XmlPullParser.END_DOCUMENT -> break@loop
                XmlPullParser.END_TAG -> {
                    if (parser.depth == itemDepth &&
                        (parser.name == "item" || parser.name == "entry")
                    ) {
                        break@loop
                    }
                }
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "title" -> title = readText(parser)
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            link = if (!href.isNullOrBlank()) href.trim() else readText(parser)
                        }
                        "description", "summary" -> description = readText(parser)
                        "encoded" -> encodedContent = readText(parser)
                        "pubDate", "published", "updated", "date" -> {
                            if (publishedRaw.isBlank()) publishedRaw = readText(parser)
                        }
                        "guid", "id" -> {
                            if (link.isBlank()) link = readText(parser)
                        }
                        "creator", "author" -> {
                            if (author.isBlank()) author = readText(parser)
                        }
                        "source" -> {
                            if (parser.namespace.isNullOrEmpty() && itemSource.isBlank()) {
                                itemSource = readText(parser)
                            }
                        }
                        "thumbnail" -> {
                            image = image.ifBlank {
                                parser.getAttributeValue(null, "url").orEmpty()
                            }
                        }
                        "content" -> {
                            val namespace = parser.namespace.orEmpty()
                            if (namespace.contains("media")) {
                                val type = parser.getAttributeValue(null, "type")
                                    ?: parser.getAttributeValue(null, "medium")
                                if (type == null || type.contains("image", ignoreCase = true)) {
                                    image = image.ifBlank {
                                        parser.getAttributeValue(null, "url").orEmpty()
                                    }
                                }
                            }
                        }
                        "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            if (type.startsWith("image", ignoreCase = true)) {
                                image = image.ifBlank {
                                    parser.getAttributeValue(null, "url").orEmpty()
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
        val cleanTitle = HtmlUtils.toPlainText(title)
            .replace(whitespace, " ")
            .trim()
        if (cleanTitle.isBlank()) return null

        val resolvedLink = resolveUrl(link, source) ?: return null
        if (!resolvedLink.startsWith("http", ignoreCase = true)) return null

        val summaryHtml = description.ifBlank { encodedContent }
        val cleanSource = HtmlUtils.toPlainText(itemSource).trim().ifBlank { source.name }
        val summary = stripFeedNoise(HtmlUtils.toPlainText(summaryHtml), cleanTitle, cleanSource)
            .replace(whitespace, " ")
            .trim()
            .take(MAX_SUMMARY_LENGTH)
        val content = stripFeedNoise(
            HtmlUtils.toPlainText(encodedContent.ifBlank { description }),
            cleanTitle,
            cleanSource
        )
            .trim()
            .take(MAX_CONTENT_LENGTH)
            .ifBlank { null }
        val resolvedImage = resolveUrl(
            image.ifBlank { HtmlUtils.firstImageUrl(summaryHtml) },
            source
        )?.takeIf { it.startsWith("http", ignoreCase = true) }

        return Article(
            id = "${source.id}|$resolvedLink",
            title = cleanTitle,
            link = resolvedLink,
            summary = summary,
            content = content,
            imageUrl = resolvedImage,
            sourceId = source.id,
            sourceName = cleanSource,
            category = source.category,
            publishedAt = DateUtils.parseFeedDate(publishedRaw) ?: 0L,
            author = author.trim().ifBlank { null }
        )

    }
    /** Removes feed boilerplate such as duplicated titles and Google News promo text. */
    private fun stripFeedNoise(text: String, title: String, sourceName: String): String {
        var value = text.trim()
        if (value.startsWith(title)) {
            value = value.removePrefix(title).trim()
        }
        value = value.replace(GOOGLE_NEWS_PROMO, "").trim()
        return if (value == sourceName) "" else value
    }

    /**
     * Reads text content of the current element, including text inside nested elements.
     * Some feeds wrap HTML in real tags instead of CDATA.
     */
    private fun readText(parser: XmlPullParser): String {
        val builder = StringBuilder()
        val startDepth = parser.depth
        loop@ while (true) {
            when (parser.next()) {
                XmlPullParser.TEXT -> builder.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.depth == startDepth) break@loop
                XmlPullParser.END_DOCUMENT -> break@loop
                else -> Unit
            }
        }
        return builder.toString()
    }

    private fun resolveUrl(raw: String?, source: NewsSource): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("/") -> source.homepage.trimEnd('/') + value
            else -> source.homepage.trimEnd('/') + "/" + value
        }
    }

}
