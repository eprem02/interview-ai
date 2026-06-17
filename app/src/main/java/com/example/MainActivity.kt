package com.example

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InterviewScreen
import com.example.ui.screens.ResumeScreen
import com.example.ui.screens.SessionDetailsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.InterviewViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: InterviewViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                MainAppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: InterviewViewModel) {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToResume = {
                        navController.navigate("resume")
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile")
                    },
                    onNavigateToInterviewWithParams = { jobTitle, company, mode, lang ->
                        viewModel.startNewInterviewSessionWithParam(jobTitle, company, mode, lang) { sessionId ->
                            navController.navigate("interview/$jobTitle")
                        }
                    },
                    onNavigateToSessionDetails = { sessionId ->
                        navController.navigate("session_detail/$sessionId")
                    },
                    onNavigateToKnowledgeBase = {
                        navController.navigate("knowledge_base")
                    },
                    onNavigateToHistory = {
                        navController.navigate("interview_history")
                    }
                )
            }

            composable("interview_history") {
                com.example.ui.screens.InterviewHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSessionDetails = { sessionId ->
                        navController.navigate("session_detail/$sessionId")
                    }
                )
            }

            composable("knowledge_base") {
                com.example.ui.screens.KnowledgeBaseScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("profile") {
                UserProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("resume") {
                ResumeScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "interview/{jobTitle}",
                arguments = listOf(navArgument("jobTitle") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobTitle = backStackEntry.arguments?.getString("jobTitle").orEmpty()
                InterviewScreen(
                    viewModel = viewModel,
                    jobTitle = jobTitle,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToResults = { sessionId ->
                        // Replaces stack so back navigation returns to Dashboard
                        navController.navigate("session_detail/$sessionId") {
                            popUpTo("dashboard") { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = "session_detail/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                SessionDetailsScreen(
                    viewModel = viewModel,
                    sessionId = sessionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
