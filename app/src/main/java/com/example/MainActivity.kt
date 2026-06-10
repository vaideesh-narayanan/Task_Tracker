package com.example

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
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.ui.AddEditTaskScreen
import com.example.ui.HomeScreen
import com.example.ui.TaskViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = AppDatabase.getDatabase(context)
                    val repository = TaskRepository(database.taskDao())
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
                                }
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
                    }
                }
            }
        }
    }
}
