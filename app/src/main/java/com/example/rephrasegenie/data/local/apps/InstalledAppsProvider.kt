package com.example.rephrasegenie.data.local.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.example.rephrasegenie.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lists the apps the user can actually open.
 *
 * Uses queryIntentActivities for ACTION_MAIN + CATEGORY_LAUNCHER, which the manifest's `<queries>`
 * block makes visible. Deliberately **not** QUERY_ALL_PACKAGES: that permission is restricted and
 * gets apps rejected on the Play Store, and launcher activities are exactly the list we want
 * anyway (§4.5).
 */
@Singleton
class InstalledAppsProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val packageManager: PackageManager get() = context.packageManager

    fun launchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved: List<ResolveInfo> = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
        }.getOrDefault(emptyList())

        return resolved.asSequence()
            .mapNotNull { info -> info.activityInfo?.applicationInfo }
            // Blocking our own app would be meaningless: the focus check already skips it.
            .filter { it.packageName != context.packageName }
            .map { InstalledApp(it.packageName, it.loadLabel(packageManager).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Falls back to the package name, so an uninstalled app still shows as a removable row. */
    fun labelFor(packageName: String): String = runCatching {
        packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
    }.getOrDefault(packageName)
}
