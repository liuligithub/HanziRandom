# 随机汉字抽取 App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a SwiftUI iOS app that randomly draws one Chinese character at a time (no repeats within a round), with manage/add/delete, Chinese TTS, adjustable font size, progress display, per-character copy, and share-to-Notes export.

**Architecture:** XcodeGen generates the `.xcodeproj` from a declarative `project.yml`, so the whole project is reproducible from the CLI. A `CharacterStore` (`ObservableObject`) owns all data + draw logic + `UserDefaults` persistence and is the only unit-tested piece. A `SpeechService` wraps `AVSpeechSynthesizer` (not unit-tested — audio hardware). Three SwiftUI views (DrawView, ManageView, CharacterDetailView) consume the store via `@EnvironmentObject`. Tests run via `xcodebuild test` against an iOS simulator.

**Tech Stack:** SwiftUI (iOS 16+ deployment target), Swift 6.4 / Xcode 27, XcodeGen, UserDefaults, AVSpeechSynthesizer, UIPasteboard, SwiftUI ShareLink. No third-party runtime dependencies.

---

## File Structure

Project root: `/Users/liuli/HanziRandom`

- `project.yml` — XcodeGen spec: defines the `HanziRandom` app target + `HanziRandomTests` unit-test target, bundle IDs, deployment target iOS 16, sources.
- `Sources/HanziRandomApp.swift` — `@main` App; creates `CharacterStore` as `@StateObject`, injects via `environmentObject`.
- `Sources/CharacterStore.swift` — core model + draw logic + persistence (the testable unit).
- `Sources/SpeechService.swift` — AVSpeechSynthesizer wrapper.
- `Sources/DrawView.swift` — main draw screen (progress, big char, font slider, Next/Reset, nav to Manage).
- `Sources/ManageView.swift` — add/paste input, export (copy + ShareLink), character list with swipe-delete + per-row copy + tap-to-detail.
- `Sources/CharacterDetailView.swift` — sheet showing one enlarged char with speak + copy.
- `Tests/CharacterStoreTests.swift` — unit tests for CharacterStore.

Each source file has one clear responsibility. CharacterStore holds all logic that changes together (data + draw + persistence). Views are split one-file-per-screen.

**Simulator target used throughout:** `platform=iOS Simulator,name=iPhone 16,OS=18.0` (confirmed available via `xcrun simctl list`). If that device/OS is missing on the executing machine, substitute any available `iPhone 16 / iOS 18.x` from `xcrun simctl list devices available`.

---

## Task 0: Install XcodeGen and scaffold the project

**Files:**
- Create: `project.yml`
- Create: `Sources/HanziRandomApp.swift`
- Create: `Sources/CharacterStore.swift` (stub)
- Create: `Tests/CharacterStoreTests.swift` (stub)

- [ ] **Step 1: Install XcodeGen**

Run:
```bash
brew install xcodegen
```
Expected: XcodeGen installs; `xcodegen --version` prints a version (e.g. `Version: 2.x`).

- [ ] **Step 2: Write `project.yml`**

Create `/Users/liuli/HanziRandom/project.yml`:
```yaml
name: HanziRandom
options:
  bundleIdPrefix: com.liuli.hanzirandom
  deploymentTarget:
    iOS: "16.0"
  createIntermediateGroups: true
settings:
  base:
    SWIFT_VERSION: "5.0"
    GENERATE_INFOPLIST_FILE: YES
    MARKETING_VERSION: "1.0"
    CURRENT_PROJECT_VERSION: "1"
targets:
  HanziRandom:
    type: application
    platform: iOS
    sources:
      - path: Sources
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.liuli.hanzirandom
        INFOPLIST_KEY_UILaunchScreen_Generation: YES
        INFOPLIST_KEY_UIApplicationSceneManifest_Generation: YES
        TARGETED_DEVICE_FAMILY: "1"
  HanziRandomTests:
    type: bundle.unit-test
    platform: iOS
    sources:
      - path: Tests
    dependencies:
      - target: HanziRandom
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.liuli.hanzirandom.tests
```

