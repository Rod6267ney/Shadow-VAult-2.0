package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.settings.SettingsManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.IntOffset

@Composable
fun AppNavigation(windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null) {
    val context = LocalContext.current
    val settingsManager = SettingsManager(context)
    val isOnboarded by settingsManager.isOnboarded.collectAsState(initial = false)
    val realPin by settingsManager.realPin.collectAsState(initial = null)
    val navController = rememberNavController()

    // [SECURITY] Global lock state observer has been REMOVED per user request
    // The app will now only require a PIN on initial cold start.


    // We can't navigate properly until we know if a pin exists. Wait for realPin state if possible,
    // but compose doesn't let us easily block. We'll use startDestination based on realPin
    // Actually, setting a dynamic startDestination isn't great.
    // Let's have a "splash" or determination composable as start.
    val startDestination = "launcher"

    val springSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    val fadeSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = springSpec) +
            fadeIn(animationSpec = fadeSpringSpec)
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = springSpec) +
            fadeOut(animationSpec = fadeSpringSpec)
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = springSpec) +
            fadeIn(animationSpec = fadeSpringSpec)
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = springSpec) +
            fadeOut(animationSpec = fadeSpringSpec)
        }
    ) {
        composable("launcher") {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (!isOnboarded) {
                    navController.navigate("onboarding") { popUpTo("launcher") { inclusive = true } }
                } else {
                    navController.navigate("login") { popUpTo("launcher") { inclusive = true } }
                }
            }
            // Empty Box while navigating
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
        }
        composable("onboarding") {
            OnboardingScreen(
                onPinSet = { pin ->
                    com.example.services.AppLockManager.unlockApp()
                    navController.navigate("dashboard/true") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { isReal ->
                    navController.navigate("dashboard/$isReal") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "dashboard/{isReal}",
            arguments = listOf(androidx.navigation.navArgument("isReal") { type = androidx.navigation.NavType.BoolType })
        ) { backStackEntry ->
            val isReal = backStackEntry.arguments?.getBoolean("isReal") ?: true
            DashboardScreen(
                isReal = isReal,
                onLock = {
                    navController.navigate("login") {
                        popUpTo("dashboard/{isReal}") { inclusive = true }
                    }
                },
                navController = navController,
                windowSizeClass = windowSizeClass
            )
        }
    }
}
