package com.dailybrief.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses small, real-world snapshots of the bundled feeds. Keeping fixtures in the
 * repository makes namespace/CDATA regressions visible without network access.
 */
class RealFeedFixturesTest {

    private val fixtures = mapOf(
        "google_news.xml" to "google-news",
        "bbc_zh.xml" to "bbc-zh",
        "nyt_zh.xml" to "nyt-zh",
        "36kr.xml" to "36kr",
        "sspai.xml" to "sspai",
        "jiemian.xml" to "jiemian"
    )

    @Test
    fun `parses bundled real feed snapshots`() {
        fixtures.forEach { (fileName, sourceId) ->
            val xml = readFixture(fileName)
            val source = requireNotNull(NewsSources.find(sourceId)) { "Missing source $sourceId" }
            val articles = FeedParser.parse(xml, source, maxItems = 10)

            assertTrue("$fileName should contain articles", articles.size >= 2)
            articles.forEach { article ->
                assertTrue("$fileName title blank", article.title.isNotBlank())
                assertFalse("$fileName title contains markup", article.title.contains("<"))
                assertTrue("$fileName link invalid: ${article.link}", article.link.startsWith("http"))
                assertEquals(source.category, article.category)
                assertTrue("$fileName source name blank", article.sourceName.isNotBlank())
                assertFalse(
                    "$fileName summary contains markup",
                    article.summary.contains("<p>") || article.summary.contains("<img")
                )
            }
            assertTrue("$fileName first item has no date", articles.first().publishedAt > 0L)
            if (sourceId == "google-news") {
                assertFalse(
                    "Google summary should not contain promo text",
                    articles.any { it.summary.contains("在Google 新闻上查看更多") }
                )
            }
            println(
                "$fileName parsed=${articles.size} " +
                    "first=${articles.first().title} " +
                    "published=${articles.first().publishedAt} " +
                    "image=${articles.first().imageUrl}"
            )
        }
    }

    private fun readFixture(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("feeds/$name")
        requireNotNull(stream) { "Fixture $name not found" }
        return stream.bufferedReader().use { it.readText() }
    }
}
