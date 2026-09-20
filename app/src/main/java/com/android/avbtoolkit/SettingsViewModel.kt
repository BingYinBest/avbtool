package com.android.avbtoolkit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val store: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(load())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        store.showFunctionKeyboard = _uiState.value.showFunctionKeyboard
    }

    fun setFunctionKeyboardVisible(visible: Boolean) {
        _uiState.update { it.copy(showFunctionKeyboard = visible) }
        store.showFunctionKeyboard = visible
    }

    private fun load() = SettingsUiState(
        showFunctionKeyboard = store.showFunctionKeyboard,
    )

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    SettingsViewModel(SettingsStore(appContext))
                }
            }
        }
    }
}
