package com.liuli.hanzirandom.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawSessionTest {

    @Test
    fun afterFinished_previousThenNext_works() {
        val session = DrawSession(listOf("甲", "乙"))
        session.goNext()
        assertTrue(session.state.value.isFinished)

        val last = session.state.value.current
        session.goPrevious()
        assertNotEquals(last, session.state.value.current)
        assertTrue(session.state.value.canGoNext)

        session.goNext()
        assertEquals(last, session.state.value.current)
    }

    @Test
    fun canGoNext_falseOnlyAtHistoryEndWhenFinished() {
        val session = DrawSession(listOf("甲"))
        assertTrue(session.state.value.isFinished)
        assertFalse(session.state.value.canGoNext)
    }

    @Test
    fun removeCurrent_lastCharacter_finishesWithNoCurrent() {
        val session = DrawSession(listOf("甲"))
        session.removeCurrent()
        assertNull(session.state.value.current)
        assertTrue(session.state.value.isFinished)
        assertEquals(0, session.state.value.total)
    }

    @Test
    fun removeCurrent_midRound_showsAnotherCharacter() {
        val session = DrawSession(listOf("甲", "乙", "丙"))
        val removed = session.state.value.current
        session.removeCurrent()
        assertNotEquals(removed, session.state.value.current)
        assertNotNull(session.state.value.current)
        assertEquals(2, session.state.value.total)
    }

    @Test
    fun removeCharacter_currentlyShown_dropsItFromHistoryAndPool() {
        val session = DrawSession(listOf("甲", "乙", "丙"))
        val removed = session.state.value.current!!
        session.removeCharacter(removed)
        assertNotEquals(removed, session.state.value.current)
        assertFalse(session.state.value.history.contains(removed))
        assertFalse(session.state.value.pool.contains(removed))
        assertEquals(2, session.state.value.total)
    }

    @Test
    fun removeCharacter_stillInPool_dropsItAndKeepsCurrent() {
        val session = DrawSession(listOf("甲", "乙"))
        val shown = session.state.value.current!!
        val queued = session.state.value.pool[0]
        session.removeCharacter(queued)
        assertEquals(shown, session.state.value.current)
        assertFalse(session.state.value.pool.contains(queued))
        assertEquals(1, session.state.value.total)
        assertTrue(session.state.value.isFinished)
    }

    @Test
    fun removeCharacter_notInRound_isNoOp() {
        val session = DrawSession(listOf("甲", "乙"))
        val before = session.state.value
        session.removeCharacter("丙")
        assertEquals(before, session.state.value)
    }

    @Test
    fun drawNext_noRepeatsWithinRound() {
        val session = DrawSession(listOf("甲", "乙", "丙", "丁"))
        val seen = mutableListOf(session.state.value.current!!)
        while (!session.state.value.isFinished) {
            session.drawNext()
            session.state.value.current?.let(seen::add)
        }
        assertEquals(4, seen.size)
        assertEquals(4, seen.toSet().size)
    }

    @Test
    fun restore_reproducesState() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = listOf("丙"),
                history = listOf("甲", "乙"),
                cursor = 1,
                total = 3,
                isFinished = false
            )
        )
        assertEquals("乙", session.state.value.current)
        assertEquals(1, session.state.value.cursor)
        assertEquals(2, session.state.value.drawnCount)
        assertEquals(3, session.state.value.total)
        assertTrue(session.state.value.canGoPrevious)
        assertTrue(session.state.value.canGoNext)
    }
}
