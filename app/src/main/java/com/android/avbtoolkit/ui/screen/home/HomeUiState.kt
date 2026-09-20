package com.android.avbtoolkit.ui.screen.home

import androidx.compose.runtime.Immutable
import com.android.avbtoolkit.ui.util.LatestVersionInfo

@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentAppVersionCode: Long,
    val systemInfo: SystemInfo,
)

@Immutable
data class HomeActions(
    val onPermissionsClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onOpenCommand: (String) -> Unit,
    val onOpenConsole: () -> Unit,
)