> Note: `SWIFT_VERSION: "5.0"` is the Swift *language mode* dialect, not the toolchain version. Xcode 27's Swift 6.4 compiler supports language mode 5.0; this avoids opt-in Swift 6 strict-concurrency breakage for a simple app.

- [ ] **Step 3: Write the `@main` App (uses CharacterStore stub)**

Create `/Users/liuli/HanziRandom/Sources/HanziRandomApp.swift`:
```swift
import SwiftUI

@main
struct HanziRandomApp: App {
    @StateObject private var store = CharacterStore()

    var body: some Scene {
        WindowGroup {
            Text("placeholder")
                .environmentObject(store)
        }
    }
}
```

- [ ] **Step 4: Write a minimal CharacterStore stub so the project compiles**

Create `/Users/liuli/HanziRandom/Sources/CharacterStore.swift`:
```swift
import Foundation

final class CharacterStore: ObservableObject {
}
```

- [ ] **Step 5: Write a trivial passing test so the test target compiles and runs**

Create `/Users/liuli/HanziRandom/Tests/CharacterStoreTests.swift`:
```swift
import XCTest
@testable import HanziRandom

final class CharacterStoreTests: XCTestCase {
    func test_scaffolding_compiles() {
        _ = CharacterStore()
        XCTAssertTrue(true)
    }
}
```

- [ ] **Step 6: Generate the Xcode project**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodegen generate
```
Expected: prints `Created project at /Users/liuli/HanziRandom/HanziRandom.xcodeproj`.

- [ ] **Step 7: Build + run tests to verify scaffolding**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj \
  -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  2>&1 | tail -20
```
Expected: `** TEST SUCCEEDED **` and `test_scaffolding_compiles` passes.

- [ ] **Step 8: Add a `.gitignore` and commit scaffolding**

Create `/Users/liuli/HanziRandom/.gitignore`:
```
# Xcode / build
build/
DerivedData/
*.xcuserstate
xcuserdata/
.DS_Store

# XcodeGen generates this; keep it out of VCS
HanziRandom.xcodeproj/
```

Run:
```bash
cd /Users/liuli/HanziRandom && git add project.yml Sources Tests .gitignore && git commit -m "chore: scaffold HanziRandom SwiftUI app via XcodeGen"
```

> Note: We gitignore the generated `.xcodeproj` so `project.yml` is the single source of truth. Anyone cloning runs `xcodegen generate` to recreate it. All later `xcodebuild` steps assume `xcodegen generate` has been run and the `.xcodeproj` exists locally.

---

## Task 1: Preset library + first-run initialization

**Files:**
- Modify: `Sources/CharacterStore.swift`
- Test: `Tests/CharacterStoreTests.swift`

The 31 preset characters (from spec): `欢 给 哈 教 室 师 怎 惊 跳 歌 舞 找 做 故 担 拍 伸 拥 抓 寒 赛 温 湿 暖 冷 办 醒 被 对 杏`

- [ ] **Step 1: Write the failing test**

Replace the body of `Tests/CharacterStoreTests.swift` with:
```swift
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
        XCTAssertEqual(store.allCharacters.count, 31)
        XCTAssertEqual(store.allCharacters.first, "欢")
        XCTAssertEqual(store.allCharacters.last, "杏")
        XCTAssertTrue(store.allCharacters.contains("舞"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests/test_firstRun_initializesWithPresetCharacters \
  2>&1 | tail -20
```
Expected: FAIL — `CharacterStore` has no `init(defaults:)` and no `allCharacters` → compile error / test failure.

- [ ] **Step 3: Implement preset + first-run init**

Replace `Sources/CharacterStore.swift` with:
```swift
import Foundation

final class CharacterStore: ObservableObject {
    static let presetCharacters: [String] = [
        "欢","给","哈","教","室","师","怎","惊","跳","歌",
        "舞","找","做","故","担","拍","伸","拥","抓","寒",
        "赛","温","湿","暖","冷","办","醒","被","对","杏"
    ].reduce(into: [String]()) { $0.append($1) } // placeholder; replaced below

    @Published private(set) var allCharacters: [String]

    private let defaults: UserDefaults
    private static let storageKey = "allCharacters"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let saved = defaults.array(forKey: Self.storageKey) as? [String] {
            self.allCharacters = saved
        } else {
            self.allCharacters = Self.preset
            defaults.set(Self.preset, forKey: Self.storageKey)
        }
    }

    private func persist() {
        defaults.set(allCharacters, forKey: Self.storageKey)
    }
}

extension CharacterStore {
    static let preset: [String] = [
        "欢","给","哈","教","室","师","怎","惊","跳","歌",
        "舞","找","做","故","担","拍","伸","拥","抓","寒",
        "赛","温","湿","暖","冷","办","醒","被","对","杏"
    ]
}
```

