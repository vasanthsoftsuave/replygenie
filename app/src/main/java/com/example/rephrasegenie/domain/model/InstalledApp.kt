package com.example.rephrasegenie.domain.model

/**
 * An app the user can actually open, as offered by the blocked-apps picker.
 *
 * The list comes from a `<queries>` block for ACTION_MAIN + CATEGORY_LAUNCHER, never from the
 * restricted QUERY_ALL_PACKAGES permission, which gets apps rejected on the Play Store.
 *
 * The icon is deliberately not here — this is domain code and must stay free of Android types.
 * The picker loads icons from the package name.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
