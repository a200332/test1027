package com.dailybrief.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HtmlUtilsTest {

    @Test
    fun `converts html to readable text`() {
        val html = "<p>Hello&nbsp;&amp; <b>world</b></p><p>second&nbsp;line</p>"
        assertEquals("Hello & world\nsecond line", HtmlUtils.toPlainText(html))
    }

    @Test
    fun `strips inline images and scripts`() {
        val html = "Text<img src='https://example.com/a.jpg'><script>bad()</script> end"
        assertEquals("Text end", HtmlUtils.toPlainText(html))
    }

    @Test
    fun `decodes numeric and named entities`() {
        assertEquals("中文 © …", HtmlUtils.decodeEntities("&#x4E2D;&#25991; &copy; &hellip;"))
        assertEquals("&lt;", HtmlUtils.decodeEntities("&amp;lt;"))
    }

    @Test
    fun `finds first usable image url`() {
        assertEquals(
            "https://example.com/a.jpg",
            HtmlUtils.firstImageUrl("<p><img data-x='1' src=\"https://example.com/a.jpg\"></p>")
        )
        assertEquals(
            "https://example.com/b.jpg",
            HtmlUtils.firstImageUrl("<img src='//example.com/b.jpg'>")
        )
        assertNull(HtmlUtils.firstImageUrl("<img src='data:image/png;base64,xx'>"))
        assertNull(HtmlUtils.firstImageUrl(null))
    }
}
