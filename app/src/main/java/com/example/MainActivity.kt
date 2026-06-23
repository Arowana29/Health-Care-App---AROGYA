package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigation.Screen
import com.example.ui.HealthViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.TrilingualText

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import com.example.ui.components.LocalAppLanguage

class MainActivity : ComponentActivity() {
    // triggering build to fix apk installation issue
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HealthViewModel = viewModel()
            val currentLanguage by viewModel.currentLanguage.collectAsState()

            MyApplicationTheme {
                CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val bottomBarscreens = listOf(
                        Screen.Home,
                        Screen.Documents,
                        Screen.Medications,
                        Screen.Share,
                        Screen.Profile
                    )

                    val startRoute = if (viewModel.appPin != null) Screen.Passcode.route else Screen.Launch.route

                    Scaffold(
                        bottomBar = {
                            if (currentRoute in bottomBarscreens.map { it.route }) {
                                NavigationBar {
                                    bottomBarscreens.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon!!, contentDescription = screen.titleEn) },
                                            selected = currentRoute == screen.route,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            label = {
                                                TrilingualText(
                                                    english = screen.titleEn,
                                                    sinhala = screen.titleSi,
                                                    tamil = screen.titleTa,
                                                    scale = 0.6f,
                                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startRoute,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Passcode.route) { PasscodeScreen(navController, viewModel) }
                            composable(Screen.Launch.route) { LaunchScreen(navController, viewModel) }
                            composable(Screen.DailyVitals.route) { DailyVitalsScreen(navController, viewModel) }
                            composable(Screen.Home.route) { DashboardScreen(navController, viewModel) }
                            composable(Screen.Documents.route) { DocumentsScreen(navController, viewModel) }
                            composable(Screen.Medications.route) { MedicationsScreen(navController, viewModel) }
                            composable(Screen.Visits.route) { VisitsScreen(navController, viewModel) }
                            composable(Screen.Expenses.route) { ConsultationExpensesScreen(navController, viewModel) }
                            composable(Screen.Symptoms.route) { SymptomsScreen(navController, viewModel) }
                            composable(Screen.Share.route) { ShareScreen(navController, viewModel) }
                            composable(Screen.Profile.route) { ProfileScreen(navController, viewModel) }
                            composable(Screen.PremiumFamily.route) { PremiumFamilyScreen(navController, viewModel) }
                            composable(Screen.Emergency.route) { EmergencyScreen(navController, viewModel) }
                            composable(Screen.PharmacyLocator.route) { PharmacyLocatorScreen(navController, viewModel) }
                            composable(Screen.Chat.route) { ChatScreen(navController, viewModel) }
                            composable(Screen.MedicalRecords.route) { com.example.ui.screens.MedicalRecordsScreen(navController, viewModel) }
                        }
                    }
                }
            }
        }
    }
}

