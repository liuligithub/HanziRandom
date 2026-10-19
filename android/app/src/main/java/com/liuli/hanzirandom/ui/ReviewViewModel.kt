package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.domain.DrawSession
import com.liuli.hanzirandom.domain.SpeechService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: CharacterRepository,
    private val speech: SpeechService,
    characters: List<String>
) : ViewModel() {
    private val session = DrawSession(characters)
    val state = session.state

    private val _justAddedMistake = MutableStateFlow(false)
    val justAddedMistake: StateFlow<Boolean> = _justAddedMistake.asStateFlow()
    private var flashJob: Job? = null

    fun next() = session.goNext()

    fun previous() = session.goPrevious()

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

    fun removeMistake(character: String) {
        repository.removeMistake(character)
        session.removeCurrent()
    }

    companion object {
        fun factory(app: HanziApplication, characters: List<String>): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    @Suppress("UNCHECKED_CAST")
                    return ReviewViewModel(
                        app.characterRepository,
                        app.speechService,
                        characters
                    ) as T
                }
            }
    }
}
