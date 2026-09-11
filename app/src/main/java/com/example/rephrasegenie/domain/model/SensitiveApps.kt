package com.example.rephrasegenie.domain.model

/**
 * Apps the bubble should stay out of by default.
 *
 * Banking, payment and password-manager apps. On first run, whichever of these are installed are
 * pre-filled into the blocked list — a safe default beats a helpful one here. The user can remove
 * any of them in Settings.
 *
 * This is only a starting point, not a security boundary: many of these apps also set
 * HIDE_NON_SYSTEM_OVERLAY_WINDOWS, which hides the bubble whether we block them or not (§4.4).
 */
object SensitiveApps {

    val PACKAGE_NAMES: Set<String> = setOf(
        // Wallets and payments
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.nbu.paisa.user",
        "com.paypal.android.p2pmobile",
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        "com.squareup.cash",
        "com.venmo",
        "com.revolut.revolut",
        "com.wise.android",

        // Password managers
        "com.lastpass.lpandroid",
        "com.onepassword.android",
        "com.agilebits.onepassword",
        "com.bitwarden.authenticator",
        "com.x8bit.bitwarden",
        "com.dashlane",
        "com.keepersecurity.parent",
        "com.azure.authenticator",
        "com.google.android.apps.authenticator2",

        // Crypto
        "com.coinbase.android",
        "com.binance.dev",
        "io.metamask",
    )

    /** Fallback for anything not on the list above: names that read like a banking app. */
    private val NAME_HINTS = listOf("bank", "banking", "wallet", "password", "authenticator")

    /**
     * Picks the apps to block by default out of what is installed.
     *
     * [installed] is matched both by exact package name and by a hint in the package name, so a
     * local bank we have never heard of is still caught.
     */
    fun detectIn(installed: List<InstalledApp>): Set<String> =
        installed.asSequence()
            .filter { app ->
                app.packageName in PACKAGE_NAMES ||
                    NAME_HINTS.any { hint -> app.packageName.contains(hint, ignoreCase = true) }
            }
            .map { it.packageName }
            .toSet()
}
