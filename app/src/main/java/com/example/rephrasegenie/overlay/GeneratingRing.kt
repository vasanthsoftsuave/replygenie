package com.example.rephrasegenie.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * The shimmer that runs around the bubble while a rephrase is in flight.
 *
 * A sweep gradient rotated a full turn about once a second, which reads as "thinking" the way an
 * indeterminate ProgressBar — what this replaced — reads as "waiting for a file to download".
 *
 * The colours come from the user's accent rather than a fixed palette, so the effect follows
 * whichever of the eight accents they picked in Settings (§10) instead of fighting it. The white
 * band in the middle of the sweep is what makes the rotation legible; without a bright leading
 * edge a symmetric gradient barely looks like it is moving.
 */
class GeneratingRing(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f * context.resources.displayMetrics.density
    }

    private val arcBounds = RectF()
    private var angle = 0f
    private var accent = Color.parseColor("#8AB4FF")
    private var animator: ValueAnimator? = null

    fun setAccent(color: Int) {
        accent = color
        buildShader()
        invalidate()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        // Inset by half the stroke, or the ring is clipped by its own thickness.
        val inset = paint.strokeWidth / 2f
        arcBounds.set(inset, inset, width - inset, height - inset)
        buildShader()
    }

    private fun buildShader() {
        if (width == 0 || height == 0) return

        // Same hue at zero alpha, so the two ends of the sweep meet without a visible seam.
        val faded = accent and 0x00FFFFFF

        paint.shader = SweepGradient(
            width / 2f,
            height / 2f,
            intArrayOf(faded, accent, Color.WHITE, accent, faded),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (arcBounds.isEmpty) return
        canvas.save()
        canvas.rotate(angle, width / 2f, height / 2f)
        canvas.drawArc(arcBounds, 0f, 360f, false, paint)
        canvas.restore()
    }

    fun start() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = ROTATION_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                angle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    /** The bubble is torn down whenever focus is lost, so the animator must not outlive it. */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    private companion object {
        const val ROTATION_MS = 1100L
    }
}
