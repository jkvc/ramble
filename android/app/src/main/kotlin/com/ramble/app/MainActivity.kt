package com.ramble.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramble.app.ui.ApiKeyScreen
import com.ramble.app.ui.SettingsScreen
import com.ramble.app.ui.TranscribeScreen
import com.ramble.app.ui.theme.RambleTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_DESTINATION = "start_destination"
        const val DESTINATION_SETTINGS = "settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = intent.getStringExtra(EXTRA_START_DESTINATION)

        setContent {
            RambleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RambleNavigation(initialDestination = startDestination)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    data object Transcribe : BottomNavItem(
        route = "transcribe",
        title = "Transcribe",
        selectedIcon = { Icon(Icons.Filled.Mic, contentDescription = "Transcribe") },
        unselectedIcon = { Icon(Icons.Outlined.Mic, contentDescription = "Transcribe") }
    )

    data object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
        unselectedIcon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
    )
}

@Composable
fun RambleNavigation(initialDestination: String? = null) {
    val navController = rememberNavController()
    val apiKeyManager = RambleApp.instance.apiKeyManager
    val apiKey by apiKeyManager.apiKey.collectAsState()

    val bottomNavItems = listOf(BottomNavItem.Transcribe, BottomNavItem.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomNav = apiKey != null &&
        currentDestination?.route in listOf("transcribe", "settings")

    LaunchedEffect(initialDestination, apiKey) {
        if (apiKey != null && initialDestination == MainActivity.DESTINATION_SETTINGS) {
            navController.navigate("settings") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { if (selected) item.selectedIcon() else item.unselectedIcon() },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        val startDest = if (apiKey != null) "transcribe" else "apikey"

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("apikey") {
                ApiKeyScreen(
                    onApiKeySaved = {
                        navController.navigate("transcribe") {
                            popUpTo("apikey") { inclusive = true }
                        }
                    }
                )
            }

            composable("transcribe") {
                TranscribeScreen()
            }

            composable("settings") {
                SettingsScreen(
                    onClearApiKey = {
                        apiKeyManager.clearApiKey()
                        navController.navigate("apikey") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
