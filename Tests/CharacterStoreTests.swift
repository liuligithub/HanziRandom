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
        XCTAssertEqual(store.allCharacters, CharacterStore.preset)
        XCTAssertEqual(store.allCharacters.first, "一")
        XCTAssertTrue(store.allCharacters.contains("舞"))
    }

    func test_preset_hasNoDuplicates() {
        XCTAssertEqual(CharacterStore.preset.count, Set(CharacterStore.preset).count)
    }

    func test_preset_allHanzi() {
        for s in CharacterStore.preset {
            XCTAssertTrue(CharacterStore.isHanzi(Character(s)), "non-hanzi in preset: \(s)")
        }
    }

    func test_startRound_setsTotalAndDrawsFirst() {
        let store = makeEmptyStore()
        store.startRound()
        XCTAssertEqual(store.session.total, CharacterStore.preset.count)
        XCTAssertEqual(store.session.drawnCount, 1)
        XCTAssertNotNil(store.session.current)
        XCTAssertFalse(store.session.isFinished)
    }

    func test_drawNext_noRepeatsWithinRound() {
        let store = makeEmptyStore()
        store.startRound()
        var seen: [String] = []
        if let c = store.session.current { seen.append(c) }
        while !store.session.isFinished {
            store.session.drawNext()
            if let c = store.session.current { seen.append(c) }
        }
        XCTAssertEqual(seen.count, CharacterStore.preset.count)
        XCTAssertEqual(Set(seen), Set(store.allCharacters))
    }

    func test_drawingLastCharacter_setsFinished() {
        let store = makeEmptyStore()
        store.startRound()
        for _ in 0..<(CharacterStore.preset.count - 1) { store.session.drawNext() }
        XCTAssertTrue(store.session.isFinished)
        XCTAssertEqual(store.session.drawnCount, CharacterStore.preset.count)
    }

    func test_drawNext_whenFinished_isNoOp() {
        let store = makeEmptyStore()
        store.startRound()
        while !store.session.isFinished { store.session.drawNext() }
        let finalCount = store.session.drawnCount
        store.session.drawNext()
        XCTAssertEqual(store.session.drawnCount, finalCount)
        XCTAssertTrue(store.session.isFinished)
    }

    func test_reset_restoresFullPoolAndDrawsFirst() {
        let store = makeEmptyStore()
        store.startRound()
        store.session.drawNext()
        store.session.drawNext()
        store.reset()
        XCTAssertEqual(store.session.drawnCount, 1)
        XCTAssertEqual(store.session.total, CharacterStore.preset.count)
        XCTAssertNotNil(store.session.current)
        XCTAssertFalse(store.session.isFinished)
        XCTAssertEqual(store.session.pool.count, CharacterStore.preset.count - 1)
    }

    func test_emptyLibrary_startRound_isFinishedWithNoCurrent() {
        let store = makeEmptyStore()
        store.replaceAllForTesting([])
        store.startRound()
        XCTAssertNil(store.session.current)
        XCTAssertTrue(store.session.isFinished)
        XCTAssertEqual(store.session.total, 0)
        XCTAssertEqual(store.session.drawnCount, 0)
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

    func test_addCharacters_acceptsExtensionBIdeograph() {
        let store = makeEmptyStore()
        store.replaceAllForTesting([])
        store.addCharacters(from: "\u{20000}") // CJK Ext B ideograph (single Unicode scalar)
        XCTAssertEqual(store.allCharacters, ["\u{20000}"])
    }

    func test_addCharacters_allDuplicatesDoesNotMutate() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["山", "水"])
        store.addCharacters(from: "山水山水") // every char already exists
        XCTAssertEqual(store.allCharacters, ["山", "水"]) // unchanged, nothing appended
    }

    func test_remove_deletesAndPersists() {
        let suite = "test.remove.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let store = CharacterStore(defaults: defaults)
        store.addCharacters(from: "山水田") // persisted (preset + these)
        store.remove("水")
        XCTAssertFalse(store.allCharacters.contains("水"))

        let reloaded = CharacterStore(defaults: defaults)
        XCTAssertFalse(reloaded.allCharacters.contains("水"))
    }

    func test_remove_nonexistent_isNoOp() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["山", "水"])
        store.remove("田")
        XCTAssertEqual(store.allCharacters, ["山", "水"])
    }

    func test_exportString_spaceJoinedInOrder() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["欢", "给", "哈"])
        XCTAssertEqual(store.exportString(), "欢 给 哈")
    }

    func test_exportString_empty_returnsEmptyString() {
        let store = makeEmptyStore()
        store.replaceAllForTesting([])
        XCTAssertEqual(store.exportString(), "")
    }

    // MARK: - Navigation history (previous/next)

    func test_afterStartRound_cannotGoPrevious() {
        let store = makeEmptyStore()
        store.startRound()
        XCTAssertFalse(store.session.canGoPrevious)
        XCTAssertEqual(store.session.position, 1)
    }

    func test_goPrevious_showsPreviouslyDrawnCharacter() {
        let store = makeEmptyStore()
        store.startRound()
        let first = store.session.current
        store.session.drawNext()
        let second = store.session.current
        XCTAssertNotEqual(first, second)
        XCTAssertTrue(store.session.canGoPrevious)

        store.session.goPrevious()
        XCTAssertEqual(store.session.current, first)
        XCTAssertFalse(store.session.canGoPrevious) // back at the start of the history
    }

    func test_goPrevious_atStart_isNoOp() {
        let store = makeEmptyStore()
        store.startRound()
        let first = store.session.current
        store.session.goPrevious()
        XCTAssertEqual(store.session.current, first)
    }

    func test_drawNext_afterGoPrevious_drawsBrandNewCharacter() {
        let store = makeEmptyStore()
        store.startRound()
        let first = store.session.current
        store.session.drawNext()
        let second = store.session.current
        store.session.drawNext()
        let third = store.session.current

        store.session.goPrevious() // -> second
        store.session.goPrevious() // -> first
        XCTAssertEqual(store.session.current, first)

        store.session.drawNext() // "next" always draws a new character
        let current = store.session.current
        XCTAssertNotEqual(current, first)
        XCTAssertNotEqual(current, second)
        XCTAssertNotEqual(current, third)
    }

    func test_reset_clearsHistory() {
        let store = makeEmptyStore()
        store.startRound()
        store.session.drawNext()
        XCTAssertTrue(store.session.canGoPrevious)
        store.reset()
        XCTAssertFalse(store.session.canGoPrevious)
        XCTAssertEqual(store.session.position, 1)
    }

    func test_position_tracksNavigation() {
        let store = makeEmptyStore()
        store.startRound()
        XCTAssertEqual(store.session.position, 1)
        store.session.drawNext()
        XCTAssertEqual(store.session.position, 2)
        store.session.goPrevious()
        XCTAssertEqual(store.session.position, 1)
    }

    /// Reproduces the reported ABC scenario: with B shown, 上一个 then 下一个 must land back on B.
    func test_previousThenNext_returnsToSameCharacter() {
        let store = makeEmptyStore()
        store.startRound()
        let a = store.session.current
        store.session.goNext()
        let b = store.session.current
        store.session.goNext()
        let c = store.session.current
        store.session.goPrevious() // back to B
        XCTAssertEqual(store.session.current, b)

        store.session.goPrevious() // A
        XCTAssertEqual(store.session.current, a)
        store.session.goNext() // should be B, not C
        XCTAssertEqual(store.session.current, b)
        XCTAssertNotEqual(store.session.current, c)
    }

    // MARK: - Mistake book

    func test_addMistake_accumulatesCount() {
        let store = makeEmptyStore()
        store.addMistake("山")
        store.addMistake("山")
        store.addMistake("水")
        XCTAssertEqual(store.mistakes["山"], 2)
        XCTAssertEqual(store.mistakes["水"], 1)
    }

    func test_removeMistake_deletesOnlyThatCharacter() {
        let store = makeEmptyStore()
        store.addMistake("山")
        store.addMistake("水")
        store.removeMistake("山")
        XCTAssertNil(store.mistakes["山"])
        XCTAssertEqual(store.mistakes["水"], 1)
    }

    func test_clearMistakes_emptiesBook() {
        let store = makeEmptyStore()
        store.addMistake("山")
        store.clearMistakes()
        XCTAssertTrue(store.mistakes.isEmpty)
    }

    func test_mistakesSorted_ordersByCountDescending() {
        let store = makeEmptyStore()
        store.addMistake("水")
        store.addMistake("山")
        store.addMistake("山")
        store.addMistake("田")
        store.addMistake("田")
        store.addMistake("田")
        XCTAssertEqual(store.mistakesSorted.map(\.char), ["田", "山", "水"])
        XCTAssertEqual(store.mistakesSorted.map(\.count), [3, 2, 1])
    }

    func test_mistakesSorted_tiesFollowLibraryOrder() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["山", "水", "田"])
        store.addMistake("田")
        store.addMistake("山")
        store.addMistake("水")
        XCTAssertEqual(store.mistakesSorted.map(\.char), ["山", "水", "田"])
    }

    func test_mistakes_persistAcrossStoreInstances() {
        let suite = "test.mistakes.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        let store1 = CharacterStore(defaults: defaults)
        store1.addMistake("山")
        store1.addMistake("山")
        store1.addMistake("水")

        let store2 = CharacterStore(defaults: defaults)
        XCTAssertEqual(store2.mistakes["山"], 2)
        XCTAssertEqual(store2.mistakes["水"], 1)
    }

    // MARK: - Adding characters mid-round

    func test_addCharacters_joinsCurrentRoundImmediately() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["山", "水"])
        store.startRound()
        XCTAssertEqual(store.session.total, 2)

        store.addCharacters(from: "田")
        XCTAssertEqual(store.session.total, 3)
        XCTAssertTrue(store.session.pool.contains("田"))

        // New char is actually drawable within this round.
        while !store.session.isFinished { store.session.goNext() }
        XCTAssertTrue(store.session.history.contains("田"))
    }

    // MARK: - Deleting from the library syncs round and mistake book

    func test_remove_alsoDropsShownCharacterFromRoundAndMistakes() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["甲", "乙", "丙"])
        store.startRound()
        let shown = store.session.current!
        store.addMistake(shown)

        store.remove(shown)

        XCTAssertNil(store.mistakes[shown])
        XCTAssertEqual(store.session.total, 2)
        XCTAssertNotEqual(store.session.current, shown)
        XCTAssertFalse(store.session.history.contains(shown))
        XCTAssertFalse(store.session.pool.contains(shown))
    }

    func test_remove_alsoDropsQueuedCharacterFromPool() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["甲", "乙"])
        store.startRound()
        let shown = store.session.current!
        let queued = store.session.pool[0]

        store.remove(queued)

        XCTAssertEqual(store.session.current, shown)
        XCTAssertFalse(store.session.pool.contains(queued))
        XCTAssertEqual(store.session.total, 1)
    }

    // MARK: - Round persistence

    func test_roundPersistsAcrossStoreInstances() {
        let suite = "test.round.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)

        let store1 = CharacterStore(defaults: defaults)
        store1.replaceAllForTesting(["甲", "乙", "丙", "丁"])
        store1.startRound()
        store1.session.goNext()
        store1.session.goPrevious()
        store1.persistRound()

        let store2 = CharacterStore(defaults: defaults)
        XCTAssertEqual(store2.session.pool, store1.session.pool)
        XCTAssertEqual(store2.session.history, store1.session.history)
        XCTAssertEqual(store2.session.cursor, store1.session.cursor)
        XCTAssertEqual(store2.session.current, store1.session.current)
        XCTAssertEqual(store2.session.total, store1.session.total)
        XCTAssertEqual(store2.session.isFinished, store1.session.isFinished)
    }

    func test_finishedRoundRestoresAsFinished() {
        let suite = "test.roundFinished.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)

        let store1 = CharacterStore(defaults: defaults)
        store1.replaceAllForTesting(["甲"])
        store1.startRound()
        XCTAssertTrue(store1.session.isFinished)
        store1.persistRound()

        let store2 = CharacterStore(defaults: defaults)
        XCTAssertTrue(store2.session.isFinished)
        XCTAssertEqual(store2.session.current, "甲")
        XCTAssertEqual(store2.session.total, 1)
    }

    func test_addCharacters_afterFinished_doesNotCorruptFinishedRound() {
        let store = makeEmptyStore()
        store.replaceAllForTesting(["山"])
        store.startRound()
        XCTAssertTrue(store.session.isFinished)

        store.addCharacters(from: "水")
        XCTAssertEqual(store.allCharacters, ["山", "水"])
        // The finished round stays finished until the user starts a new one.
        XCTAssertTrue(store.session.isFinished)
        store.reset()
        XCTAssertEqual(store.session.total, 2)
        XCTAssertFalse(store.session.isFinished)
    }
}
