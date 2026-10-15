import XCTest
@testable import HanziRandom

final class CharacterStoreTests: XCTestCase {
    /// Returns a store backed by a throwaway UserDefaults suite so tests never
    /// touch real app storage and start clean every time.
    private func makeEmptyStore(_ fn: StaticString = #function) -> CharacterStore {
        let suite = "test.\(fn).\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        return CharacterStore(defaults: defaults)
    }

    func test_firstRun_initializesWithPresetCharacters() {
        let store = makeEmptyStore()
        XCTAssertEqual(store.allCharacters.count, 30)
        XCTAssertEqual(store.allCharacters.first, "欢")
        XCTAssertEqual(store.allCharacters.last, "杏")
        XCTAssertTrue(store.allCharacters.contains("舞"))
    }

    func test_startRound_setsTotalAndDrawsFirst() {
        let store = makeEmptyStore()
        store.startRound()
        XCTAssertEqual(store.total, 30)
        XCTAssertEqual(store.drawnCount, 1)
        XCTAssertNotNil(store.current)
        XCTAssertFalse(store.isFinished)
    }

    func test_drawNext_noRepeatsWithinRound() {
        let store = makeEmptyStore()
        store.startRound()
        var seen: [String] = []
        if let c = store.current { seen.append(c) }
        while !store.isFinished {
            store.drawNext()
            if let c = store.current { seen.append(c) }
        }
        XCTAssertEqual(seen.count, 30)
        XCTAssertEqual(Set(seen), Set(store.allCharacters))
    }

    func test_drawingLastCharacter_setsFinished() {
        let store = makeEmptyStore()
        store.startRound()
        for _ in 0..<29 { store.drawNext() } // 1 from startRound + 29 = 30 drawn
        XCTAssertTrue(store.isFinished)
        XCTAssertEqual(store.drawnCount, 30)
    }

    func test_drawNext_whenFinished_isNoOp() {
        let store = makeEmptyStore()
        store.startRound()
        while !store.isFinished { store.drawNext() }
        let finalCount = store.drawnCount
        store.drawNext()
        XCTAssertEqual(store.drawnCount, finalCount)
        XCTAssertTrue(store.isFinished)
    }
}
