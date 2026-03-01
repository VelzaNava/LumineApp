package com.thesis.lumine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.ui.admin.AdminScreen
import com.thesis.lumine.ui.ar.ARScreen
import com.thesis.lumine.ui.auth.LoginScreen
import com.thesis.lumine.ui.catalog.CatalogScreen
import com.thesis.lumine.ui.theme.LumineAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumineAppTheme {
                LumineNavigation()
            }
        }
    }
}

@Composable
fun LumineNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "catalog"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("catalog") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("catalog") {
            CatalogScreen(
                onJewelrySelected = { jewelry ->
                    val jewelryJson = Gson().toJson(jewelry)
                    val encoded = java.net.URLEncoder.encode(jewelryJson, "UTF-8")
                    navController.navigate("ar/$encoded")
                }
            )
        }
        composable(
            route = "ar/{jewelry}",
            arguments = listOf(
                navArgument("jewelry") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jewelryJson = backStackEntry.arguments?.getString("jewelry") ?: ""
            val decoded = java.net.URLDecoder.decode(jewelryJson, "UTF-8")
            val jewelry = Gson().fromJson(decoded, Jewelry::class.java)

            ARScreen(
                jewelry = jewelry,
                onBack = { navController.popBackStack() }
            )
        }
        composable("admin") {
            AdminScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}