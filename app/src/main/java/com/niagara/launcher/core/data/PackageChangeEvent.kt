package com.niagara.launcher.core.data

/**
 * Typed package lifecycle events used to drive incremental app list updates.
 */
sealed interface PackageChangeEvent {
    data object FullReload : PackageChangeEvent
    data class PackageAdded(val packageName: String) : PackageChangeEvent
    data class PackageRemoved(val packageName: String) : PackageChangeEvent
    data class PackageChanged(val packageName: String) : PackageChangeEvent
}
