package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.MistakeEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MistakeBookViewModel(private val repository: CharacterRepository) : ViewModel() {
    val mistakes: StateFlow<List<MistakeEntry>> =
        combine(repository.mistakes, repository.library) { _, _ -> repository.mistakesSorted() }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                repository.mistakesSorted()
            )

    fun remove(character: String) = repository.removeMistake(character)

    fun clear() = repository.clearMistakes()

    companion object {
        fun factory(app: HanziApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(MistakeBookViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    @Suppress("UNCHECKED_CAST")
                    return MistakeBookViewModel(app.characterRepository) as T
                }
            }
    }
}
