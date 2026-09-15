package com.example.rephrasegenie.overlay

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
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
import com.example.rephrasegenie.domain.usecase.RephraseTextUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What became of an attempt to put text back into the field. */
private enum class WriteResult {
    WRITTEN,

    /** The user moved to another app while the AI was working, so nothing was written (§4.3). */
    WRONG_APP,

    /** The field is gone, or the app blocks ACTION_SET_TEXT. */
    FAILED,
}

/**
 * The core of the app: keeps the bubble on screen while the switch is on, and on tap rephrases
 * whatever the user has typed in the field they are in and writes it back.
 */
@AndroidEntryPoint
class RephraseAccessibilityService : AccessibilityService() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var tones: ToneRepository
    @Inject lateinit var rephraseText: RephraseTextUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private var overlay: BubbleOverlay? = null
    private var rephraseJob: Job? = null

    /** The app in front, so the bubble can step aside in apps on the blocked list. */
    private var foregroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = BubbleOverlay(
            service = this,
            onTap = ::onBubbleTapped,
            onLongPress = ::onBubbleLongPressed,
            onToneChosen = ::onToneChosen,
        )
        observeSettings()
        Log.i(TAG, "Service connected.")
    }

    /**
     * The only thing an event decides now is whether the app in front is on the blocked list.
     *
     * The bubble used to chase the focused field, which meant handling a focus and a
     * text-selection event for every keystroke the user typed in any app, each one walking the
     * node tree to re-read the field's bounds. It is on screen from the moment the switch goes on
     * instead, so those event types are no longer requested at all (see
     * accessibility_service_config.xml) and the field is read once, at the moment of the tap.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName.isEmpty()) return

        // Our own windows are not "the app in front": adding the bubble fires an event from our
        // own package, and treating that as an app change would have it react to itself. The
        // keyboard is skipped for the same reason — it opens on top of the app underneath, which
        // is the one that counts.
        if (packageName == applicationContext.packageName) return
        if (isInputMethod(packageName)) return

        if (packageName == foregroundPackage) return
        foregroundPackage = packageName
        syncBubble()
    }

    /** The bubble is up whenever the switch is on and the app in front is not blocked. */
    private fun syncBubble() {
        val enabled = settings.settings.value.bubbleEnabled
        // Read from memory, never from disk: this runs on every app change.
        val blocked = foregroundPackage?.let(settings::isAppBlocked) == true

        if (enabled && !blocked) {
            overlay?.show()
        } else {
            hideBubble(if (!enabled) "switch off" else "blocked app")
        }
    }

    /**
     * Takes the bubble down.
     *
     * The dragged position deliberately survives this: the user moved the bubble where they wanted
     * it, and starting it back at the default every time they pass through a blocked app would
     * undo that for them.
     */
    private fun hideBubble(reason: String = "unspecified") {
        handler.removeCallbacks(clearStatus)
        Log.d(TAG, "Hiding bubble: $reason")
        overlay?.hide()
    }

    /** True for keyboards and other input methods, which we must ignore. */
    private fun isInputMethod(packageName: String): Boolean =
        packageName.contains("inputmethod", ignoreCase = true) ||
            packageName.contains("latin", ignoreCase = true) ||
            packageName == "com.android.systemui"

    // -- actions -----------------------------------------------------------------------------

    private fun onBubbleTapped() {
        // A second tap while a rephrase is in flight aborts it. Starting over instead would throw
        // away a run that may be five of its six API calls in (§8.3), on the user's own OpenAI
        // key — too much to charge someone for a fumbled tap.
        if (rephraseJob?.isActive == true) {
            cancelRephrase()
            return
        }

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
     * The tone a plain tap uses: the one picked in the sheet, then the most-used. A tap never
     * opens a menu — that is what the long-press is for.
     */
    private suspend fun resolveTone(): Tone? = pickTone(tones.observeTones().first())

    private fun pickTone(all: List<Tone>): Tone? {
        if (all.isEmpty()) return null
        val selectedId = settings.settings.value.defaultToneId
        return all.firstOrNull { it.id == selectedId } ?: all.maxByOrNull { it.usageCount }
    }

    private fun cancelRephrase() {
        rephraseJob?.cancel()
        rephraseJob = null
        overlay?.setWorking(false)
    }

    private fun onBubbleLongPressed() {
        scope.launch {
            val all = tones.observeTones().first()
            if (all.isEmpty()) {
                flash("No tones available", isError = true)
                return@launch
            }
            overlay?.showToneSheet(all, selectedToneId = pickTone(all)?.id)
        }
    }

    /**
     * Picking a tone only picks it.
     *
     * It used to start a rephrase on the spot, which left no way to change tone without spending
     * an API call on the user's own key to do it. The choice is stored, so it is still there after
     * the service restarts, and the tick in the sheet reads back from the same place.
     */
    private fun onToneChosen(tone: Tone) {
        overlay?.hideSheet()
        scope.launch { settings.update { it.copy(defaultToneId = tone.id) } }
    }

    /**
     * Nothing is said while this runs. The ring sweeping round the bubble is the progress report,
     * and a chip that appeared, changed its wording twice and vanished again was one more thing
     * moving on screen at the moment the user is waiting to read the result.
     */
    private fun rephraseWith(tone: Tone) {
        // Read now rather than tracked as focus moves: the bubble no longer follows the field, so
        // the field is whatever the user is in at the moment they tap.
        val node = findEditableNode()
        if (node == null) {
            flash("Tap a text field first")
            return
        }

        val draft = node.text?.toString().orEmpty()
        // Windows fails silently here, which leaves the user tapping a bubble that does nothing.
        if (draft.isBlank()) {
            flash("Nothing to rephrase")
            return
        }

        val targetPackage = node.packageName?.toString().orEmpty()

        rephraseJob?.cancel()
        rephraseJob = scope.launch {
            overlay?.setWorking(true)
            try {
                val result = rephraseText(tone, draft)
                overlay?.setWorking(false)

                // Each outcome says its own piece. They used to share one branch, so switching
                // apps mid-rephrase reported "Copied — paste it in" over the top of the real
                // reason, and the user was told to paste something that was never copied.
                when (writeBack(targetPackage, result.text)) {
                    WriteResult.WRITTEN -> {
                        // The one thing worth a button: a rephrase is never a one-way door, and
                        // the draft the user actually wrote is one tap away until the chip goes.
                        showStatus(
                            text = "Done ✓",
                            actions = listOf(BubbleAction("Undo") { undo(targetPackage, draft) }),
                        )
                        clearStatusAfter(ACTION_MESSAGE_MS)
                    }

                    WriteResult.WRONG_APP ->
                        flash("You switched apps before the rephrase finished.", isError = true)

                    WriteResult.FAILED -> {
                        // Some apps block ACTION_SET_TEXT. The clipboard is the way out (§4.3).
                        copyToClipboard(result.text)
                        flash("Copied — paste it in")
                    }
                }
            } catch (e: RephraseError) {
                overlay?.setWorking(false)
                // The reason, with nothing to press. A Retry button spends another call on the
                // user's own key, and tapping the bubble again already does exactly that.
                flash(e.userMessage, isError = true)
            } catch (e: CancellationException) {
                // Cancelling is the user getting what they asked for, not a failure. It has to be
                // caught above the generic handler, or a cancelled run reports itself as broken —
                // and rethrowing is what keeps structured concurrency intact.
                throw e
            } catch (e: Exception) {
                overlay?.setWorking(false)
                flash("The rephrase could not be generated.", isError = true)
            }
        }
    }

    /**
     * Puts the draft the user wrote back into the field.
     *
     * Says nothing when it works — the user is looking at their own words back in the field, which
     * is the whole confirmation. Only a failure needs wording.
     */
    private fun undo(targetPackage: String, originalText: String) {
        scope.launch {
            if (writeBack(targetPackage, originalText) == WriteResult.WRITTEN) {
                overlay?.setStatus(null)
            } else {
                flash("Could not undo — the field has moved on.", isError = true)
            }
        }
    }

    /**
     * Puts a message on the chip and drops any auto-clear left over from an earlier one.
     *
     * Without that second part a clear scheduled by the previous message fires part-way through
     * this one and wipes it, which is how progress text went missing when events arrived in quick
     * succession.
     */
    private fun showStatus(
        text: String,
        isError: Boolean = false,
        actions: List<BubbleAction> = emptyList(),
    ) {
        handler.removeCallbacks(clearStatus)
        overlay?.setStatus(text, isError, actions)
    }

    /** A message with no action, cleared on its own. */
    private fun flash(text: String, isError: Boolean = false) {
        showStatus(text, isError = isError)
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
    private suspend fun writeBack(targetPackage: String, text: String): WriteResult =
        withContext(Dispatchers.Main) {
            val live = findEditableNode() ?: return@withContext WriteResult.FAILED

            val livePackage = live.packageName?.toString().orEmpty()
            if (livePackage != targetPackage) return@withContext WriteResult.WRONG_APP

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text,
                )
            }

            if (live.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                WriteResult.WRITTEN
            } else {
                WriteResult.FAILED
            }
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

    /**
     * Keeps the bubble's look and its presence in step with the settings.
     *
     * This is what makes the switch on the Home screen immediate: flipping it on puts the bubble
     * up there and then, rather than at the next time the user happens to tap into a text field.
     */
    private fun observeSettings() {
        scope.launch {
            settings.observeSettings().collect { current ->
                val accent = runCatching {
                    android.graphics.Color.parseColor(current.accentColor)
                }.getOrDefault(DEFAULT_ACCENT_ARGB)
                overlay?.setAppearance(accent, dark = isDarkTheme(current.themeMode))
                syncBubble()
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

    /**
     * Clears any message, but leaves the bubble up.
     *
     * The bubble is not feedback the system is asking us to stop — it is a control the user
     * switched on, and taking it away here would leave them with nothing to tap and no way to
     * understand why.
     */
    override fun onInterrupt() {
        handler.removeCallbacks(clearStatus)
        overlay?.setStatus(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clearStatus)
        hideBubble("destroyed")
        scope.cancel()
    }

    private companion object {
        const val TAG = "RephraseA11yService"
        const val MESSAGE_MS = 3000L

        /** Long enough to notice the Undo button and reach for it. */
        const val ACTION_MESSAGE_MS = 8000L
        const val DEFAULT_ACCENT_ARGB = 0xFF8AB4FF.toInt()
    }
}