Then delete the bogus `static let presetCharacters` line (it was only a placeholder note) so the file reads cleanly:
```swift
import Foundation

final class CharacterStore: ObservableObject {
    static let preset: [String] = [
        "欢","给","哈","教","室","师","怎","惊","跳","歌",
        "舞","找","做","故","担","拍","伸","拥","抓","寒",
        "赛","温","湿","暖","冷","办","醒","被","对","杏"
    ]

    @Published private(set) var allCharacters: [String]

    private let defaults: UserDefaults
    private static let storageKey = "allCharacters"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let saved = defaults.array(forKey: Self.storageKey) as? [String] {
            self.allCharacters = saved
        } else {
            self.allCharacters = Self.preset
            defaults.set(Self.preset, forKey: Self.storageKey)
        }
    }

    private func persist() {
        defaults.set(allCharacters, forKey: Self.storageKey)
    }
}
```

> `preset` has 30 entries listed on 3 rows of 10. The spec says 31 and lists: 欢 给 哈 教 室 师 怎 惊 跳 歌 舞 找 做 故 担 拍 伸 拥 抓 寒 赛 温 湿 暖 冷 办 醒 被 对 杏 — count them: that is 30 characters. **The spec text says "31 个字" but lists 30.** Resolve by matching the listed glyphs (the authoritative content) and update the test's expected count to the actual number of listed glyphs. See Step 4.

- [ ] **Step 4: Correct the count to match the listed glyphs**

Count the spec's listed glyphs exactly: 欢(1) 给(2) 哈(3) 教(4) 室(5) 师(6) 怎(7) 惊(8) 跳(9) 歌(10) 舞(11) 找(12) 做(13) 故(14) 担(15) 拍(16) 伸(17) 拥(18) 抓(19) 寒(20) 赛(21) 温(22) 湿(23) 暖(24) 冷(25) 办(26) 醒(27) 被(28) 对(29) 杏(30) = **30 glyphs**.

Update the test in `Tests/CharacterStoreTests.swift` to assert 30:
```swift
        XCTAssertEqual(store.allCharacters.count, 30)
```
(Leave the `first == 欢`, `last == 杏`, `contains 舞` assertions unchanged.)

> **Flag for the human during review:** spec says 31 but lists 30 glyphs. We implemented the 30 listed glyphs. If a 31st character was intended, add it to both `preset` and the expected count. Do not silently invent a character.

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests/test_firstRun_initializesWithPresetCharacters \
  2>&1 | tail -20
```
Expected: PASS.

- [ ] **Step 6: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/CharacterStore.swift Tests/CharacterStoreTests.swift && git commit -m "feat: preset library and first-run initialization"
```

---

## Task 2: Draw logic — drawNext, no repeats, isFinished

**Files:**
- Modify: `Sources/CharacterStore.swift`
- Test: `Tests/CharacterStoreTests.swift`

State to add (per spec): `pool: [String]` (in-memory), `current: String?`, `total: Int`, `drawnCount: Int`, `isFinished: Bool`. A new round starts shuffled.

- [ ] **Step 1: Write the failing tests**

