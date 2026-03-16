package com.example.savagestats.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.savagestats.ui.theme.DarkSurface
import com.example.savagestats.ui.theme.SavageRed
import com.example.savagestats.ui.theme.TextMuted
import com.example.savagestats.ui.theme.TextPrimary

// --- Route constants ---

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val COACH = "coach"
    const val PROFILE = "profile"
}

// --- Bottom nav tab definition ---

data class BottomNavTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val route: String,
)

val bottomNavTabs = listOf(
    BottomNavTab(
        label = "LOG",
        icon = Icons.Default.DateRange,
        contentDescription = "Dashboard",
        route = NavRoutes.DASHBOARD,
    ),
    BottomNavTab(
        label = "COACH",
        icon = Icons.Default.Star,
        contentDescription = "Coach",
        route = NavRoutes.COACH,
    ),
    BottomNavTab(
        label = "PROFILE",
        icon = Icons.Default.Person,
        contentDescription = "Profile",
        route = NavRoutes.PROFILE,
    ),
)

// --- Bottom navigation bar ---

@Composable
fun BottomNavigationBar(
    tabs: List<BottomNavTab>,
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextPrimary,
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.contentDescription,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SavageRed,
                    selectedTextColor = SavageRed,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = DarkSurface,
                ),
            )
        }
    }
}

// --- Main screen ---

@Composable
fun MainScreen(
    dashboardViewModel: DashboardViewModel,
    coachViewModel: CoachViewModel,
    profileViewModel: ProfileViewModel,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                tabs = bottomNavTabs,
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(NavRoutes.DASHBOARD) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(NavRoutes.COACH) {
                CoachScreen(viewModel = coachViewModel)
            }
            composable(NavRoutes.PROFILE) {
                ProfileScreen(viewModel = profileViewModel)
            }
        }
    }
}
