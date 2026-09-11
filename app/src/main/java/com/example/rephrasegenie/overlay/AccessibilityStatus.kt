package com.example.rephrasegenie.overlay

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/** Whether the user has switched our accessibility service on in system settings. */
object AccessibilityStatus {

    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, RephraseAccessibilityService::class.java)
            .flattenToString()

        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (entry in splitter) {
            if (entry.equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
