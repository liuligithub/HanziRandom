package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.SettingsRepository
import com.liuli.hanzirandom.domain.SpeechService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(
    private val repository: CharacterRepository,
    settings: SettingsRepository,
    private val speech: SpeechService
) : ViewModel() {
    val state = repository.session.state
    val fontSize = settings.fontSize
    val swipeEnabled = settings.swipeEnabled

    private val _justAddedMistake = MutableStateFlow(false)
    val justAddedMistake: StateFlow<Boolean> = _justAddedMistake.asStateFlow()
    private var flashJob: Job? = null

    fun next() = repository.session.goNext()

    fun previous() = repository.session.goPrevious()

    fun reset() = repository.reset()

    fun speak(character: String) = speech.speak(character)

    fun addMistake(character: String) {
        repository.addMistake(character)
        flashJob?.cancel()
        _justAddedMistake.value = true
        flashJob = viewModelScope.launch {
            delay(1000)
            _justAddedMistake.value = false
        }
    }

    companion object {
        fun factory(app: HanziApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(StudyViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    @Suppress("UNCHECKED_CAST")
                    return StudyViewModel(
                        app.characterRepository,
                        app.settingsRepository,
                        app.speechService
                    ) as T
                }
            }
    }
}
