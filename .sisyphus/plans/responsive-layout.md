# Full Tablet + Phone Responsive Layout Implementation Plan

**Goal:** Make every screen in SavageStats responsive — phones use bottom NavigationBar, tablets use NavigationRail on the left. All screens adapt their layout based on WindowWidthSizeClass (Compact / Medium / Expanded).

**Architecture:** Use `CompositionLocal` to provide `WindowSizeClass` from `MainActivity` through the entire composable tree. Each screen reads `LocalWindowSizeClass.current` and adapts. `MainScreen` switches between `NavigationBar` (Compact) and `NavigationRail` (Medium/Expanded).

**Tech Stack:** Jetpack Compose Material3, `material3-window-size-class:1.3.0` (via BOM 2024.09.00)

---

## Existing Codebase Context

### Theme Colors (from `com.savagestats.app.ui.theme`):
- `SavageRed`, `SavageRedDark`, `SavageRedMuted` — brand colors
- `DarkBackground` (#0D0D0D), `DarkSurface` (#1A1A1A), `DarkSurfaceVariant` (#252525)
- `TextPrimary` (#F5F5F5), `TextSecondary` (#9E9E9E), `TextMuted` (#616161)
- `SuccessGreen`, `WarningAmber` — accents

### Styling Patterns:
- Cards: `ElevatedCard` + `RoundedCornerShape(16.dp)` + `containerColor = DarkSurface` + `elevation = 6.dp`
- Buttons: `RoundedCornerShape(50)` pill shape, `SavageRed` primary, `FontWeight.Black`, `letterSpacing = 2.sp`
- TextFields: `OutlinedTextFieldDefaults.colors(focusedBorderColor = SavageRed, unfocusedBorderColor = DarkSurfaceVariant, ...)`
- Navigation: `NavigationBar(containerColor = DarkSurface)`, selected = `SavageRed`, unselected = `TextMuted`

### Navigation Architecture:
- `MainScreen.kt` has `Scaffold` with `bottomBar` + `NavHost`
- Routes: DASHBOARD, COACH, MISSIONS, PROFILE, CAMERA
- `bottomNavTabs` list defines 4 tabs (LOG, COACH, MISSIONS, PROFILE)
- Camera is navigated to from Dashboard, not a tab

### Screen Files (line counts):
- `DashboardScreen.kt` — 1892 lines (biggest, most complex)
- `CoachScreen.kt` — 868 lines
- `ProfileScreen.kt` — 458 lines
- `CameraScreen.kt` — 393 lines
- `SetupScreen.kt` — 360 lines
- `OnboardingScreen.kt` — 358 lines
- `MissionsScreen.kt` — 355 lines
- `MainScreen.kt` — 185 lines

### All screens use:
- `Scaffold` as root
- `verticalScroll` or `LazyColumn` for content
- Hard-coded `dp` values for padding (typically 16-24dp), spacing, heights
- `fillMaxWidth()` already used widely (good — many elements are already relative)

---

## Task 1: Add WindowSizeClass Dependency

**File:** `app/build.gradle.kts`

Add inside `dependencies {}`:
```kotlin
implementation("androidx.compose.material3:material3-window-size-class")
```

No version needed — BOM manages it. Place it right after the existing `material3` line.

---

## Task 2: Create `LocalWindowSizeClass` CompositionLocal

**Create file:** `app/src/main/java/com/savagestats/app/ui/theme/WindowSize.kt`

```kotlin
package com.savagestats.app.ui.theme

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided — wrap your root composable with CompositionLocalProvider")
}

/**
 * Responsive dimension helper. Returns a value based on window width class.
 * Usage: `val padding = responsiveDp(compact = 16.dp, medium = 24.dp, expanded = 32.dp)`
 */
fun responsiveDp(
    widthSizeClass: WindowWidthSizeClass,
    compact: Dp,
    medium: Dp = compact,
    expanded: Dp = medium
): Dp = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> compact
    WindowWidthSizeClass.Medium -> medium
    WindowWidthSizeClass.Expanded -> expanded
    else -> compact
}

/**
 * Returns max content width for the current window size.
 * On expanded displays, content should not stretch to full width.
 */
fun maxContentWidth(widthSizeClass: WindowWidthSizeClass): Dp = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> Dp.Infinity    // Full width
    WindowWidthSizeClass.Medium -> 600.dp           // Constrained
    WindowWidthSizeClass.Expanded -> 840.dp         // Constrained
    else -> Dp.Infinity
}
```

---

## Task 3: Provide WindowSizeClass from MainActivity

**File:** `app/src/main/java/com/savagestats/app/MainActivity.kt`

1. Add import:
```kotlin
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import com.savagestats.app.ui.theme.LocalWindowSizeClass
```

2. Add `@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)` on `onCreate`

3. Inside `setContent { SavageStatsTheme { ... } }`, wrap the content with:
```kotlin
val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
    // ... existing if/else for onboarding vs main content
}
```

The `CompositionLocalProvider` should be INSIDE `SavageStatsTheme` but wrapping everything else.

---

## Task 4: Make MainScreen Adaptive (NavigationRail vs NavigationBar)

**File:** `app/src/main/java/com/savagestats/app/ui/MainScreen.kt`

This is the core change. On Compact → bottom `NavigationBar`. On Medium/Expanded → left `NavigationRail`.

### New imports needed:
```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.savagestats.app.ui.theme.LocalWindowSizeClass
```

### Modified `MainScreen` composable:

```kotlin
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
                    onOpenCamera = { navController.navigate(NavRoutes.CAMERA) }
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
        Row(modifier = Modifier.fillMaxSize()) {
            SavageNavigationRail(
                tabs = bottomNavTabs,
                currentRoute = currentRoute,
                onTabSelected = onTabSelected,
            )
            navContent(Modifier.weight(1f))
        }
    } else {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    tabs = bottomNavTabs,
                    currentRoute = currentRoute,
                    onTabSelected = onTabSelected,
                )
            }
        ) { innerPadding ->
            navContent(Modifier.padding(innerPadding))
        }
    }
}
```

### New `SavageNavigationRail` composable (same file):

```kotlin
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
    }
}
```

Additional import: `import androidx.compose.material3.NavigationRailItemDefaults`

---

## Task 5: Make Each Screen Responsive

### General Pattern for Each Screen:

Each screen should:
1. Read `val windowSizeClass = LocalWindowSizeClass.current`
2. Derive `val widthClass = windowSizeClass.widthSizeClass`
3. Use adaptive padding: `responsiveDp(widthClass, compact = 16.dp, medium = 24.dp, expanded = 32.dp)`
4. On Medium/Expanded: arrange logically paired sections side-by-side in a `Row`
5. Constrain max content width on Expanded displays
6. Keep all existing theme colors and styling patterns

### Key Imports Each Screen Needs:
```kotlin
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.responsiveDp
import com.savagestats.app.ui.theme.maxContentWidth
```

### Per-Screen Responsive Details:

#### 5a. DashboardScreen.kt (1892 lines)
- Largest screen. Has: weekly calendar, food search, macro inputs, exercise section, log history
- **Compact**: Keep current single-column layout
- **Medium/Expanded**: 
  - Wrap the outer Column with `Modifier.widthIn(max = maxContentWidth(widthClass)).align(Alignment.CenterHorizontally)` inside a `Box(Modifier.fillMaxSize())`
  - The "macro input" row (protein/carbs/fat/fiber/sodium fields) can spread wider
  - The MetricTile row (which shows today's macros at a glance) should use wider tiles
  - Search results dropdown can be wider
- Replace hard-coded `padding(horizontal = 16.dp)` → `padding(horizontal = responsiveDp(widthClass, 16.dp, 24.dp, 32.dp))`
- The `MetricTile` composable uses `height = 72.dp` — on larger screens use `height = responsiveDp(widthClass, 72.dp, 84.dp, 96.dp)`

#### 5b. CoachScreen.kt (868 lines)
- Has: model status card, chat messages, prompt input
- **Compact**: Current layout
- **Medium/Expanded**:
  - Constrain content width with `maxContentWidth`
  - Chat messages can be wider but still centered
  - Prompt input area at bottom can be wider

#### 5c. ProfileScreen.kt (458 lines)
- Has: user biometrics form (age, weight, height), goal selection chips, macros summary
- **Compact**: Current single-column
- **Medium/Expanded**:
  - Biometric inputs (age/weight/height) could go in a Row of 3
  - Goal chips could spread wider
  - Constrain content width

#### 5d. MissionsScreen.kt (355 lines)
- Has: mission cards with checkboxes
- **Compact**: Single column
- **Medium/Expanded**:
  - Two-column grid of mission cards using `LazyVerticalGrid` or two side-by-side `Column`s
  - Constrain content width

#### 5e. OnboardingScreen.kt (358 lines)
- Has: welcome text, biometric inputs, goal selection, confirm button
- Same treatment as ProfileScreen — wider layout, constrained max width

#### 5f. SetupScreen.kt (360 lines)
- Has: model download progress, permission requests
- Minimal layout — just constrain max width and center

#### 5g. CameraScreen.kt (393 lines)
- Has: camera preview, capture/gallery buttons, detected tags
- Camera preview should fill available space
- Buttons can spread wider on tablets
- Constrain max width for the overlay controls

---

## Task 6: Verify and Build

After all changes:
1. Run `./gradlew assembleDebug` — must pass
2. Check no import errors
3. Check no `as any` or type suppressions
4. Verify all theme colors match existing patterns

---

## Critical Constraints

- **DO NOT** change any colors, fonts, or branding — keep the SavageStats dark theme exactly as-is
- **DO NOT** add new dependencies beyond `material3-window-size-class`
- **DO NOT** modify any ViewModel logic or data layer
- **DO NOT** use `@Suppress` or `@ts-ignore` equivalents
- **DO** use `@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)` where required
- **DO** keep all existing composable function signatures backward-compatible (no breaking changes to public APIs)
- **DO** match the existing code style: trailing commas, 4-space indent, KDoc comments where non-obvious
