package com.android.avbtoolkit.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.android.avbtoolkit.ui.UiMode
import com.android.avbtoolkit.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val uiMode: UiMode,
)
