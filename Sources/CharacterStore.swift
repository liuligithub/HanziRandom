import Foundation

final class CharacterStore: ObservableObject {
    static let preset: [String] = [
        "欢","给","哈","教","室","师","怎","惊","跳","歌",
        "舞","找","做","故","担","拍","伸","拥","抓","寒",
        "赛","温","湿","暖","冷","办","醒","被","对","杏"
    ]

    @Published private(set) var allCharacters: [String]
    @Published private(set) var pool: [String] = []
    @Published private(set) var current: String?
    @Published private(set) var total: Int = 0
    @Published private(set) var drawnCount: Int = 0
    @Published private(set) var isFinished: Bool = false

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

    /// Begin a fresh round: shuffle the whole library into the pool and draw the first.
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

    /// Reset to a brand-new shuffled round (same as startRound; kept distinct for the
    /// UI's "重置" button so intent reads clearly).
    func reset() {
        startRound()
    }

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

    #if DEBUG
    /// Test-only: replace the whole library without touching persistence semantics.
    func replaceAllForTesting(_ characters: [String]) {
        allCharacters = characters
    }
    #endif
}
