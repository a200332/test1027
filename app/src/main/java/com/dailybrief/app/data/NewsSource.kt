package com.dailybrief.app.data

/**
 * A public RSS feed the app can aggregate. Categories are intentionally simple so the
 * chip row on the home screen stays stable even when sources are added.
 */
data class NewsSource(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val homepage: String
)

object NewsSources {
    const val CATEGORY_ALL = "全部"
    const val CATEGORY_HEADLINE = "头条"
    const val CATEGORY_WORLD = "国际"
    const val CATEGORY_TECH = "科技"
    const val CATEGORY_BUSINESS = "财经"

    val categories = listOf(
        CATEGORY_ALL,
        CATEGORY_HEADLINE,
        CATEGORY_WORLD,
        CATEGORY_TECH,
        CATEGORY_BUSINESS
    )

    val all: List<NewsSource> = listOf(
        NewsSource(
            id = "google-news",
            name = "Google 新闻",
            url = "https://news.google.com/rss?hl=zh-CN&gl=CN&ceid=CN:zh-Hans",
            category = CATEGORY_HEADLINE,
            homepage = "https://news.google.com"
        ),
        NewsSource(
            id = "bbc-zh",
            name = "BBC 中文",
            url = "https://feeds.bbci.co.uk/zhongwen/simp/rss.xml",
            category = CATEGORY_WORLD,
            homepage = "https://www.bbc.com/zhongwen/simp"
        ),
        NewsSource(
            id = "nyt-zh",
            name = "纽约时报中文网",
            url = "https://cn.nytimes.com/rss/",
            category = CATEGORY_WORLD,
            homepage = "https://cn.nytimes.com"
        ),
        NewsSource(
            id = "36kr",
            name = "36氪",
            url = "https://36kr.com/feed",
            category = CATEGORY_TECH,
            homepage = "https://36kr.com"
        ),
        NewsSource(
            id = "sspai",
            name = "少数派",
            url = "https://sspai.com/feed",
            category = CATEGORY_TECH,
            homepage = "https://sspai.com"
        ),
        NewsSource(
            id = "jiemian",
            name = "界面新闻",
            url = "https://a.jiemian.com/index.php?m=article&a=rss",
            category = CATEGORY_BUSINESS,
            homepage = "https://www.jiemian.com"
        )
    )

    val defaultEnabledIds: Set<String> = all.map { it.id }.toSet()

    fun find(id: String): NewsSource? = all.firstOrNull { it.id == id }
}
