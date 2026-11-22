package com.niagara.launcher.core.model

/**
 * User configuration and customization settings.
 */
data class LauncherPreferences(
    val favoriteKeys: List<String> = emptyList(),
    val hiddenKeys: Set<String> = emptySet(),
    val isRightHanded: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val showBatteryInGlance: Boolean = true
)
