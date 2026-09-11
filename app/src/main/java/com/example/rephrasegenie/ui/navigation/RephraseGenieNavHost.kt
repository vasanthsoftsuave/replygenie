package com.example.rephrasegenie.ui.navigation

import androidx.compose.runtime.Composable
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

@Composable
fun RephraseGenieNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {

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
