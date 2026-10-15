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
}
