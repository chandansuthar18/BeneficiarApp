package com.example.beneficiaryapp.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.beneficiaryapp.ui.screens.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Home Screen
        composable("home") {
            HomeScreen(navController = navController)
        }

        // View Beneficiaries Screen
        composable("view_beneficiaries") {
            ViewBeneficiariesScreen(navController = navController)
        }

        // Add Beneficiary Screen (Basic Info)
        composable("add_beneficiary") {
            // Make sure this screen exists - if not, create it
            BeneficiaryRegistrationScreen(navController = navController)
        }

        // Pregnancy Form Screen
        composable("pregnancy") {
            PregnancyScreen(navController = navController)
        }

        // Lactating Form Screen
        composable("lactating") {
            LactatingScreen(navController = navController)
        }

        // Add any other screens you have
    }
}