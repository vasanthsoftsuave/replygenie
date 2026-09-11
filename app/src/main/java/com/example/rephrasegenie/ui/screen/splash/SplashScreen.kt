package com.example.rephrasegenie.ui.screen.splash

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rephrasegenie.R
import com.example.rephrasegenie.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/*
 * The intro video, shown once at launch in place of a static splash.
 *
 * The file lives at `app/src/main/res/raw/splash_video.mp4`. `raw`, not `drawable`: `drawable`
 * holds images and the platform will not play a video from it. A GIF would not work here either —
 * Android has no built-in GIF decoder for views — so an mp4 is the right format.
 *
 * Built on MediaPlayer and a TextureView rather than the simpler VideoView, for two reasons:
 *
 * - **The video is landscape and the phone is not.** VideoView only ever letterboxes, so a 16:9
 *   clip sat in a band with dark above and below it. A TextureView can be given a transform, which
 *   is what makes centre-crop possible.
 * - **VideoView will not open the file until its surface exists**, which means waiting for Compose
 *   to compose, measure and lay it out before decoding even starts. Here the player is handed the
 *   file and told to prepare during composition, and the surface is attached whenever it turns up.
 */

/** Used until the player reports how long the video actually is. */
private const val PREPARE_TIMEOUT_MS = 4_000L

/** Hard ceiling. Nobody should be held on an intro longer than this, whatever the file says. */
private const val MAX_SPLASH_MS = 12_000L

/** A little air after the last frame, so the cut does not land mid-motion. */
private const val TAIL_MS = 250L

/** Just enough to take the edge off the first frame appearing. */
private const val FADE_IN_MS = 180

/**
 * How the landscape clip is placed on a portrait screen.
 *
 * FIT shows the whole frame, letterboxed on the theme background. CROP fills the screen and throws
 * away whichever axis overflows. How much that costs depends entirely on the clip: a 16:9 one lost
 * about three quarters of its width here and cut the text in half, while the 9:16 one in use now
 * loses roughly 6% off each side, which its margins absorb. CROP, because black bars on a launch
 * screen look like something failed to load.
 */
private enum class VideoFit { FIT, CROP }

private val VIDEO_FIT = VideoFit.CROP

private fun videoUri(context: Context): Uri =
    Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video_new}")

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val finish by rememberUpdatedState(onFinished)

    // Starts as a short "did the player even wake up" timeout, and is replaced with the real
    // duration once the video is prepared. Either way the splash always ends: a file that will not
    // decode, or a player that never reports completion, cannot strand the user on the intro.
    var timeoutMs by remember { mutableLongStateOf(PREPARE_TIMEOUT_MS) }
    var firstFrameRendered by remember { mutableStateOf(false) }

    val playback = remember {
        SplashPlayback().apply {
            onFirstFrame = { firstFrameRendered = true }
            onDuration = { durationMs ->
                timeoutMs = (durationMs + TAIL_MS).coerceAtMost(MAX_SPLASH_MS)
            }
            onEnded = { finish() }
            open(context)
        }
    }

    DisposableEffect(playback) {
        onDispose { playback.release() }
    }


    LaunchedEffect(timeoutMs) {
        delay(timeoutMs)
        finish()
    }

    // Held back until there is actually something to show, so the handover from the launch window
    // is a fade rather than a black flash from an empty surface.
    val videoAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered) 1f else 0f,
        animationSpec = tween(FADE_IN_MS),
        label = "splashVideoAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            // The cropped video is drawn wider than the screen, so the overflow has to be clipped.
            .clipToBounds(),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(videoAlpha),
            factory = { ctx ->
                TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    surfaceTextureListener = playback.surfaceListener(this)
                }
            },
        )
    }
}

/**
 * Owns the MediaPlayer and the two things that have to meet before playback can start: the file
 * being prepared, and the surface being created. They arrive in either order, so each one checks
 * whether the other has already happened.
 */
private class SplashPlayback {

    private val player = MediaPlayer()
    private var view: TextureView? = null
    private var prepared = false
    private var surfaceReady = false
    private var videoWidth = 0
    private var videoHeight = 0
    private var released = false

    var onFirstFrame: () -> Unit = {}
    var onDuration: (Long) -> Unit = {}
    var onEnded: () -> Unit = {}

    fun open(context: Context) {
        player.setOnPreparedListener { mp ->
            prepared = true
            videoWidth = mp.videoWidth
            videoHeight = mp.videoHeight
            applyScale()
            val duration = mp.duration.toLong()
            onDuration(if (duration > 0) duration else MAX_SPLASH_MS)
            startIfReady()
        }
        player.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) onFirstFrame()
            false
        }
        player.setOnCompletionListener { onEnded() }
        // A video that will not decode on this device is not worth a stack trace in front of the
        // user. Treat it as no splash at all.
        player.setOnErrorListener { _, _, _ ->
            onEnded()
            true
        }

        // Decoding is the slow part of opening a video, so it is kicked off now rather than after
        // the view has been laid out. The surface is attached later, whenever it appears.
        val started = runCatching {
            player.setDataSource(context, videoUri(context))
            player.prepareAsync()
        }.isSuccess

        if (!started) onEnded()
    }

    fun surfaceListener(textureView: TextureView): TextureView.SurfaceTextureListener {
        view = textureView
        return object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                if (released) return
                runCatching { player.setSurface(Surface(surface)) }
                surfaceReady = true
                applyScale()
                startIfReady()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int,
            ) = applyScale()

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
    }

    private fun startIfReady() {
        if (released || !prepared || !surfaceReady) return
        runCatching { player.start() }
    }

    /**
     * Places the video in the view according to [VIDEO_FIT].
     *
     * A TextureView stretches its surface to the view bounds before any transform is applied, so
     * the matrix has to undo that stretch and then apply the scale we actually want. Working
     * straight from the video size would double-count the stretch and blow the picture up.
     */
    private fun applyScale() {
        val target = view ?: return
        val viewWidth = target.width
        val viewHeight = target.height
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

        val widthRatio = viewWidth.toFloat() / videoWidth
        val heightRatio = viewHeight.toFloat() / videoHeight
        val fit = when (VIDEO_FIT) {
            VideoFit.FIT -> min(widthRatio, heightRatio)
            VideoFit.CROP -> max(widthRatio, heightRatio)
        }
        val scaleX = (videoWidth * fit) / viewWidth
        val scaleY = (videoHeight * fit) / viewHeight

        target.setTransform(
            Matrix().apply { setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f) }
        )
    }

    fun release() {
        released = true
        view = null
        runCatching { player.setSurface(null) }
        runCatching { player.release() }
    }
}
