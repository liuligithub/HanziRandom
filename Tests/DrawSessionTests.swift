import XCTest
@testable import HanziRandom

final class DrawSessionTests: XCTestCase {
    /// Reproduces the mistake-review bug: after a short round finishes, going
    /// back must re-enable going forward through history.
    func test_afterFinished_previousThenNext_works() {
        let session = DrawSession(characters: ["甲", "乙"])
        // Draw through the whole 2-char round until finished.
        session.goNext()
        XCTAssertTrue(session.isFinished, "2-char round should finish after one goNext")

        let last = session.current
        session.goPrevious()
        XCTAssertNotEqual(session.current, last, "goPrevious should move back")
        XCTAssertTrue(session.canGoNext, "after stepping back, goNext must be available even when finished")

        session.goNext()
        XCTAssertEqual(session.current, last, "goNext should return to the later character")
    }

    func test_canGoNext_falseOnlyAtHistoryEndWhenFinished() {
        let session = DrawSession(characters: ["甲"])
        // Single-char round: drawn on init, pool empty → finished at history end.
        XCTAssertTrue(session.isFinished)
        XCTAssertFalse(session.canGoNext, "at the end of a finished round there is nothing next")
    }

    /// The single-mistake delete bug: removing the only character must clear the
    /// card and move to the finished state, not keep showing the deleted char.
    func test_removeCurrent_lastCharacter_finishesWithNoCurrent() {
        let session = DrawSession(characters: ["甲"])
        session.removeCurrent()
        XCTAssertNil(session.current, "after removing the only char there is nothing to show")
        XCTAssertTrue(session.isFinished)
        XCTAssertEqual(session.total, 0)
    }

    func test_removeCurrent_midRound_showsAnotherCharacter() {
        let session = DrawSession(characters: ["甲", "乙", "丙"])
        let removed = session.current
        session.removeCurrent()
        XCTAssertNotEqual(session.current, removed, "a different char should be shown after delete")
        XCTAssertNotNil(session.current)
        XCTAssertEqual(session.total, 2)
    }

    // MARK: - Removing a library character mid-round

    func test_removeCharacter_currentlyShown_dropsItFromHistoryAndPool() {
        let session = DrawSession(characters: ["甲", "乙", "丙"])
        let removed = session.current!
        session.removeCharacter(removed)

        XCTAssertNotEqual(session.current, removed)
        XCTAssertFalse(session.history.contains(removed))
        XCTAssertFalse(session.pool.contains(removed))
        XCTAssertEqual(session.total, 2)
    }

    func test_removeCharacter_stillInPool_dropsItAndKeepsCurrent() {
        let session = DrawSession(characters: ["甲", "乙"])
        let shown = session.current!
        let queued = session.pool[0]
        session.removeCharacter(queued)

        XCTAssertEqual(session.current, shown)
        XCTAssertFalse(session.pool.contains(queued))
        XCTAssertEqual(session.total, 1)
        XCTAssertTrue(session.isFinished, "empty pool means no further fresh draws")
    }

    func test_removeCharacter_notInRound_isNoOp() {
        let session = DrawSession(characters: ["甲", "乙"])
        let before = (session.total, session.current, session.pool, session.history)
        session.removeCharacter("丙")

        XCTAssertEqual(session.total, before.0)
        XCTAssertEqual(session.current, before.1)
        XCTAssertEqual(session.pool, before.2)
        XCTAssertEqual(session.history, before.3)
    }
}
