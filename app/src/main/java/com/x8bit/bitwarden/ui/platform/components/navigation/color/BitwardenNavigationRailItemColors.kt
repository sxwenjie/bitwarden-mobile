package com.x8bit.bitwarden.ui.platform.components.navigation.color

import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for navigation rail items.
 */
@Composable
fun bitwardenNavigationRailItemColors(): NavigationRailItemColors = NavigationRailItemColors(
    selectedIconColor = BitwardenTheme.colorScheme.icon.secondary,
    unselectedIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledIconColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    selectedTextColor = BitwardenTheme.colorScheme.icon.secondary,
    unselectedTextColor = BitwardenTheme.colorScheme.icon.primary,
    disabledTextColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    selectedIndicatorColor = Color.Transparent,
)
