package com.ufomap.ufosightingmap.ui.theme

/**
 * Re-export the actual theme implementation.
 * This maintains compatibility with code that uses UFOSightingsTheme
 * while pointing to the actual implementation in UfoSightingMapTheme.
 */
@androidx.compose.runtime.Composable
fun UFOSightingsTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    UfoSightingMapTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}