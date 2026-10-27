package com.dailybrief.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dailybrief.app.R
import com.dailybrief.app.data.NewsSources
import com.dailybrief.app.data.SettingsStore
import com.dailybrief.app.databinding.ActivityMainBinding
import com.dailybrief.app.util.DateUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var articleAdapter: ArticleAdapter
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                R.string.notification_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupList()
        setupChips()
        setupRefresh()

        binding.retryButton.setOnClickListener { viewModel.refresh(manual = true) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        maybeRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncSettings()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? SearchView ?: return true
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setQuery(query.orEmpty())
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText.orEmpty())
                return true
            }
        })
        val currentQuery = viewModel.state.value.query
        if (currentQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(currentQuery, false)
        }
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.setQuery("")
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refresh(manual = true)
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupList() {
        articleAdapter = ArticleAdapter { article ->
            startActivity(DetailActivity.createIntent(this, article))
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = articleAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupChips() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipHeadline -> NewsSources.CATEGORY_HEADLINE
                R.id.chipWorld -> NewsSources.CATEGORY_WORLD
                R.id.chipTech -> NewsSources.CATEGORY_TECH
                R.id.chipBusiness -> NewsSources.CATEGORY_BUSINESS
                else -> NewsSources.CATEGORY_ALL
            }
            viewModel.selectCategory(category)
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh(manual = true) }
    }

    private fun render(state: NewsUiState) {
        binding.dateText.text = DateUtils.todayLabel()
        binding.updateText.text = buildUpdateLabel(state)
        val selectedChip = when (state.category) {
            NewsSources.CATEGORY_HEADLINE -> R.id.chipHeadline
            NewsSources.CATEGORY_WORLD -> R.id.chipWorld
            NewsSources.CATEGORY_TECH -> R.id.chipTech
            NewsSources.CATEGORY_BUSINESS -> R.id.chipBusiness
            else -> R.id.chipAll
        }
        if (binding.chipGroup.checkedChipId != selectedChip) {
            binding.chipGroup.check(selectedChip)
        }
        binding.swipeRefresh.isRefreshing = state.isRefreshing && !state.isLoading

        val showError = !state.isLoading && state.allArticles.isEmpty() && state.errorMessage != null
        binding.loadingView.isVisible = state.isLoading && state.allArticles.isEmpty()
        binding.errorView.isVisible = showError
        binding.errorMessage.text = state.errorMessage
        binding.emptyView.isVisible = !state.isLoading && !showError &&
            state.visibleArticles.isEmpty()
        binding.recyclerView.isVisible = state.visibleArticles.isNotEmpty()

        articleAdapter.submitList(state.visibleArticles)
    }

    private fun buildUpdateLabel(state: NewsUiState): String {
        val parts = mutableListOf<String>()
        if (state.lastUpdated != null) {
            parts += getString(
                R.string.updated_at,
                DateUtils.shortClock(state.lastUpdated),
                state.allArticles.size
            )
        } else {
            parts += getString(if (state.isRefreshing) R.string.updating else R.string.never_updated)
        }
        if (state.failedSourceCount > 0) {
            parts += getString(R.string.sources_failed, state.failedSourceCount)
        }
        if (state.isCached) {
            parts += getString(R.string.offline_cache)
        }
        return parts.joinToString(" · ")
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val settings = SettingsStore(this)
        if (!settings.dailyDigestEnabled || settings.notificationAsked) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        settings.notificationAsked = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

}
