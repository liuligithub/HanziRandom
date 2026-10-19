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
    fun removeCharacter_beforeCursorWhileCurrentIsNull_keepsCurrentNull() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = emptyList(),
                history = listOf("甲", "乙", "丙"),
                cursor = 2,
                total = 3,
                isFinished = true
            )
        )
        session.removeCurrent()
        assertNull(session.state.value.current)

        session.removeCharacter("甲")

        val s = session.state.value
        assertNull(s.current)
        assertEquals(listOf("乙"), s.history)
        assertEquals(0, s.cursor)
    }

    @Test
    fun removeCharacter_historyBeforeCursor_keepsCurrentAndShiftsCursor() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = listOf("丁"),
                history = listOf("甲", "乙", "丙"),
                cursor = 2,
                total = 4,
                isFinished = false
            )
        )

        session.removeCharacter("甲")

        val s = session.state.value
        assertEquals("丙", s.current)
        assertEquals(1, s.cursor)
        assertEquals(listOf("乙", "丙"), s.history)
        assertEquals(3, s.total)
        assertEquals(2, s.drawnCount)
        assertFalse(s.isFinished)
    }

    @Test
    fun removeCharacter_historyAfterCursor_keepsCurrentAndCursor() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = listOf("丁"),
                history = listOf("甲", "乙", "丙"),
                cursor = 0,
                total = 4,
                isFinished = false
            )
        )

        session.removeCharacter("丙")

        val s = session.state.value
        assertEquals("甲", s.current)
        assertEquals(0, s.cursor)
        assertEquals(listOf("甲", "乙"), s.history)
        assertEquals(3, s.total)
        assertEquals(2, s.drawnCount)
        assertFalse(s.isFinished)
    }

    @Test
    fun removeCurrent_afterGoPrevious_showsNextHistoryEntry() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = emptyList(),
                history = listOf("甲", "乙", "丙"),
                cursor = 2,
                total = 3,
                isFinished = true
            )
        )
        session.goPrevious()

        session.removeCurrent()

        val s = session.state.value
        assertEquals("丙", s.current)
        assertEquals(1, s.cursor)
        assertEquals(listOf("甲", "丙"), s.history)
        assertEquals(2, s.total)
        assertEquals(2, s.drawnCount)
        assertTrue(s.isFinished)
    }

    @Test
    fun appendToPool_midRound_extendsPoolAndTotal() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = listOf("丙"),
                history = listOf("甲"),
                cursor = 0,
                total = 2,
                isFinished = false
            )
        )

        session.appendToPool(listOf("丁"))

        val s = session.state.value
        assertEquals(listOf("丙", "丁"), s.pool)
        assertEquals(3, s.total)
        assertEquals("甲", s.current)
    }

    @Test
    fun appendToPool_afterFinished_isNoOp() {
        val session = DrawSession(listOf("甲"))
        val before = session.state.value
        assertTrue(before.isFinished)

        session.appendToPool(listOf("乙"))

        assertEquals(before, session.state.value)
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
