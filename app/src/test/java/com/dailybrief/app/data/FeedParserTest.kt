package com.dailybrief.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FeedParserTest {

    private val source = NewsSource(
        id = "test",
        name = "测试源",
        url = "https://example.com/feed",
        category = "科技",
        homepage = "https://example.com"
    )

    @Test
    fun `parses rss items with cdata media and relative links`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                xmlns:media="http://search.yahoo.com/mrss/"
                xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>测试频道</title>
                <item>
                  <title><![CDATA[头条：A &amp; B]]></title>
                  <link>https://example.com/a?utm=1</link>
                  <description><![CDATA[<p>第一段 &amp; 内容</p><p>第二段</p><img src="https://example.com/a.jpg">]]></description>
                  <content:encoded><![CDATA[<p>完整正文</p>]]></content:encoded>
                  <pubDate>Tue, 27 Oct 2026 11:53:59 +0800</pubDate>
                  <media:thumbnail url="https://example.com/thumb.jpg" />
                  <source url="https://source.example">示例来源</source>
                </item>
                <item>
                  <title>没有日期的条目</title>
                  <link>/relative/b</link>
                  <enclosure url="https://example.com/b.png" type="image/png" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val articles = FeedParser.parse(xml, source)

        assertEquals(2, articles.size)
        val first = articles[0]
        assertEquals("头条：A & B", first.title)
        assertEquals("https://example.com/a?utm=1", first.link)
        assertEquals("第一段 & 内容 第二段", first.summary)
        assertEquals("完整正文", first.content)
        assertEquals("https://example.com/thumb.jpg", first.imageUrl)
        assertEquals("示例来源", first.sourceName)
        assertEquals("科技", first.category)
        assertEquals(
            Instant.parse("2026-10-27T03:53:59Z").toEpochMilli(),
            first.publishedAt
        )

        val second = articles[1]
        assertEquals("https://example.com/relative/b", second.link)
        assertEquals("https://example.com/b.png", second.imageUrl)
        assertEquals(0L, second.publishedAt)
        assertEquals("测试源", second.sourceName)
    }

    @Test
    fun `parses atom entries`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom 频道</title>
              <entry>
                <title>Atom 新闻</title>
                <link href="https://example.org/atom/1" />
                <summary>这是摘要</summary>
                <updated>2026-10-26T08:00:00Z</updated>
                <author><name>记者甲</name></author>
              </entry>
            </feed>
        """.trimIndent()

        val articles = FeedParser.parse(xml, source)

        assertEquals(1, articles.size)
        assertEquals("Atom 新闻", articles[0].title)
        assertEquals("https://example.org/atom/1", articles[0].link)
        assertEquals("这是摘要", articles[0].summary)
        assertEquals("记者甲", articles[0].author)
        assertEquals(
            Instant.parse("2026-10-26T08:00:00Z").toEpochMilli(),
            articles[0].publishedAt
        )
    }

    @Test
    fun `ignores malformed items but keeps valid ones`() {
        val xml = """
            <rss version="2.0"><channel>
              <item><title>缺少链接</title></item>
              <item>
                <title>正常条目</title>
                <link>https://example.com/ok</link>
                <pubDate>not a date</pubDate>
              </item>
            </channel></rss>
        """.trimIndent()

        val articles = FeedParser.parse(xml, source)

        assertEquals(1, articles.size)
        assertEquals("正常条目", articles[0].title)
        assertEquals(0L, articles[0].publishedAt)
        assertNotNull(articles[0].id)
        assertTrue(articles[0].id.contains("example.com/ok"))
    }
}
