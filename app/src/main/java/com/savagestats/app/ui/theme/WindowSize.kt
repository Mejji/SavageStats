package com.savagestats.app.ui.theme

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal providing [WindowSizeClass] throughout the composable tree.
 * Provided from [com.savagestats.app.MainActivity] via [CompositionLocalProvider].
 */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided — wrap your root composable with CompositionLocalProvider")
}

/**
 * Returns a [Dp] value adapted to the current window width class.
 *
 * Usage:
 * ```
 * val padding = responsiveDp(widthClass, compact = 16.dp, medium = 24.dp, expanded = 32.dp)
 * ```
 */
fun responsiveDp(
    widthSizeClass: WindowWidthSizeClass,
    compact: Dp,
    medium: Dp = compact,
    expanded: Dp = medium,
): Dp = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> compact
    WindowWidthSizeClass.Medium -> medium
    WindowWidthSizeClass.Expanded -> expanded
    else -> compact
}

/**
 * Maximum content width for the current window size.
 * On larger displays content is constrained so it doesn't stretch edge-to-edge.
 */
fun maxContentWidth(widthSizeClass: WindowWidthSizeClass): Dp = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> Dp.Infinity
    WindowWidthSizeClass.Medium -> 600.dp
    WindowWidthSizeClass.Expanded -> 840.dp
    else -> Dp.Infinity
}
