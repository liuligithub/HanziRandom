package com.liuli.hanzirandom.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val store: KeyValueStore) {
    private val _fontSize = MutableStateFlow(store.getFloat(KEY_FONT_SIZE, 150f))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _swipeEnabled = MutableStateFlow(store.getBoolean(KEY_SWIPE_ENABLED, true))
    val swipeEnabled: StateFlow<Boolean> = _swipeEnabled.asStateFlow()

    fun setFontSize(value: Float) {
        val clamped = value.coerceIn(60f, 260f)
        _fontSize.value = clamped
        store.putFloat(KEY_FONT_SIZE, clamped)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        _swipeEnabled.value = enabled
        store.putBoolean(KEY_SWIPE_ENABLED, enabled)
    }

    companion object {
        const val KEY_FONT_SIZE = "fontSize"
        const val KEY_SWIPE_ENABLED = "swipeEnabled"
    }
}