Append these methods inside `final class CharacterStoreTests` in `Tests/CharacterStoreTests.swift`:
```swift
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
        // Every character drawn exactly once, covering the whole library.
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: FAIL — `startRound`, `drawNext`, `pool`, `current`, `total`, `drawnCount`, `isFinished` don't exist yet (compile errors).

- [ ] **Step 3: Implement draw state + logic**

Add the published state and methods to `Sources/CharacterStore.swift`. Insert the new `@Published` properties right after the existing `allCharacters` declaration, and add the methods before the closing brace of the class:
```swift
    @Published private(set) var pool: [String] = []
    @Published private(set) var current: String?
    @Published private(set) var total: Int = 0
    @Published private(set) var drawnCount: Int = 0
    @Published private(set) var isFinished: Bool = false

    /// Begin a fresh round: shuffle the whole library into the pool and draw the first.
    func startRound() {
        pool = allCharacters.shuffled()
        total = allCharacters.count
        drawnCount = 0
        current = nil
        isFinished = false
        drawNext()
    }

    /// Draw one character out of the pool (no repeats). When the pool empties, finish.
    func drawNext() {
        guard !isFinished else { return }
        guard !pool.isEmpty else {
            isFinished = true
            return
        }
        let index = Int.random(in: 0..<pool.count)
        current = pool.remove(at: index)
        drawnCount += 1
        if pool.isEmpty {
            isFinished = true
        }
    }
```

> Note on `isFinished` timing: the last `drawNext()` both sets `current` to the final glyph AND flips `isFinished = true` (because the pool just emptied). This matches the spec's "第 30 / 共 30" being visible together with the final character; the DrawView shows the finished banner alongside the last glyph. The `drawnCount` reaches `total` exactly. An empty library is handled in Task 3.

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: PASS (all tests, including Task 1's).

- [ ] **Step 5: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/CharacterStore.swift Tests/CharacterStoreTests.swift && git commit -m "feat: round-based draw logic with no repeats"
```

---

## Task 3: reset(), empty-library handling, and progress

**Files:**
- Modify: `Sources/CharacterStore.swift`
- Test: `Tests/CharacterStoreTests.swift`

Per spec: `reset()` reshuffles the whole library back into the pool, zeroes drawnCount, draws the first. Empty library → Next unavailable; nothing to draw. Progress is `drawnCount / total`.

- [ ] **Step 1: Write the failing tests**

Append inside `final class CharacterStoreTests`:
```swift
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
        // Remove all characters first (remove() lands in Task 5; emulate via repeated
        // removal once it exists). For now, build an empty store directly:
        let emptyStore = store
        emptyStore.replaceAllForTesting([])
        emptyStore.startRound()
        XCTAssertNil(emptyStore.current)
        XCTAssertTrue(emptyStore.isFinished)
        XCTAssertEqual(emptyStore.total, 0)
        XCTAssertEqual(emptyStore.drawnCount, 0)
    }
```

> Note: `replaceAllForTesting` is a tiny test-only seam so this task doesn't depend on Task 5's `remove`. It's declared `internal` and only used in tests.

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: FAIL — `reset`, `replaceAllForTesting` don't exist; empty-library path not handled.

- [ ] **Step 3: Implement reset, empty-library guard, and the test seam**

Add to `Sources/CharacterStore.swift` (methods before the class closing brace):
```swift
    /// Reset to a brand-new shuffled round (same as startRound; kept distinct for the
    /// UI's "重置" button so intent reads clearly).
    func reset() {
        startRound()
    }

    #if DEBUG
    /// Test-only: replace the whole library without touching persistence semantics.
    func replaceAllForTesting(_ characters: [String]) {
        allCharacters = characters
    }
    #endif
```

Update `startRound()` to guard the empty case (replace the existing `startRound()` body):
```swift
    func startRound() {
        total = allCharacters.count
        drawnCount = 0
        current = nil
        isFinished = false
        pool = allCharacters.shuffled()
        if pool.isEmpty {
            isFinished = true
            return
        }
        drawNext()
    }
```

