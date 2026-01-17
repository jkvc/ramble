package com.ramble.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ramble.app.ui.LoginScreen
import com.ramble.app.ui.TranscribeScreen
import com.ramble.app.ui.theme.RambleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            RambleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RambleNavigation()
                }
            }
        }
    }
}

@Composable
fun RambleNavigation() {
    val navController = rememberNavController()
    val authManager = RambleApp.instance.authManager
    val isLoggedIn by authManager.currentUser.collectAsState()
    
    val startDestination = if (isLoggedIn != null) "transcribe" else "login"
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("transcribe") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("transcribe") {
            TranscribeScreen(
                onLogout = {
                    authManager.logout()
                    navController.navigate("login") {
                        popUpTo("transcribe") { inclusive = true }
                    }
                }
            )
        }
    }
}
