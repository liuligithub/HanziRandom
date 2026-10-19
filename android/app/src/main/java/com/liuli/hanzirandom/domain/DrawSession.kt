package com.liuli.hanzirandom.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoundSnapshot(
    val pool: List<String>,
    val history: List<String>,
    val cursor: Int,
    val total: Int,
    val isFinished: Boolean
)

data class SessionState(
    val pool: List<String> = emptyList(),
    val current: String? = null,
    val total: Int = 0,
    val drawnCount: Int = 0,
    val isFinished: Boolean = false,
    val history: List<String> = emptyList(),
    val cursor: Int = -1
) {
    val canGoPrevious: Boolean get() = cursor > 0
    val canGoNext: Boolean get() = cursor < history.size - 1 || !isFinished
    val position: Int get() = cursor + 1
}

/**
 * A single shuffled drawing round. Mirrors the iOS DrawSession state machine.
 */
class DrawSession(characters: List<String> = emptyList()) {
    private var characters: List<String> = characters
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        restart()
    }

    fun restart(characters: List<String>? = null) {
        if (characters != null) this.characters = characters
        val pool = this.characters.shuffled()
        _state.value = SessionState(pool = pool, total = this.characters.size)
        if (pool.isEmpty()) {
            _state.value = _state.value.copy(isFinished = true)
            return
        }
        drawNext()
    }

    fun drawNext() {
        val s = _state.value
        if (s.isFinished || s.pool.isEmpty()) {
            _state.value = s.copy(isFinished = true)
            return
        }
        val index = s.pool.indices.random()
        val drawn = s.pool[index]
        val pool = s.pool.toMutableList().apply { removeAt(index) }
        val history = s.history + drawn
        _state.value = s.copy(
            pool = pool,
            current = drawn,
            history = history,
            cursor = history.lastIndex,
            drawnCount = s.drawnCount + 1,
            isFinished = pool.isEmpty()
        )
    }

    fun appendToPool(newCharacters: List<String>) {
        val s = _state.value
        if (s.isFinished || newCharacters.isEmpty()) return
        _state.value = s.copy(
            pool = s.pool + newCharacters,
            total = s.total + newCharacters.size
        )
    }

    fun removeCharacter(character: String) {
        val s = _state.value
        val index = s.history.indexOf(character)
        if (index >= 0) {
            if (index == s.cursor) {
                removeCurrent()
                return
            }
            val history = s.history.toMutableList().apply { removeAt(index) }
            val cursor = if (index < s.cursor) s.cursor - 1 else s.cursor
            _state.value = s.copy(
                history = history,
                cursor = cursor,
                current = if (cursor in history.indices) history[cursor] else null,
                total = maxOf(0, s.total - 1),
                drawnCount = history.size,
                isFinished = s.pool.isEmpty()
            )
            return
        }
        if (character !in s.pool) return
        val pool = s.pool.filter { it != character }
        _state.value = s.copy(
            pool = pool,
            total = maxOf(0, s.total - 1),
            isFinished = pool.isEmpty()
        )
    }

    fun removeCurrent() {
        val s = _state.value
        val shown = s.current ?: return
        if (s.cursor < 0) return

        val pool = s.pool.filter { it != shown }
        val history = s.history.toMutableList().apply { removeAt(s.cursor) }
        val total = maxOf(0, s.total - 1)
        val drawnCount = maxOf(0, s.drawnCount - 1)

        when {
            s.cursor < history.size -> {
                _state.value = s.copy(
                    pool = pool,
                    history = history,
                    current = history[s.cursor],
                    total = total,
                    drawnCount = drawnCount
                )
            }
            pool.isNotEmpty() -> {
                _state.value = s.copy(
                    pool = pool,
                    history = history,
                    total = total,
                    drawnCount = drawnCount
                )
                drawNext()
            }
            else -> {
                _state.value = s.copy(
                    pool = pool,
                    history = history,
                    current = null,
                    cursor = history.lastIndex,
                    total = total,
                    drawnCount = drawnCount,
                    isFinished = true
                )
            }
        }
    }

    fun restore(snapshot: RoundSnapshot) {
        _state.value = SessionState(
            pool = snapshot.pool,
            history = snapshot.history,
            current = if (snapshot.cursor in snapshot.history.indices) {
                snapshot.history[snapshot.cursor]
            } else {
                null
            },
            total = snapshot.total,
            drawnCount = snapshot.history.size,
            cursor = snapshot.cursor,
            isFinished = snapshot.isFinished
        )
    }

    fun goPrevious() {
        val s = _state.value
        if (s.cursor <= 0) return
        val cursor = s.cursor - 1
        _state.value = s.copy(cursor = cursor, current = s.history[cursor])
    }

    fun goNext() {
        val s = _state.value
        if (s.cursor < s.history.lastIndex) {
            val cursor = s.cursor + 1
            _state.value = s.copy(cursor = cursor, current = s.history[cursor])
        } else {
            drawNext()
        }
    }
}
