package com.dailybrief.app.data

data class Article(
    val id: String,
    val title: String,
    val link: String,
    val summary: String,
    val content: String?,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val category: String,
    val publishedAt: Long,
    val author: String?
)
