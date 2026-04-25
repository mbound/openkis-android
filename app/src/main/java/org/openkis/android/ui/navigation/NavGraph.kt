package org.openkis.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.openkis.android.ui.caves.CaveDetailScreen
import org.openkis.android.ui.caves.CaveListScreen
import org.openkis.android.ui.export.ExportScreen
import org.openkis.android.ui.map.MapScreen
import org.openkis.android.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Map : Screen("map", "Map", Icons.Default.Map)
    data object Caves : Screen("caves", "Browse", Icons.Default.Explore)
    data object Export : Screen("export", "Export", Icons.Default.FileDownload)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object CaveDetail : Screen("cave/{type}/{code}", "Detail", Icons.Default.Explore)
}

val bottomNavItems = listOf(Screen.Map, Screen.Caves, Screen.Export, Screen.Settings)

@Composable
fun OpenKisNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    onCaveClick = { type, code ->
                        navController.navigate("cave/$type/$code")
                    }
                )
            }
            composable(Screen.Caves.route) {
                CaveListScreen(
                    onItemClick = { type, code ->
                        navController.navigate("cave/$type/$code")
                    }
                )
            }
            composable(Screen.Export.route) {
                ExportScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.CaveDetail.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("code") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "caves"
                val code = backStackEntry.arguments?.getString("code") ?: ""
                CaveDetailScreen(
                    type = type,
                    code = code,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail screen
    if (currentDestination?.route?.startsWith("cave/") == true) return

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
