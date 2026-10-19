package com.liuli.hanzirandom.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRepositoryTest {

    private fun repositoryWith(library: List<String>): CharacterRepository {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", library.joinToString("\n"))
        return CharacterRepository(store)
    }

    @Test
    fun firstRun_initializesWithPreset() {
        val repo = CharacterRepository(InMemoryKeyValueStore())
        assertEquals(CharacterRepository.PRESET, repo.library.value)
        assertEquals("一", repo.library.value.first())
        assertTrue(repo.library.value.contains("舞"))
    }

    @Test
    fun preset_hasNoDuplicates() {
        assertEquals(
            CharacterRepository.PRESET.size,
            CharacterRepository.PRESET.toSet().size
        )
    }

    @Test
    fun preset_allHanzi() {
        for (char in CharacterRepository.PRESET) {
            assertTrue(
                "non-hanzi in preset: $char",
                CharacterRepository.isHanzi(char.codePointAt(0))
            )
        }
    }

    @Test
    fun startRound_setsTotalAndDrawsFirst() {
        val repo = repositoryWith(listOf("甲", "乙", "丙"))
        repo.startRound()
        val state = repo.session.state.value
        assertEquals(3, state.total)
        assertEquals(1, state.drawnCount)
        assertNotNull(state.current)
        assertFalse(state.isFinished)
    }

    @Test
    fun drawNext_noRepeatsWithinRound() {
        val repo = repositoryWith(listOf("甲", "乙", "丙", "丁"))
        val seen = mutableListOf(repo.session.state.value.current!!)
        while (!repo.session.state.value.isFinished) {
            repo.session.drawNext()
            repo.session.state.value.current?.let(seen::add)
        }
        assertEquals(4, seen.size)
        assertEquals(4, seen.toSet().size)
    }

    @Test
    fun emptyLibrary_isFinishedWithNoCurrent() {
        val repo = repositoryWith(emptyList())
        val state = repo.session.state.value
        assertNull(state.current)
        assertTrue(state.isFinished)
        assertEquals(0, state.total)
        assertEquals(0, state.drawnCount)
    }

    @Test
    fun addCharacters_appendsNewUniqueInOrder() {
        val repo = repositoryWith(listOf("欢", "给"))
        repo.addCharacters("山水田")
        assertEquals(listOf("欢", "给", "山", "水", "田"), repo.library.value)
    }

    @Test
    fun addCharacters_skipsExisting() {
        val repo = repositoryWith(listOf("欢", "给"))
        repo.addCharacters("给山欢水")
        assertEquals(listOf("欢", "给", "山", "水"), repo.library.value)
    }

    @Test
    fun addCharacters_dedupsWithinInput() {
        val repo = repositoryWith(emptyList())
        repo.addCharacters("山山水山田水")
        assertEquals(listOf("山", "水", "田"), repo.library.value)
    }

    @Test
    fun addCharacters_filtersWhitespaceAndNonHanzi() {
        val repo = repositoryWith(emptyList())
        repo.addCharacters(" 山a水\n1，田! 水 ")
        assertEquals(listOf("山", "水", "田"), repo.library.value)
    }

    @Test
    fun addCharacters_acceptsExtensionB() {
        val repo = repositoryWith(emptyList())
        repo.addCharacters("𠀀")
        assertEquals(listOf("𠀀"), repo.library.value)
    }

    @Test
    fun addCharacters_allDuplicatesDoesNotMutate() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.addCharacters("山水山水")
        assertEquals(listOf("山", "水"), repo.library.value)
    }

    @Test
    fun addCharacters_persists() {
        val store = InMemoryKeyValueStore()
        val first = repositoryWith(listOf("欢", "给"))
        // 复用同一份存储验证持久化
        store.putString("allCharacters", first.library.value.joinToString("\n"))
        val second = CharacterRepository(store)
        second.addCharacters("山水")
        val third = CharacterRepository(store)
        assertTrue(third.library.value.contains("山"))
        assertTrue(third.library.value.contains("水"))
    }

    @Test
    fun addCharacters_joinsCurrentRoundImmediately() {
        val repo = repositoryWith(listOf("山", "水"))
        assertEquals(2, repo.session.state.value.total)

        repo.addCharacters("田")
        assertEquals(3, repo.session.state.value.total)
        assertTrue(repo.session.state.value.pool.contains("田"))

        while (!repo.session.state.value.isFinished) {
            repo.session.goNext()
        }
        assertTrue(repo.session.state.value.history.contains("田"))
    }

    @Test
    fun addCharacters_afterFinished_doesNotCorruptFinishedRound() {
        val repo = repositoryWith(listOf("山"))
        assertTrue(repo.session.state.value.isFinished)

        repo.addCharacters("水")
        assertEquals(listOf("山", "水"), repo.library.value)
        assertTrue(repo.session.state.value.isFinished)

        repo.reset()
        assertEquals(2, repo.session.state.value.total)
        assertFalse(repo.session.state.value.isFinished)
    }

    @Test
    fun remove_deletesAndPersists() {
        val store = InMemoryKeyValueStore()
        val repo = CharacterRepository(store)
        repo.addCharacters("山")
        repo.remove("山")
        assertFalse(repo.library.value.contains("山"))

        val reloaded = CharacterRepository(store)
        assertFalse(reloaded.library.value.contains("山"))
    }

    @Test
    fun remove_nonexistent_isNoOp() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.remove("田")
        assertEquals(listOf("山", "水"), repo.library.value)
    }

    @Test
    fun remove_alsoDropsShownCharacterFromRoundAndMistakes() {
        val repo = repositoryWith(listOf("甲", "乙", "丙"))
        val shown = repo.session.state.value.current!!
        repo.addMistake(shown)

        repo.remove(shown)

        assertNull(repo.mistakes.value[shown])
        assertEquals(2, repo.session.state.value.total)
        assertNotEquals(shown, repo.session.state.value.current)
        assertFalse(repo.session.state.value.history.contains(shown))
        assertFalse(repo.session.state.value.pool.contains(shown))
    }

    @Test
    fun exportString_spaceJoinedInOrder() {
        val repo = repositoryWith(listOf("欢", "给", "哈"))
        assertEquals("欢 给 哈", repo.exportString())
    }

    @Test
    fun exportString_empty_returnsEmptyString() {
        val repo = repositoryWith(emptyList())
        assertEquals("", repo.exportString())
    }

    @Test
    fun addMistake_accumulatesCount() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.addMistake("山")
        repo.addMistake("山")
        repo.addMistake("水")
        assertEquals(2, repo.mistakes.value["山"])
        assertEquals(1, repo.mistakes.value["水"])
    }

    @Test
    fun removeMistake_deletesOnlyThatCharacter() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.addMistake("山")
        repo.addMistake("水")
        repo.removeMistake("山")
        assertNull(repo.mistakes.value["山"])
        assertEquals(1, repo.mistakes.value["水"])
    }

    @Test
    fun clearMistakes_emptiesBook() {
        val repo = repositoryWith(listOf("山"))
        repo.addMistake("山")
        repo.clearMistakes()
        assertTrue(repo.mistakes.value.isEmpty())
    }

    @Test
    fun mistakesSorted_ordersByCountDescending() {
        val repo = repositoryWith(listOf("水", "山", "田"))
        repo.addMistake("水")
        repo.addMistake("山")
        repo.addMistake("山")
        repo.addMistake("田")
        repo.addMistake("田")
        repo.addMistake("田")
        assertEquals(listOf("田", "山", "水"), repo.mistakesSorted().map { it.char })
        assertEquals(listOf(3, 2, 1), repo.mistakesSorted().map { it.count })
    }

    @Test
    fun mistakesSorted_tiesFollowLibraryOrder() {
        val repo = repositoryWith(listOf("山", "水", "田"))
        repo.addMistake("田")
        repo.addMistake("山")
        repo.addMistake("水")
        assertEquals(listOf("山", "水", "田"), repo.mistakesSorted().map { it.char })
    }

    @Test
    fun mistakes_persistAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val first = CharacterRepository(store)
        first.addMistake("山")
        first.addMistake("山")
        first.addMistake("水")

        val second = CharacterRepository(store)
        assertEquals(2, second.mistakes.value["山"])
        assertEquals(1, second.mistakes.value["水"])
    }

    @Test
    fun roundPersistsAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val first = CharacterRepository(store)
        first.session.goNext()
        first.session.goPrevious()
        first.persistRound()

        val second = CharacterRepository(store)
        assertEquals(first.session.state.value.pool, second.session.state.value.pool)
        assertEquals(first.session.state.value.history, second.session.state.value.history)
        assertEquals(first.session.state.value.cursor, second.session.state.value.cursor)
        assertEquals(first.session.state.value.current, second.session.state.value.current)
        assertEquals(first.session.state.value.total, second.session.state.value.total)
        assertEquals(first.session.state.value.isFinished, second.session.state.value.isFinished)
    }

    @Test
    fun finishedRoundRestoresAsFinished() {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", "甲")
        val first = CharacterRepository(store)
        assertTrue(first.session.state.value.isFinished)
        first.persistRound()

        val second = CharacterRepository(store)
        assertTrue(second.session.state.value.isFinished)
        assertEquals("甲", second.session.state.value.current)
        assertEquals(1, second.session.state.value.total)
    }

    @Test
    fun invalidRoundSnapshot_isDiscarded() {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", "甲\n乙")
        store.putString("roundSnapshot", "garbage")
        val repo = CharacterRepository(store)
        assertEquals(2, repo.session.state.value.total)
        assertNotNull(repo.session.state.value.current)
    }

    @Test
    fun roundPersists_extendedBCharacter() {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", "𠀀")
        val first = CharacterRepository(store)
        first.persistRound()

        val second = CharacterRepository(store)
        assertEquals("𠀀", second.session.state.value.current)
        assertEquals(1, second.session.state.value.total)
    }
}
