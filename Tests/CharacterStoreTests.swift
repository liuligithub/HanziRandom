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

    func test_reset_restoresFullPoolAndDrawsFirst() {
        let store = makeEmptyStore()
        store.startRound()
        store.drawNext()
        store.drawNext()
        store.reset()
        XCTAssertEqual(store.drawnCount, 1)
        XCTAssertEqual(store.total, 30)
        XCTAssertNotNil(store.current)
        XCTAssertFalse(store.isFinished)
        XCTAssertEqual(store.pool.count, 29) // 30 total - 1 drawn
    }

    func test_emptyLibrary_startRound_isFinishedWithNoCurrent() {
        let store = makeEmptyStore()
        store.replaceAllForTesting([])
        store.startRound()
        XCTAssertNil(store.current)
        XCTAssertTrue(store.isFinished)
        XCTAssertEqual(store.total, 0)
        XCTAssertEqual(store.drawnCount, 0)
    }

    func test_addCharacters_appendsNewUniqueInOrder() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["欢", "给"])
        store.addCharacters(from: "山水田")
        XCTAssertEqual(store.allCharacters, ["欢", "给", "山", "水", "田"])
    }

    func test_addCharacters_skipsExisting() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["欢", "给"])
        store.addCharacters(from: "给山欢水")
        XCTAssertEqual(store.allCharacters, ["欢", "给", "山", "水"])
    }

    func test_addCharacters_dedupsWithinInput() {
        let store = makeEmptyStore()
        store.replaceAllForTesting([])
        store.addCharacters(from: "山山水山田水")
        XCTAssertEqual(store.allCharacters, ["山", "水", "田"])
    }

    func test_addCharacters_filtersWhitespaceAndNonHanzi() {
        let store = makeEmptyStore()
        store.replaceAllForTesting([])
        store.addCharacters(from: " 山a水\n1，田! 水 ")
        XCTAssertEqual(store.allCharacters, ["山", "水", "田"])
    }

    func test_addCharacters_persists() {
        let suite = "test.persist.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let store1 = CharacterStore(defaults: defaults)
        store1.replaceAllForTesting([]) // note: replaceAllForTesting does not persist
        store1.addCharacters(from: "山水")   // addCharacters DOES persist
        let store2 = CharacterStore(defaults: defaults)
        XCTAssertTrue(store2.allCharacters.contains("山"))
        XCTAssertTrue(store2.allCharacters.contains("水"))
    }
}