> `isFinished = true` on an empty library is what the UI uses to keep "下一个" disabled and show the guidance text; `current` stays `nil`.

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/CharacterStore.swift Tests/CharacterStoreTests.swift && git commit -m "feat: reset and empty-library handling"
```

---

## Task 4: addCharacters — split, filter non-Hanzi, double dedup, append order

**Files:**
- Modify: `Sources/CharacterStore.swift`
- Test: `Tests/CharacterStoreTests.swift`

Per spec: split input into single characters; drop whitespace and non-Hanzi; dedup twice — (1) within the input itself, keep first occurrence; (2) skip chars already in `allCharacters`. New chars appended to the end in first-seen order. Persist after.

"Hanzi" = a CJK unified ideograph. We detect it with a Unicode scalar range check covering the common CJK blocks.

- [ ] **Step 1: Write the failing tests**

Append inside `final class CharacterStoreTests`:
```swift
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
        // 给 and 欢 already exist → skipped; 山 水 appended in first-seen order.
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
```

> `replaceAllForTesting` intentionally does NOT persist, so `test_addCharacters_persists` proves `addCharacters` itself writes to defaults (store2 re-reads and still sees 山/水). The preset-seed write on first `init` also persists, but the assertions only check the newly added glyphs.

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: FAIL — `addCharacters(from:)` not defined.

- [ ] **Step 3: Implement addCharacters + Hanzi detection**

Add to `Sources/CharacterStore.swift` (before the class closing brace):
```swift
    /// Merge characters parsed from arbitrary pasted text into the library.
    /// Rules: split by Character, keep only CJK ideographs, dedup within the input
    /// (first occurrence wins) and against the existing library, append in first-seen order.
    func addCharacters(from text: String) {
        let existing = Set(allCharacters)
        var seenThisInput = Set<String>()
        var toAppend: [String] = []

        for ch in text {
            let s = String(ch)
            guard Self.isHanzi(ch) else { continue }
            guard !existing.contains(s), !seenThisInput.contains(s) else { continue }
            seenThisInput.insert(s)
            toAppend.append(s)
        }

        guard !toAppend.isEmpty else { return }
        allCharacters.append(contentsOf: toAppend)
        persist()
    }

    /// True when the character is a single CJK unified ideograph (common + ext-A + compatibility).
    static func isHanzi(_ ch: Character) -> Bool {
        let scalars = ch.unicodeScalars
        guard scalars.count == 1, let scalar = scalars.first else { return false }
        let v = scalar.value
        return (0x4E00...0x9FFF).contains(v)   // CJK Unified Ideographs
            || (0x3400...0x4DBF).contains(v)   // CJK Ext A
            || (0xF900...0xFAFF).contains(v)   // CJK Compatibility Ideographs
            || (0x20000...0x2A6DF).contains(v) // CJK Ext B
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/CharacterStore.swift Tests/CharacterStoreTests.swift && git commit -m "feat: addCharacters with filtering and double dedup"
```

---

## Task 5: remove() and exportString()

**Files:**
- Modify: `Sources/CharacterStore.swift`
- Test: `Tests/CharacterStoreTests.swift`

Per spec: `remove(_:)` deletes from `allCharacters` and persists. `exportString()` returns all characters space-joined, in `allCharacters` order.

- [ ] **Step 1: Write the failing tests**

Append inside `final class CharacterStoreTests`:
```swift
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: FAIL — `remove(_:)` and `exportString()` not defined.

- [ ] **Step 3: Implement remove + exportString**

Add to `Sources/CharacterStore.swift` (before the class closing brace):
```swift
    /// Remove a character from the library and persist. No-op if absent.
    func remove(_ character: String) {
        guard let idx = allCharacters.firstIndex(of: character) else { return }
        allCharacters.remove(at: idx)
        persist()
    }

    /// Space-joined representation of the whole library, used for both clipboard copy
    /// and ShareLink export.
    func exportString() -> String {
        allCharacters.joined(separator: " ")
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -only-testing:HanziRandomTests/CharacterStoreTests \
  2>&1 | tail -25
```
Expected: PASS. CharacterStore logic is now fully implemented and tested.

- [ ] **Step 5: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/CharacterStore.swift Tests/CharacterStoreTests.swift && git commit -m "feat: remove and exportString"
```

---

## Task 6: SpeechService

**Files:**
- Create: `Sources/SpeechService.swift`

Not unit-tested (audio hardware). Per spec: `speak(_:)` uses zh-CN voice; stop any prior utterance before speaking so repeated taps don't queue up.

- [ ] **Step 1: Write SpeechService**

Create `/Users/liuli/HanziRandom/Sources/SpeechService.swift`:
```swift
import AVFoundation

final class SpeechService {
    private let synthesizer = AVSpeechSynthesizer()

    func speak(_ text: String) {
        guard !text.isEmpty else { return }
        // Stop any in-flight utterance so rapid taps don't queue/overlap.
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        let utterance = AVSpeechUtterance(string: text)
        // May be nil if the zh-CN voice is unavailable; AVSpeechUtterance tolerates a
        // nil voice (falls back to default) — spec requires silent degradation, not a crash.
        utterance.voice = AVSpeechSynthesisVoice(language: "zh-CN")
        synthesizer.speak(utterance)
    }
}
```

- [ ] **Step 2: Verify it compiles (build only)**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodegen generate && xcodebuild build \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

> Re-run `xcodegen generate` whenever you add a new source file so it's included in the target.

- [ ] **Step 3: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/SpeechService.swift && git commit -m "feat: SpeechService zh-CN wrapper"
```

---

## Task 7: DrawView — progress, big char, font slider, Next/Reset, nav

**Files:**
- Create: `Sources/DrawView.swift`
- Modify: `Sources/HanziRandomApp.swift`

Per spec: top progress "第 X / 共 Y"; center big current char (tap → speak; long-press contextMenu → copy); font-size slider (persisted to UserDefaults); bottom "下一个" button (disabled when finished or empty); finished → "全部抽完" + "重置"; empty library → guidance text + Next disabled; nav bar entry to ManageView. Call `startRound()` on first appear.

- [ ] **Step 1: Create DrawView**

Create `/Users/liuli/HanziRandom/Sources/DrawView.swift`:
```swift
import SwiftUI

struct DrawView: View {
    @EnvironmentObject private var store: CharacterStore
    private let speech = SpeechService()

    // Font size preference persisted automatically via @AppStorage.
    @AppStorage("fontSize") private var fontSize: Double = 120
    @State private var didStart = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 32) {
                progressHeader
                Spacer()
                characterArea
                Spacer()
                fontSlider
                bottomControls
            }
            .padding()
            .navigationTitle("随机汉字")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink("管理") { ManageView() }
                }
            }
            .onAppear {
                if !didStart {
                    store.startRound()
                    didStart = true
                }
            }
        }
    }

    @ViewBuilder private var progressHeader: some View {
        if store.total > 0 {
            Text("第 \(store.drawnCount) / 共 \(store.total)")
                .font(.headline)
                .foregroundStyle(.secondary)
        }
    }

    @ViewBuilder private var characterArea: some View {
        if store.allCharacters.isEmpty {
            Text("请先在管理页添加汉字")
                .font(.title3)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        } else if let current = store.current {
            Text(current)
                .font(.system(size: fontSize))
                .onTapGesture { speech.speak(current) }
                .contextMenu {
                    Button("复制") { UIPasteboard.general.string = current }
                }
        }
    }

    @ViewBuilder private var fontSlider: some View {
        if store.current != nil {
            Slider(value: $fontSize, in: 60...260) {
                Text("字号")
            }
        }
    }

    @ViewBuilder private var bottomControls: some View {
        if store.allCharacters.isEmpty {
            Button("下一个") {}.buttonStyle(.borderedProminent).disabled(true)
        } else if store.isFinished {
            VStack(spacing: 16) {
                Text("全部抽完").font(.title2).bold()
                Button("重置") { store.reset() }
                    .buttonStyle(.borderedProminent)
            }
        } else {
            Button("下一个") { store.drawNext() }
                .buttonStyle(.borderedProminent)
                .font(.title2)
        }
    }
}
```

- [ ] **Step 2: Wire DrawView into the App**

Replace `Sources/HanziRandomApp.swift` body with the real root view:
```swift
import SwiftUI

@main
struct HanziRandomApp: App {
    @StateObject private var store = CharacterStore()

    var body: some Scene {
        WindowGroup {
            DrawView()
                .environmentObject(store)
        }
    }
}
```

> ManageView doesn't exist yet (Task 8). This step will not compile until Task 8 lands. To keep the build green between tasks, add a temporary stub now and delete it in Task 8 Step 1. Create `Sources/ManageView.swift` with a placeholder:
```swift
import SwiftUI

struct ManageView: View {
    var body: some View { Text("管理页") }
}
```

- [ ] **Step 3: Regenerate + build**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodegen generate && xcodebuild build \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/DrawView.swift Sources/HanziRandomApp.swift Sources/ManageView.swift && git commit -m "feat: DrawView with progress, font slider, next/reset, speak and copy"
```

---

## Task 8: ManageView — add input, export (copy + ShareLink), list with delete/copy/detail

**Files:**
- Modify: `Sources/ManageView.swift` (replace the stub)

Per spec: multiline paste input + "添加" button → addCharacters; export region with "复制到剪贴板" (button text briefly flips to "已复制") and "存到备忘录" via ShareLink of `exportString()`; list of allCharacters with swipe-to-delete, a trailing per-row "复制" button, and tap-row → CharacterDetailView sheet.

- [ ] **Step 1: Replace the ManageView stub with the full screen**

Replace the entire contents of `/Users/liuli/HanziRandom/Sources/ManageView.swift`:
```swift
import SwiftUI

struct ManageView: View {
    @EnvironmentObject private var store: CharacterStore

    @State private var inputText: String = ""
    @State private var copiedAll = false
    @State private var detailCharacter: String?

    var body: some View {
        List {
            addSection
            exportSection
            charactersSection
        }
        .navigationTitle("管理汉字")
        .sheet(item: Binding(
            get: { detailCharacter.map(IdentifiableString.init) },
            set: { detailCharacter = $0?.value }
        )) { item in
            CharacterDetailView(character: item.value)
        }
    }

    private var addSection: some View {
        Section("添加汉字（可粘贴整段）") {
            TextEditor(text: $inputText)
                .frame(minHeight: 100)
            Button("添加") {
                store.addCharacters(from: inputText)
                inputText = ""
            }
            .disabled(inputText.isEmpty)
        }
    }

    private var exportSection: some View {
        Section("导出全部") {
            Button(copiedAll ? "已复制" : "复制到剪贴板") {
                UIPasteboard.general.string = store.exportString()
                copiedAll = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    copiedAll = false
                }
            }
            ShareLink("存到备忘录", item: store.exportString())
        }
    }

    private var charactersSection: some View {
        Section("字库（\(store.allCharacters.count)）") {
            ForEach(store.allCharacters, id: \.self) { ch in
                HStack {
                    Text(ch).font(.title2)
                    Spacer()
                    Button {
                        UIPasteboard.general.string = ch
                    } label: {
                        Image(systemName: "doc.on.doc")
                    }
                    .buttonStyle(.borderless)
                }
                .contentShape(Rectangle())
                .onTapGesture { detailCharacter = ch }
            }
            .onDelete { offsets in
                for index in offsets {
                    store.remove(store.allCharacters[index])
                }
            }
        }
    }
}

