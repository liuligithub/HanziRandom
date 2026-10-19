package com.liuli.hanzirandom.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun defaults_matchIos() {
        val repo = SettingsRepository(InMemoryKeyValueStore())
        assertEquals(150f, repo.fontSize.value, 0.001f)
        assertTrue(repo.swipeEnabled.value)
    }

    @Test
    fun values_persistAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val first = SettingsRepository(store)
        first.setFontSize(200f)
        first.setSwipeEnabled(false)

        val second = SettingsRepository(store)
        assertEquals(200f, second.fontSize.value, 0.001f)
        assertFalse(second.swipeEnabled.value)
    }

    @Test
    fun fontSize_isClampedToRange() {
        val repo = SettingsRepository(InMemoryKeyValueStore())
        repo.setFontSize(10f)
        assertEquals(60f, repo.fontSize.value, 0.001f)
        repo.setFontSize(999f)
        assertEquals(260f, repo.fontSize.value, 0.001f)
    }
}
