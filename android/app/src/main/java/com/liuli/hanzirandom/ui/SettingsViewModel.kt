package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.SettingsRepository

class SettingsViewModel(
    private val repository: CharacterRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    val library = repository.library
    val fontSize = settings.fontSize
    val swipeEnabled = settings.swipeEnabled

    fun setFontSize(value: Float) = settings.setFontSize(value)
    fun setSwipeEnabled(enabled: Boolean) = settings.setSwipeEnabled(enabled)
    fun resetRound() = repository.reset()
    fun addCharacters(text: String): Int = repository.addCharacters(text)
    fun remove(character: String) = repository.remove(character)
    fun exportString(): String = repository.exportString()

    companion object {
        fun factory(app: HanziApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    "Unknown ViewModel class: ${modelClass.name}"
                }
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(app.characterRepository, app.settingsRepository) as T
            }
        }
    }
}
