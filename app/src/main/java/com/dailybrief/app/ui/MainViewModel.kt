package com.dailybrief.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybrief.app.R
import com.dailybrief.app.data.Article
import com.dailybrief.app.data.NewsCache
import com.dailybrief.app.data.NewsRepository
import com.dailybrief.app.data.NewsSources
import com.dailybrief.app.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val allArticles: List<Article> = emptyList(),
    val category: String = NewsSources.CATEGORY_ALL,
    val query: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val failedSourceCount: Int = 0,
    val isCached: Boolean = false,
    val errorMessage: String? = null
) {
    val visibleArticles: List<Article>
        get() {
            val keyword = query.trim()
            return allArticles.filter { article ->
                val matchesCategory = category == NewsSources.CATEGORY_ALL ||
                    article.category == category
                val matchesQuery = keyword.isEmpty() ||
                    article.title.contains(keyword, ignoreCase = true) ||
                    article.summary.contains(keyword, ignoreCase = true) ||
                    article.sourceName.contains(keyword, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsStore(application)
    private val repository = NewsRepository(NewsCache(application), settings)

    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var refreshJob: Job? = null
    private var pendingRefresh = false
    private var lastEnabledIds: Set<String> = settings.enabledSourceIds

    init {
        viewModelScope.launch {
            val cached = repository.loadCached()
            if (cached != null) {
                _state.update {
                    it.copy(
                        allArticles = cached.articles,
                        lastUpdated = cached.fetchedAt,
                        isLoading = false,
                        isCached = true
                    )
                }
            }
            refresh()
        }
    }

    fun refresh(manual: Boolean = false) {
        if (refreshJob?.isActive == true) {
            if (manual) pendingRefresh = true
            return
        }
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = it.allArticles.isEmpty(),
                    isRefreshing = true,
                    errorMessage = null
                )
            }
            try {
                val outcome = repository.refresh()
                val hadArticles = _state.value.allArticles.isNotEmpty()
                val useExisting = outcome.articles.isEmpty() && hadArticles
                _state.update { current ->
                    current.copy(
                        allArticles = if (useExisting) current.allArticles else outcome.articles,
                        lastUpdated = if (useExisting) current.lastUpdated else outcome.fetchedAt,
                        isLoading = false,
                        isRefreshing = false,
                        isCached = useExisting,
                        failedSourceCount = outcome.failedSources.size,
                        errorMessage = if (!useExisting && outcome.articles.isEmpty()) {
                            app.getString(R.string.no_content)
                        } else {
                            null
                        }
                    )
                }
                if (outcome.failedSources.isNotEmpty()) {
                    sendEvent(app.getString(R.string.sources_failed, outcome.failedSources.size))
                }
                if (useExisting) {
                    sendEvent(app.getString(R.string.no_content))
                } else if (manual && outcome.articles.isNotEmpty()) {
                    sendEvent(app.getString(R.string.refresh_done, outcome.articles.size))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val cached = repository.loadCached()
                _state.update { current ->
                    val fallback = current.allArticles.ifEmpty { cached?.articles.orEmpty() }
                    val hasContent = fallback.isNotEmpty()
                    current.copy(
                        allArticles = fallback,
                        lastUpdated = current.lastUpdated ?: cached?.fetchedAt,
                        isLoading = false,
                        isRefreshing = false,
                        isCached = hasContent,
                        errorMessage = if (hasContent) null else app.getString(R.string.error_message)
                    )
                }
                sendEvent(
                    app.getString(
                        if (_state.value.allArticles.isNotEmpty()) {
                            R.string.offline_cache
                        } else {
                            R.string.error_message
                        }
                    )
                )
            } finally {
                refreshJob = null
                if (pendingRefresh) {
                    pendingRefresh = false
                    refresh()
                }
            }
        }
    }

    fun selectCategory(category: String) {
        if (category !in NewsSources.categories) return
        _state.update { it.copy(category = category) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    /** Called from onResume so changes made in Settings are picked up. */
    fun syncSettings() {
        val enabled = settings.enabledSourceIds
        if (enabled == lastEnabledIds) return
        lastEnabledIds = enabled
        if (refreshJob?.isActive == true) {
            pendingRefresh = true
        } else {
            refresh()
        }
    }

    private val app: Application
        get() = getApplication()

    private fun sendEvent(message: String) {
        _events.trySend(message)
    }
}
