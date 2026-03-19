package com.savagestats.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary

// --- Route constants ---

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val COACH = "coach"
    const val MISSIONS = "missions"
    const val PROFILE = "profile"
    const val CAMERA = "camera"
}

// --- Nav tab definition ---

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
        label = "MISSIONS",
        icon = Icons.Default.CheckCircle,
        contentDescription = "Missions",
        route = NavRoutes.MISSIONS,
    ),
    BottomNavTab(
        label = "PROFILE",
        icon = Icons.Default.Person,
        contentDescription = "Profile",
        route = NavRoutes.PROFILE,
    ),
)

// --- Bottom navigation bar (phone / compact) ---

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

// --- Navigation rail (tablet / medium+expanded) ---

@Composable
fun SavageNavigationRail(
    tabs: List<BottomNavTab>,
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    NavigationRail(
        containerColor = DarkSurface,
        contentColor = TextPrimary,
    ) {
        Spacer(Modifier.weight(1f))
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationRailItem(
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
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = SavageRed,
                    selectedTextColor = SavageRed,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = DarkSurface,
                ),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

// --- Main screen ---

@Composable
fun MainScreen(
    dashboardViewModel: DashboardViewModel,
    coachViewModel: CoachViewModel,
    missionsViewModel: MissionsViewModel,
    profileViewModel: ProfileViewModel,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val windowSizeClass = LocalWindowSizeClass.current
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val onTabSelected: (BottomNavTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navContent: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.DASHBOARD,
            modifier = modifier,
        ) {
            composable(NavRoutes.DASHBOARD) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onOpenCamera = { navController.navigate(NavRoutes.CAMERA) },
                )
            }
            composable(NavRoutes.COACH) {
                CoachScreen(viewModel = coachViewModel)
            }
            composable(NavRoutes.MISSIONS) {
                MissionsScreen(viewModel = missionsViewModel)
            }
            composable(NavRoutes.PROFILE) {
                ProfileScreen(viewModel = profileViewModel)
            }
            composable(NavRoutes.CAMERA) {
                CameraScreen(
                    onMealScanned = { tagCluster ->
                        // Route camera tags through RAG food search — dropdown appears with DB matches
                        dashboardViewModel.onCameraTagDetected(tagCluster)
                    },
                    onBackToDashboard = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }

    if (useNavRail) {
        // Tablet / expanded: side rail + content
        Row(modifier = Modifier.fillMaxSize()) {
            SavageNavigationRail(
                tabs = bottomNavTabs,
                currentRoute = currentRoute,
                onTabSelected = onTabSelected,
            )
            Scaffold(
                modifier = Modifier.weight(1f),
            ) { innerPadding ->
                navContent(Modifier.padding(innerPadding))
            }
        }
    } else {
        // Phone / compact: bottom bar
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    tabs = bottomNavTabs,
                    currentRoute = currentRoute,
                    onTabSelected = onTabSelected,
                )
            },
        ) { innerPadding ->
            navContent(Modifier.padding(innerPadding))
        }
    }
}
