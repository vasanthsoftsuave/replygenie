package com.example.rephrasegenie.domain.model

/** The three theme choices, matching the Windows app. Dark is the default. */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DARK
    }
}

/**
 * Everything the user can configure.
 *
 * [blockedApps] holds package names and is read on every focus event, so it is kept in memory
 * rather than read from disk on that path.
 */
data class AppSettings(
    val username: String = "",
    val defaultToneId: String? = null,
    val bubbleEnabled: Boolean = true,
    val guardrailsEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: String = DEFAULT_ACCENT,
    val blockedApps: Set<String> = emptySet(),
) {
    companion object {
        const val DEFAULT_ACCENT = "#8AB4FF"
    }
}
