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

    #if DEBUG
    /// Test-only: replace the whole library without touching persistence semantics.
    func replaceAllForTesting(_ characters: [String]) {
        allCharacters = characters
    }
    #endif
}
