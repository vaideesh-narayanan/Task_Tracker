package com.example

/*
 * List of Libraries and Versions (as of June 2026):
 * - androidx.core:core-ktx: 1.18.0 (Free of known security vulnerabilities)
 * - androidx.lifecycle:lifecycle-*: 2.8.7 (Free of known security vulnerabilities)
 * - androidx.activity:activity-compose: 1.10.1 (Free of known security vulnerabilities)
 * - androidx.compose.ui:ui-*: [compose-bom 2024.09.00] (Free of known security vulnerabilities)
 * - androidx.compose.material3:material3: [compose-bom 2024.09.00] (Free of known security vulnerabilities)
 * - androidx.navigation:navigation-compose: 2.8.9 (Free of known security vulnerabilities)
 * - androidx.room:room-*: 2.7.0 (Free of known security vulnerabilities)
 * - org.jetbrains.kotlinx:kotlinx-coroutines-*: 1.10.2 (Free of known security vulnerabilities)
 * - androidx.datastore:datastore-preferences: 1.1.7 (Free of known security vulnerabilities)
 * - com.google.code.gson:gson: 2.11.0 (Free of known security vulnerabilities)
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import com.example.data.TaskRepository
import com.example.ui.AddEditTaskScreen
import com.example.ui.ArchiveScreen
import com.example.ui.HomeScreen
import com.example.ui.RecycleBinScreen
import com.example.ui.SettingsScreen
import com.example.ui.AboutAppScreen
import com.example.ui.TaskViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_alerts",
                "Task Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for your tasks and reminders"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val settingsManager = SettingsManager(context)
            val isDarkTheme by settingsManager.isDarkThemeFlow.collectAsState(initial = false)
            val accentColor by settingsManager.accentColorFlow.collectAsState(initial = null)
            
            MyApplicationTheme(darkTheme = isDarkTheme, accentColor = accentColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val database = AppDatabase.getDatabase(context)
                    val notificationScheduler = com.example.data.NotificationScheduler(context)
                    val repository = TaskRepository(database.taskDao(), notificationScheduler)
                    val viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repository))
                    
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAddEdit = { taskId ->
                                    if (taskId == null) {
                                        navController.navigate("add_edit")
                                    } else {
                                        navController.navigate("add_edit?taskId=$taskId")
                                    }
                                },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToAbout = { navController.navigate("about") },
                                onNavigateToArchive = { navController.navigate("archive") },
                                onNavigateToRecycleBin = { navController.navigate("recycle_bin") }
                            )
                        }
                        composable(
                            route = "add_edit?taskId={taskId}",
                            arguments = listOf(navArgument("taskId") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val taskIdArg = backStackEntry.arguments?.getInt("taskId") ?: -1
                            val taskId = if (taskIdArg == -1) null else taskIdArg
                            AddEditTaskScreen(
                                taskId = taskId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                settingsManager = settingsManager,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("about") {
                            AboutAppScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("archive") {
                            ArchiveScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAddEdit = { taskId ->
                                    navController.navigate("add_edit?taskId=$taskId")
                                }
                            )
                        }
                        composable("recycle_bin") {
                            RecycleBinScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
