package com.example.rephrasegenie.domain.usecase

import com.example.rephrasegenie.domain.model.SensitiveApps
import com.example.rephrasegenie.domain.repository.InstalledAppsRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On first run, pre-fills the blocked list with whichever banking, payment and password-manager
 * apps are installed. A safe default beats a helpful one here (§4.5).
 *
 * Runs once ever. Re-adding an app the user deliberately removed would be worse than not
 * suggesting it in the first place, so the flag is stored separately from the list itself.
 */
@Singleton
class SeedBlockedAppsUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val installedApps: InstalledAppsRepository,
) {
    suspend operator fun invoke() {
        if (settings.hasSeededBlockedApps()) return

        val detected = SensitiveApps.detectIn(installedApps.launchableApps())
        if (detected.isNotEmpty()) {
            settings.update { it.copy(blockedApps = it.blockedApps + detected) }
        }
        settings.markBlockedAppsSeeded()
    }
}
