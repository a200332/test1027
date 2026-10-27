package com.dailybrief.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dailybrief.app.BuildConfig
import com.dailybrief.app.R
import com.dailybrief.app.data.NewsSource
import com.dailybrief.app.data.NewsSources
import com.dailybrief.app.data.SettingsStore
import com.dailybrief.app.databinding.ActivitySettingsBinding
import com.dailybrief.app.util.DigestScheduler
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: SourceSettingsAdapter
    private lateinit var settings: SettingsStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Snackbar.make(
                binding.root,
                R.string.notification_permission_denied,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.settingsToolbar.setNavigationOnClickListener { finish() }

        settings = SettingsStore(this)

        adapter = SourceSettingsAdapter(::toggleSource)
        binding.sourceList.layoutManager = LinearLayoutManager(this)
        binding.sourceList.adapter = adapter
        refreshSources()

        binding.digestSwitch.isChecked = settings.dailyDigestEnabled
        binding.digestSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.dailyDigestEnabled = isChecked
            if (isChecked) {
                DigestScheduler.schedule(this)
                requestNotificationPermissionIfNeeded()
            } else {
                DigestScheduler.cancel(this)
            }
        }

        binding.versionText.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun toggleSource(source: NewsSource) {
        val wasEnabled = settings.enabledSourceIds.contains(source.id)
        if (wasEnabled && settings.enabledSourceIds.size == 1) {
            Snackbar.make(binding.root, R.string.at_least_one_source, Snackbar.LENGTH_SHORT).show()
            refreshSources()
            return
        }
        settings.setSourceEnabled(source.id, !wasEnabled)
        refreshSources()
        Snackbar.make(
            binding.root,
            getString(
                if (wasEnabled) R.string.source_removed else R.string.source_added,
                source.name
            ),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun refreshSources() {
        val enabled = settings.enabledSourceIds
        adapter.submitList(
            NewsSources.all.map { SourceRow(it, it.id in enabled) }
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
