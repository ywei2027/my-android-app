package com.example.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.app.feature.register.AgreementScreen
import com.example.app.feature.register.RegisterScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "register"
    ) {
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { phone ->
                    navController.navigate("login?phone=$phone") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToAgreement = {
                    navController.navigate("agreement")
                },
                onRegistrationSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "login?phone={phone}",
            arguments = listOf(
                navArgument("phone") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            // TODO: LoginScreen — 后续阶段实现
        }

        composable("agreement") {
            AgreementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("home") {
            // TODO: HomeScreen — 后续阶段实现
        }
    }
}
