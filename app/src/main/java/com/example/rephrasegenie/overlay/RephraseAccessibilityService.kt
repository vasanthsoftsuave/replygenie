package com.example.rephrasegenie.overlay

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.ThemeMode
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import com.example.rephrasegenie.domain.usecase.RephraseStage
import com.example.rephrasegenie.domain.usecase.RephraseTextUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The core of the app: watches for the user focusing a text field in any app, shows the bubble
 * next to it, and on tap rephrases what they typed and writes it back.
 */
@AndroidEntryPoint
class RephraseAccessibilityService : AccessibilityService() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var tones: ToneRepository
    @Inject lateinit var rephraseText: RephraseTextUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private var overlay: BubbleOverlay? = null
    private var currentField: FocusedField? = null
    private var rephraseJob: Job? = null

    /** Debounces focus events. Windows has none and the bubble would flicker on a phone. */
    private var pendingShow: Runnable? = null

    /** The tone the last rephrase used, so a second tap repeats it without another choice. */
    private var lastUsedTone: Tone? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = BubbleOverlay(
            service = this,
            onTap = ::onBubbleTapped,
            onLongPress = ::onBubbleLongPressed,
            onToneChosen = ::onToneChosen,
        )
        applyAppearance()
        Log.i(TAG, "Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            -> handleFocus(event)

            // Every other event type is none of our business. The service asks for as little as
            // it can get away with, which is what Play review looks for.
            else -> Unit
        }
    }

    /**
     * Decides whether the bubble should be showing, and where.
     *
     * A window change is handled the same way as a focus change: re-check whether there is still
     * an editable field. Hiding blindly on a window change made the bubble flicker away as soon as
     * a search screen or dialog opened.
     */
    private fun handleFocus(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString().orEmpty()

        // Ignore, never hide, for events we cause ourselves.
        //
        // The bubble is our own window, so adding it fires a window event from our own package.
        // Treating that as "the user left the app" made the bubble hide itself the instant it
        // appeared. The keyboard opening is the same story: its events arrive while the focused
        // node is briefly not the text field.
        if (packageName == applicationContext.packageName) return
        if (isInputMethod(packageName)) return

        // 1. Is the bubble switched on?
        if (!settings.settings.value.bubbleEnabled) return hideBubble("bubble off")

        // 2. Is this app on the blocked list? Read from memory, never from disk.
        if (packageName.isEmpty() || settings.isAppBlocked(packageName)) return hideBubble("blocked app")

        val node = findEditableNode()
        if (node == null) {
            // Focus reads as empty for a moment whenever the keyboard opens or the screen
            // re-lays-out. Only give up if the user has actually left the app we are showing
            // the bubble for, otherwise it disappears the instant it appears.
            if (currentField != null && currentField?.packageName != packageName) hideBubble("left app")
            return
        }

        val bounds = Rect().also { node.getBoundsInScreen(it) }
        if (bounds.width() < MIN_FIELD_PX || bounds.height() < MIN_FIELD_PX) return hideBubble("field too small")

        val field = FocusedField(packageName, bounds)
        if (field == currentField) return

        Log.d(TAG, "Field focused in $packageName at $bounds")
        currentField = field
        scheduleShow(field)
    }

    private fun scheduleShow(field: FocusedField) {
        pendingShow?.let(handler::removeCallbacks)
        val runnable = Runnable {
            Log.d(TAG, "Showing bubble near ${field.bounds}")
            overlay?.showNear(field.bounds)
        }
        pendingShow = runnable
        handler.postDelayed(runnable, FOCUS_DEBOUNCE_MS)
    }

    /**
     * Takes the bubble down.
     *
     * The dragged position deliberately survives this: the user moved the bubble where they wanted
     * it, and putting it back beside the next field would undo that every time they change field.
     */
    private fun hideBubble(reason: String = "unspecified") {
        if (currentField == null && pendingShow == null) return
        Log.d(TAG, "Hiding bubble: $reason")
        pendingShow?.let(handler::removeCallbacks)
        pendingShow = null
        currentField = null
        overlay?.hide()
    }

    /** True for keyboards and other input methods, which we must ignore. */
    private fun isInputMethod(packageName: String): Boolean =
        packageName.contains("inputmethod", ignoreCase = true) ||
            packageName.contains("latin", ignoreCase = true) ||
            packageName == "com.android.systemui"

    // -- actions -----------------------------------------------------------------------------

    private fun onBubbleTapped() {
        scope.launch {
            val tone = resolveTone()
            if (tone == null) {
                flash("No tones available", isError = true)
            } else {
                rephraseWith(tone)
            }
        }
    }

    /**
     * The tone a plain tap uses: whatever was used last, then the default from Settings, then the
     * most-used tone. A tap should never open a menu — that is what the long-press is for.
     */
    private suspend fun resolveTone(): Tone? {
        lastUsedTone?.let { return it }

        val all = tones.observeTones().first()
        if (all.isEmpty()) return null

        val defaultId = settings.settings.value.defaultToneId
        return all.firstOrNull { it.id == defaultId }
            ?: all.maxByOrNull { it.usageCount }
            ?: all.first()
    }

    private fun onBubbleLongPressed() {
        scope.launch {
            val all = tones.observeTones().first()
            if (all.isEmpty()) {
                flash("No tones available", isError = true)
            } else {
                overlay?.showToneSheet(all)
            }
        }
    }

    private fun onToneChosen(tone: Tone) {
        overlay?.hideSheet()
        lastUsedTone = tone
        scope.launch { rephraseWith(tone) }
    }

    private fun rephraseWith(tone: Tone) {
        val field = currentField ?: return
        val node = findEditableNode()
        val draft = node?.text?.toString().orEmpty()

        // Windows fails silently here, which leaves the user tapping a bubble that does nothing.
        if (draft.isBlank()) {
            flash("Nothing to rephrase")
            return
        }

        rephraseJob?.cancel()
        rephraseJob = scope.launch {
            overlay?.setWorking(true)
            try {
                val result = rephraseText(tone, draft) { stage ->
                    overlay?.setStatus(
                        when (stage) {
                            RephraseStage.REPHRASING -> "Rephrasing…"
                            RephraseStage.CHECKING -> "Checking result…"
                        }
                    )
                }

                lastUsedTone = tone
                val written = writeBack(field, result.text)
                overlay?.setWorking(false)

                if (written) {
                    // A rephrase is never a one-way door: the draft the user actually wrote is
                    // one tap away until the chip goes.
                    overlay?.setStatus(
                        text = "Done ✓",
                        actions = listOf(BubbleAction("Undo") { undo(field, draft) }),
                    )
                    clearStatusAfter(ACTION_MESSAGE_MS)
                } else {
                    copyToClipboard(result.text)
                    overlay?.setStatus("Copied — paste it in")
                    clearStatusAfter(MESSAGE_MS)
                }
            } catch (e: RephraseError) {
                overlay?.setWorking(false)
                overlay?.setStatus(
                    text = e.userMessage,
                    isError = true,
                    actions = listOf(BubbleAction("Retry") { scope.launch { rephraseWith(tone) } }),
                )
                clearStatusAfter(ACTION_MESSAGE_MS)
            } catch (e: Exception) {
                overlay?.setWorking(false)
                overlay?.setStatus("The rephrase could not be generated.", isError = true)
                clearStatusAfter(MESSAGE_MS)
            }
        }
    }

    /** Puts the draft the user wrote back into the field. */
    private fun undo(field: FocusedField, originalText: String) {
        scope.launch {
            val restored = writeBack(field, originalText)
            if (restored) {
                overlay?.setStatus("Your text is back")
            } else {
                overlay?.setStatus("Could not undo — the field has moved on.", isError = true)
            }
            clearStatusAfter(MESSAGE_MS)
        }
    }

    /** A message with no action, cleared on its own. */
    private fun flash(text: String, isError: Boolean = false) {
        overlay?.setStatus(text, isError = isError)
        clearStatusAfter(MESSAGE_MS)
    }

    private fun clearStatusAfter(delayMs: Long) {
        handler.removeCallbacks(clearStatus)
        handler.postDelayed(clearStatus, delayMs)
    }

    private val clearStatus = Runnable { overlay?.setStatus(null) }

    /**
     * Writes the result into the field.
     *
     * Two rules taken from the Windows app: fetch the node again, because the one captured when
     * the bubble appeared can be stale by now, especially in apps built on WebView; and refuse to
     * write if the user switched apps while the AI was working.
     */
    private suspend fun writeBack(field: FocusedField, text: String): Boolean =
        withContext(Dispatchers.Main) {
            val live = findEditableNode() ?: return@withContext false

            val livePackage = live.packageName?.toString().orEmpty()
            if (livePackage != field.packageName) {
                overlay?.setStatus(
                    "You switched apps before the rephrase finished.",
                    isError = true,
                )
                return@withContext false
            }

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text,
                )
            }
            live.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clipboard?.setPrimaryClip(
            android.content.ClipData.newPlainText("RephraseGenie", text)
        )
    }

    /** The focused node, if it is a real editable field. */
    private fun findEditableNode(): AccessibilityNodeInfo? {
        val node = runCatching { findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
            ?: return null
        return if (node.isEditable && node.isEnabled && !node.isPassword) node else null
    }

    private fun applyAppearance() {
        scope.launch {
            settings.observeSettings().collect { current ->
                val accent = runCatching {
                    android.graphics.Color.parseColor(current.accentColor)
                }.getOrDefault(DEFAULT_ACCENT_ARGB)
                overlay?.setAppearance(accent, dark = isDarkTheme(current.themeMode))
                if (!current.bubbleEnabled) hideBubble("setting off")
            }
        }
    }

    /** The overlay is drawn outside Compose, so it has to work the theme out for itself. */
    private fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM ->
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    override fun onInterrupt() {
        hideBubble("interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clearStatus)
        hideBubble("destroyed")
        scope.cancel()
    }

    private companion object {
        const val TAG = "RephraseA11yService"
        const val FOCUS_DEBOUNCE_MS = 150L
        const val MESSAGE_MS = 3000L

        /** Long enough to notice an Undo or Retry button and reach for it. */
        const val ACTION_MESSAGE_MS = 8000L
        const val MIN_FIELD_PX = 24
        const val DEFAULT_ACCENT_ARGB = 0xFF8AB4FF.toInt()
    }
}
