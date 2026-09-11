package com.example.rephrasegenie.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.rephrasegenie.domain.model.Tone
import kotlin.math.abs
import kotlin.math.roundToInt

/** A button offered beside the status chip, such as Undo or Retry. */
data class BubbleAction(val label: String, val onClick: () -> Unit)

/**
 * Draws the floating bubble, the status chip and the tone sheet.
 *
 * Uses TYPE_APPLICATION_OVERLAY, which needs the "draw over other apps" permission.
 *
 * TYPE_ACCESSIBILITY_OVERLAY would avoid that permission, but adding one through a plain
 * WindowManager silently creates no window — addView throws nothing and nothing appears. It needs
 * the newer AccessibilityService.attachAccessibilityOverlayToDisplay path, which is API 34 and up.
 */
class BubbleOverlay(
    private val service: AccessibilityService,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onToneChosen: (Tone) -> Unit,
) {
    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val overlayWindowType =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private var bubbleView: View? = null
    private var sheetView: View? = null
    private var sheetScrim: View? = null
    private var statusView: LinearLayout? = null

    private val bubbleSizePx = dp(48)
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop

    /**
     * Where the user last dropped the bubble. Once set, the bubble stops following the field and
     * stays put, which is what "the position is remembered" has to mean for a draggable bubble.
     */
    private var manualPosition: Pair<Int, Int>? = null

    private var snapAnimator: ValueAnimator? = null
    private var glyphPulse: ValueAnimator? = null

    private var accentColor: Int = Color.parseColor("#8AB4FF")
    private var isDark: Boolean = true

    fun setAppearance(accent: Int, dark: Boolean) {
        accentColor = accent
        isDark = dark
        bubbleView?.let { applyBubbleStyle(it) }
        bubbleView?.findViewWithTag<GeneratingRing>(TAG_RING)?.setAccent(accent)
    }

    fun showNear(bounds: Rect) {
        hideSheet()
        val view = bubbleView ?: createBubble().also { bubbleView = it }

        val (x, y) = manualPosition ?: positionFor(bounds)
        val params = view.layoutParams as? WindowManager.LayoutParams ?: bubbleParams()
        params.x = x
        params.y = y

        if (view.isAttachedToWindow) {
            windowManager.updateViewLayout(view, params)
        } else {
            addViewLogged(view, params)
        }
        view.visibility = View.VISIBLE
    }

    fun hide() {
        snapAnimator?.cancel()
        hideSheet()
        bubbleView?.let { view ->
            if (view.isAttachedToWindow) runCatching { windowManager.removeView(view) }
        }
        bubbleView = null
        setStatus(null)
    }

    /**
     * Shows that a rephrase is running: a shimmer sweeping round the rim and the sparkle breathing
     * in the middle. The sparkle deliberately stays put rather than being swapped for a spinner —
     * it is the same bubble doing the same job, not a different control.
     */
    fun setWorking(working: Boolean) {
        val view = bubbleView ?: return
        val ring = view.findViewWithTag<GeneratingRing>(TAG_RING)
        val glyph = view.findViewWithTag<TextView>(TAG_GLYPH)

        if (working) {
            ring?.visibility = View.VISIBLE
            ring?.start()
            glyph?.let(::startGlyphPulse)
        } else {
            ring?.stop()
            ring?.visibility = View.GONE
            stopGlyphPulse(glyph)
        }
    }

    private fun startGlyphPulse(glyph: View) {
        if (glyphPulse?.isRunning == true) return
        glyphPulse = ValueAnimator.ofFloat(1f, 0.72f).apply {
            duration = PULSE_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                glyph.scaleX = scale
                glyph.scaleY = scale
                glyph.alpha = scale
            }
            start()
        }
    }

    private fun stopGlyphPulse(glyph: View?) {
        glyphPulse?.cancel()
        glyphPulse = null
        glyph?.scaleX = 1f
        glyph?.scaleY = 1f
        glyph?.alpha = 1f
    }

    /**
     * The small chip beside the bubble. Replaces the Windows toast, which vanished before it could
     * be read and offered nothing to do about a failure.
     *
     * With no [actions] the chip is not touchable, so it never steals a tap meant for the app
     * underneath.
     */
    fun setStatus(
        text: String?,
        isError: Boolean = false,
        actions: List<BubbleAction> = emptyList(),
    ) {
        removeStatusView()
        if (text == null) return

        // A chip with no bubble beside it has nothing to point at, and would be placed off the
        // top-left corner of the screen. This happens when a result arrives after focus has
        // already moved on, and the message is not worth showing by then anyway.
        if (bubbleView == null) return

        val container = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(if (isDark) Color.parseColor("#E6161927") else Color.parseColor("#F2FFFFFF"))
                setStroke(dp(1), if (isDark) Color.parseColor("#252B3E") else Color.parseColor("#D4DCE8"))
            }
        }

        container.addView(
            TextView(service).apply {
                this.text = text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                maxWidth = dp(220)
                setTextColor(
                    when {
                        isError -> Color.parseColor("#FF5C7A")
                        isDark -> Color.parseColor("#EEF0F5")
                        else -> Color.parseColor("#0F172A")
                    }
                )
            }
        )

        actions.forEach { action ->
            container.addView(
                TextView(service).apply {
                    this.text = action.label
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setTextColor(accentColor)
                    setPadding(dp(10), dp(2), dp(2), dp(2))
                    isClickable = true
                    setOnClickListener { action.onClick() }
                }
            )
        }

        addViewLogged(container, statusParams(touchable = actions.isNotEmpty()))
        statusView = container

        // Placed once now and again after layout: the chip wraps its text, so its width — which
        // decides whether it fits on screen — is not known until it has been measured.
        repositionStatus()
        container.post { repositionStatus() }
    }

    /** Keeps the chip beside the bubble, on screen, wherever the bubble has got to. */
    private fun repositionStatus() {
        val chip = statusView ?: return
        val bubbleParams = bubbleView?.layoutParams as? WindowManager.LayoutParams ?: return
        val params = chip.layoutParams as? WindowManager.LayoutParams ?: return

        val metrics = service.resources.displayMetrics
        val maxX = (metrics.widthPixels - chip.width).coerceAtLeast(0)
        params.x = bubbleParams.x.coerceIn(0, maxX)
        params.y = (bubbleParams.y - dp(40)).coerceAtLeast(0)

        if (chip.isAttachedToWindow) {
            runCatching { windowManager.updateViewLayout(chip, params) }
        }
    }

    // The scrim is a bare catcher with no click semantics of its own: any touch dismisses the
    // sheet, which is why it does not go through performClick.
    @SuppressLint("ClickableViewAccessibility")
    fun showToneSheet(tones: List<Tone>) {
        hideSheet()
        val bubbleParams = bubbleView?.layoutParams as? WindowManager.LayoutParams

        // A full-screen catcher behind the sheet, so tapping anywhere else closes it. Without one
        // the sheet stays up until the field loses focus, which reads as the app being stuck.
        val scrim = View(service).apply {
            setOnTouchListener { _, _ ->
                hideSheet()
                true
            }
        }
        addViewLogged(scrim, scrimParams())
        sheetScrim = scrim

        val container = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) Color.parseColor("#161927") else Color.WHITE)
                setStroke(dp(1), if (isDark) Color.parseColor("#252B3E") else Color.parseColor("#D4DCE8"))
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        // Tapping a tone rephrases straight away. On Windows this only changed the default and the
        // user then had to click again, which is three actions for one job.
        tones.take(MAX_SHEET_TONES).forEach { tone ->
            container.addView(createTonePill(tone))
        }

        val scroll = ScrollView(service).apply { addView(container) }

        val metrics = service.resources.displayMetrics
        val sheetWidth = dp(220)
        val params = WindowManager.LayoutParams(
            sheetWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Opens on whichever side of the bubble has room, so it is never half off-screen.
            val bubbleX = bubbleParams?.x ?: 0
            x = if (bubbleX + bubbleSizePx + sheetWidth <= metrics.widthPixels) {
                bubbleX + bubbleSizePx + dp(6)
            } else {
                (bubbleX - sheetWidth - dp(6)).coerceAtLeast(dp(8))
            }
            y = ((bubbleParams?.y ?: 0) + bubbleSizePx + dp(6))
                .coerceAtMost(metrics.heightPixels - dp(160))
                .coerceAtLeast(dp(8))
        }

        addViewLogged(scroll, params)
        sheetView = scroll
    }

    fun hideSheet() {
        sheetView?.let { view ->
            if (view.isAttachedToWindow) runCatching { windowManager.removeView(view) }
        }
        sheetView = null
        sheetScrim?.let { view ->
            if (view.isAttachedToWindow) runCatching { windowManager.removeView(view) }
        }
        sheetScrim = null
    }

    fun isSheetShowing(): Boolean = sheetView != null

    // -- building blocks ---------------------------------------------------------------------

    private fun removeStatusView() {
        statusView?.let { view ->
            if (view.isAttachedToWindow) runCatching { windowManager.removeView(view) }
        }
        statusView = null
    }

    private fun createTonePill(tone: Tone): View {
        val row = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            isClickable = true
            setOnClickListener { onToneChosen(tone) }
        }

        val dotColor = runCatching { Color.parseColor(tone.color ?: "#8AB4FF") }
            .getOrDefault(accentColor)

        val dot = View(service).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                marginEnd = dp(10)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(dotColor)
            }
        }

        val label = TextView(service).apply {
            text = tone.name
            setTextColor(if (isDark) Color.parseColor("#EEF0F5") else Color.parseColor("#0F172A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        }

        row.addView(dot)
        row.addView(label)
        return row
    }

    private fun statusParams(touchable: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
    }

    private fun scrimParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun bubbleParams() = WindowManager.LayoutParams(
        bubbleSizePx,
        bubbleSizePx,
        overlayWindowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun createBubble(): View {
        val frame = FrameLayout(service).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSizePx, bubbleSizePx)
        }

        val glyph = TextView(service).apply {
            tag = TAG_GLYPH
            text = "✦"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val ring = GeneratingRing(service).apply {
            tag = TAG_RING
            visibility = View.GONE
            setAccent(accentColor)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        frame.addView(glyph)
        frame.addView(ring)
        applyBubbleStyle(frame)
        attachGestures(frame)
        return frame
    }

    private fun applyBubbleStyle(view: View) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accentColor)
        }
        view.elevation = dp(6).toFloat()
    }

    /** Tap rephrases, long-press opens the tone sheet, drag moves the bubble. */
    private fun attachGestures(view: View) {
        // Routed through performClick so a tap is a real click as far as the system is concerned,
        // and TalkBack can still reach the bubble.
        view.setOnClickListener { onTap() }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        var longPressFired = false

        val longPressRunnable = Runnable {
            longPressFired = true
            onLongPress()
        }

        view.setOnTouchListener { v, event ->
            val params = v.layoutParams as WindowManager.LayoutParams
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    longPressFired = false
                    v.postDelayed(longPressRunnable, LONG_PRESS_MS)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                        v.removeCallbacks(longPressRunnable)
                        hideSheet()
                    }
                    if (dragging) {
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        runCatching { windowManager.updateViewLayout(v, params) }
                        repositionStatus()
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.removeCallbacks(longPressRunnable)
                    if (dragging) {
                        snapToNearestEdge(v, params)
                    } else if (!longPressFired && event.actionMasked == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Slides the bubble to whichever side edge it is closest to and remembers where it landed.
     *
     * Left free-floating, a bubble dropped mid-screen sits on top of whatever the user is reading.
     * Snapping also keeps it clear of the text field it belongs to.
     */
    private fun snapToNearestEdge(view: View, params: WindowManager.LayoutParams) {
        val metrics = service.resources.displayMetrics
        val margin = dp(4)
        val centreX = params.x + bubbleSizePx / 2
        val targetX = if (centreX < metrics.widthPixels / 2) {
            margin
        } else {
            metrics.widthPixels - bubbleSizePx - margin
        }
        val targetY = params.y.coerceIn(0, metrics.heightPixels - bubbleSizePx)

        params.y = targetY
        manualPosition = targetX to targetY

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = SNAP_MS
            addUpdateListener { animator ->
                if (!view.isAttachedToWindow) return@addUpdateListener
                params.x = animator.animatedValue as Int
                runCatching { windowManager.updateViewLayout(view, params) }
                repositionStatus()
            }
            start()
        }
    }

    /** Just below and to the right of the field, matching the Windows placement. */
    private fun positionFor(bounds: Rect): Pair<Int, Int> {
        val metrics = service.resources.displayMetrics
        val x = (bounds.right - (bubbleSizePx * 0.4f)).roundToInt()
            .coerceIn(0, metrics.widthPixels - bubbleSizePx)
        val y = (bounds.bottom + dp(6))
            .coerceIn(0, metrics.heightPixels - bubbleSizePx)
        return x to y
    }

    private fun addViewLogged(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            // Some apps set HIDE_NON_SYSTEM_OVERLAY_WINDOWS and the system refuses the window.
            // Nothing can be done about that, and nothing should be, so fail quietly (§4.4).
            android.util.Log.e(TAG, "addView failed", e)
        }
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).roundToInt()

    private companion object {
        const val TAG = "BubbleOverlay"
        const val TAG_GLYPH = "glyph"
        const val TAG_RING = "ring"
        const val LONG_PRESS_MS = 400L
        const val MAX_SHEET_TONES = 20
        const val SNAP_MS = 180L
        const val PULSE_MS = 620L
    }
}
