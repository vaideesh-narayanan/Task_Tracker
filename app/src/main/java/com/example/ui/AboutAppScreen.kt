package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.EventAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About App") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.EventAvailable, 
                contentDescription = "App Logo", 
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Task Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "A simple and offline-first task tracking application to help you keep track of your daily goals, pending work, and completed tasks effectively.\n\n" +
                     "Features:\n" +
                     "• Fully Offline: No internet required.\n" +
                     "• Privacy Focused: All data is stored only on your phone. Developers, Google, or any third-party don't have access to your data.\n" +
                     "• No Unnecessary Permissions: This app doesn't ask you for any permissions except to send notifications about your tasks.\n" +
                     "• Organization: Filter tasks by All, Pending, Completed, Expired, and Archived.\n" +
                     "• Themes & Customization: Supports light and dark modes, with a choice of modern accent colors.\n" +
                     "• Backup & Restore: Easily backup your tasks and restore them on demand.\n" +
                     "• Recycle Bin: Recover accidentally deleted tasks easily.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Helpful? Share it with your friends and family!\n\nFound a bug, or want to suggest new features? Please let me know by submitting your feedback privately.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}