/// Wrapper so a plain String can drive `.sheet(item:)`.
private struct IdentifiableString: Identifiable {
    let value: String
    var id: String { value }
}
```

> `.sheet(item:)` requires an `Identifiable`. We wrap the selected character string. CharacterDetailView is built in Task 9; add a temporary stub to keep the build green, then Task 9 replaces it.

- [ ] **Step 2: Add a temporary CharacterDetailView stub**

Create `/Users/liuli/HanziRandom/Sources/CharacterDetailView.swift`:
```swift
import SwiftUI

struct CharacterDetailView: View {
    let character: String
    var body: some View { Text(character) }
}
```

- [ ] **Step 3: Regenerate + build**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodegen generate && xcodebuild build \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/ManageView.swift Sources/CharacterDetailView.swift && git commit -m "feat: ManageView with add, export, list delete/copy/detail"
```

---

## Task 9: CharacterDetailView — enlarged char, speak, copy, close

**Files:**
- Modify: `Sources/CharacterDetailView.swift` (replace the stub)

Per spec: enlarged centered char; tap → speak zh-CN; "复制" copies this single char; close button dismisses.

- [ ] **Step 1: Replace the stub with the full detail view**

Replace the entire contents of `/Users/liuli/HanziRandom/Sources/CharacterDetailView.swift`:
```swift
import SwiftUI

struct CharacterDetailView: View {
    let character: String

    @Environment(\.dismiss) private var dismiss
    private let speech = SpeechService()
    @State private var copied = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 40) {
                Spacer()
                Text(character)
                    .font(.system(size: 200))
                    .onTapGesture { speech.speak(character) }
                Button(copied ? "已复制" : "复制") {
                    UIPasteboard.general.string = character
                    copied = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        copied = false
                    }
                }
                .buttonStyle(.borderedProminent)
                Spacer()
            }
            .frame(maxWidth: .infinity)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Regenerate + build**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodegen generate && xcodebuild build \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 3: Commit**

Run:
```bash
cd /Users/liuli/HanziRandom && git add Sources/CharacterDetailView.swift && git commit -m "feat: CharacterDetailView with speak and single-char copy"
```

---

## Task 10: Full test + build pass and manual verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run:
```bash
cd /Users/liuli/HanziRandom && xcodegen generate && xcodebuild test \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  2>&1 | tail -30
```
Expected: `** TEST SUCCEEDED **`, all CharacterStoreTests pass.

- [ ] **Step 2: Launch in the simulator for manual UI verification**

Run:
```bash
cd /Users/liuli/HanziRandom && xcrun simctl boot "iPhone 16" 2>/dev/null; open -a Simulator
xcodebuild build \
  -project HanziRandom.xcodeproj -scheme HanziRandom \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.0' \
  -derivedDataPath build 2>&1 | tail -5
