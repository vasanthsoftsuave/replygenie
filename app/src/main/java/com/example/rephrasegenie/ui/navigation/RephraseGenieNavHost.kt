package com.example.rephrasegenie.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rephrasegenie.ui.screen.home.HomeScreen
import com.example.rephrasegenie.ui.screen.settings.BlockedAppsScreen
import com.example.rephrasegenie.ui.screen.settings.SettingsScreen
import com.example.rephrasegenie.ui.screen.setup.SetupScreen
import com.example.rephrasegenie.ui.screen.tonebuilder.ToneBuilderScreen

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val BLOCKED_APPS = "blockedApps"

    const val ARG_TONE_ID = "toneId"

    /** Stands in for "no id" on the tone builder route, which cannot take an empty path segment. */
    const val NEW_TONE = "new"

    const val TONE_BUILDER = "toneBuilder/{$ARG_TONE_ID}"

    fun toneBuilder(toneId: String? = null): String = "toneBuilder/${toneId ?: NEW_TONE}"
}

/** Long enough to read as movement, short enough not to sit between the user and the next screen. */
private const val SLIDE_MS = 280

/**
 * Screens slide horizontally, in the direction the user is travelling: a new screen comes in from
 * the right and the one behind it drifts a quarter of the way left, then the whole thing reverses
 * on back. The partial travel of the outgoing screen is what makes the two read as a stack rather
 * than two unrelated pages swapping.
 */
@Composable
fun RephraseGenieNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val spec = tween<Float>(SLIDE_MS, easing = FastOutSlowInEasing)
    val offsetSpec = tween<IntOffset>(SLIDE_MS, easing = FastOutSlowInEasing)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, offsetSpec) + fadeIn(spec)
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Start, offsetSpec) { it / 4 } + fadeOut(spec)
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.End, offsetSpec) { it / 4 } + fadeIn(spec)
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, offsetSpec) + fadeOut(spec)
        },
    ) {

        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAddBlockedApp = { navController.navigate(Routes.BLOCKED_APPS) },
                onNewTone = { navController.navigate(Routes.toneBuilder()) },
                onEditTone = { toneId -> navController.navigate(Routes.toneBuilder(toneId)) },
            )
        }

        composable(Routes.BLOCKED_APPS) {
            BlockedAppsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.TONE_BUILDER,
            arguments = listOf(
                navArgument(Routes.ARG_TONE_ID) {
                    type = NavType.StringType
                    defaultValue = Routes.NEW_TONE
                }
            ),
        ) {
            ToneBuilderScreen(onDone = { navController.popBackStack() })
        }
    }
}
