package com.dailybrief.app.util

object HtmlUtils {

    private val imagePattern = Regex(
        """<img[^>]*\bsrc\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val breakPattern = Regex(
        """<\s*(?:br|/p|/div|/li|/h[1-6]|/tr)\s*/?\s*>""",
        RegexOption.IGNORE_CASE
    )
    private val listItemPattern = Regex("""<\s*li[^>]*>""", RegexOption.IGNORE_CASE)
    private val scriptPattern = Regex(
        """<(script|style)[^>]*>.*?</\1>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val tagPattern = Regex("<[^>]*>")
    private val horizontalWhitespace = Regex("[\t ]+")
    private val numericEntityPattern = Regex("""&#(x?[0-9a-fA-F]+);""")

    private val namedEntities = mapOf(
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "ldquo" to "“",
        "rdquo" to "”",
        "lsquo" to "‘",
        "rsquo" to "’",
        "hellip" to "…",
        "mdash" to "—",
        "ndash" to "–",
        "middot" to "·",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
        "amp" to "&"
    )

    /** Returns the first usable http(s) image URL found in an HTML fragment. */
    fun firstImageUrl(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val raw = imagePattern.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (raw.isBlank() || raw.startsWith("data:", ignoreCase = true)) return null
        val normalized = when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("http://", ignoreCase = true) -> raw
            raw.startsWith("https://", ignoreCase = true) -> raw
            else -> return null
        }
        return normalized
    }

    /** Converts escaped feed HTML into readable plain text while keeping paragraph breaks. */
    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        var text = scriptPattern.replace(html, " ")
        text = breakPattern.replace(text, "\n")
        text = listItemPattern.replace(text, "\n• ")
        text = tagPattern.replace(text, "")
        text = decodeEntities(text)
        text = text.replace('\u00A0', ' ')
        return text
            .lineSequence()
            .map { it.trim().replace(horizontalWhitespace, " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .trim()
    }

    fun decodeEntities(input: String): String {
        var text = numericEntityPattern.replace(input) { match ->
            val token = match.groupValues[1]
            val codePoint = runCatching {
                if (token.startsWith("x", ignoreCase = true)) {
                    token.drop(1).toInt(16)
                } else {
                    token.toInt()
                }
            }.getOrNull()
            if (codePoint != null && codePoint in 1..0x10FFFF) {
                String(Character.toChars(codePoint))
            } else {
                match.value
            }
        }
        for ((name, value) in namedEntities) {
            text = text.replace("&$name;", value, ignoreCase = true)
        }
        return text
    }
}