xcrun simctl install "iPhone 16" "$(find build/Build/Products -name 'HanziRandom.app' | head -1)"
xcrun simctl launch "iPhone 16" com.liuli.hanzirandom
```
Expected: app launches showing a random character and "第 1 / 共 30".

- [ ] **Step 3: Manual checklist (verify by hand in the simulator — audio & share can't be unit-tested)**

Verify each:
- Progress shows "第 1 / 共 30" on launch.
- Tapping the big character speaks it in Chinese (needs simulator audio; silent-fail is acceptable if voice absent).
- Long-press the big character → "复制" copies it.
- Font slider enlarges/shrinks the character.
- "下一个" advances with no repeats; after the 30th, "全部抽完" + "重置" appear.
- "重置" starts a fresh round at "第 1 / 共 30".
- Manage page: paste "山 水 山 田a水", tap 添加 → only 山 水 田 appended (dedup + filter).
- Manage list: swipe-delete removes a row; trailing copy button copies that char.
- Tap a row → detail sheet with enlarged char; tap char speaks; "复制" copies; "关闭" dismisses.
- "复制到剪贴板" flips to "已复制" briefly; "存到备忘录" opens the system share sheet.
- Delete all characters → DrawView shows "请先在管理页添加汉字" and "下一个" is disabled.
- Kill and relaunch → added/deleted characters and font size persist.

- [ ] **Step 4: Final commit (if any tweaks were needed)**

Run:
```bash
cd /Users/liuli/HanziRandom && git add -A && git commit -m "test: full suite green; manual UI verification complete"
```
(Skip if there were no changes.)

---

## Self-Review Notes (for the executor)

- **Spec coverage:** preset+first-run (T1), no-repeat draw + isFinished (T2), reset + empty-library (T3), addCharacters double-dedup+filter+order (T4), remove + exportString (T5), SpeechService zh-CN stop-before-speak (T6), DrawView progress/font/next/reset/speak/copy (T7), ManageView add/export-copy/ShareLink/list-delete/row-copy/detail (T8), CharacterDetailView enlarge/speak/copy/close (T9), per-character copy across all three surfaces (T7 contextMenu, T8 row button, T9 button). All spec sections mapped.
- **Known spec discrepancy flagged:** spec says "31 个字" but lists 30 glyphs (Task 1 Step 4). Implemented 30; must be confirmed with the human.
- **Type consistency:** `allCharacters`, `pool`, `current`, `total`, `drawnCount`, `isFinished`, `startRound()`, `drawNext()`, `reset()`, `addCharacters(from:)`, `remove(_:)`, `exportString()`, `isHanzi(_:)` used consistently across tasks and views.
- **Build-greenness between tasks:** temporary stubs for ManageView (T7) and CharacterDetailView (T8) are introduced before their real implementations and replaced in T8/T9 respectively.
