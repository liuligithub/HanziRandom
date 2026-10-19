import Foundation

final class CharacterStore: ObservableObject {
    /// Raw first-run character source; deduped into `preset` below.
    private static let presetSource =
        "一二三人个大山上下小雨天车开火门口月羊只有牛水中风头田土里在日白马手出" +
        "王子儿女刀木鸟伞鱼瓜气飞虫米云电石了来玉去回多少问早巴父工力不干几男坐打" +
        "四五六闪的花朵果比午面包高文分平我连元丁厂目见兔禾是把笑着看会自己画好爱心" +
        "也可家爸妈这爷奶们书串球圆形长方正角灯块冰那弓买哪乐谢想抱竹要立耳红色吃香" +
        "穿身它发衣他她毛朋友哥弟兄眉鼻光星阳亮今明走兴空两哭牙肚尾你脸黑半呀吗" +
        "七八九十又很太宝贝和关园生公苗叶林草先岛嘴眼睛脚腿翅膀羽体没认识颜黄蓝绿" +
        "粉青紫彩食物尝闻味鲜苦甜什么起运动踢足赛快慢跑步唱歌跳舞吹号讲故事怎帮找欢" +
        "担闷急害怕惊哈老师同学校桌椅教室课做给操伸拉举抓拍拥扔寒冷温暖热凉湿雾净" +
        "到都办渴喝饿饭睡觉累醒房间帘窗户台视床被处让对姐妹阿姨叔伯姑亲餐盆碗筷盖袋" +
        "巾杯成用像原菜萝卜豆苹桃杏笋扫地拖洒洗服晒收全呢还谁"

    static let preset: [String] = {
        var seen = Set<String>()
        var result: [String] = []
        for ch in presetSource {
            let s = String(ch)
            if seen.insert(s).inserted { result.append(s) }
        }
        return result
    }()

    @Published private(set) var allCharacters: [String]

    /// The learning round. Separate from any mistake-review session the UI creates.
    let session = DrawSession()

    /// Wrong attempts per character; drives the mistake book ordering.
    @Published private(set) var mistakes: [String: Int] = [:]

    private let defaults: UserDefaults
    private static let storageKey = "allCharacters"
    private static let mistakesKey = "mistakes"
    private static let roundKey = "roundSnapshot"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--uitest-reset") {
            defaults.removeObject(forKey: Self.storageKey)
            defaults.removeObject(forKey: Self.mistakesKey)
            defaults.removeObject(forKey: Self.roundKey)
        }
        #endif
        if let saved = defaults.array(forKey: Self.storageKey) as? [String] {
            self.allCharacters = saved
        } else {
            self.allCharacters = Self.preset
            defaults.set(Self.preset, forKey: Self.storageKey)
        }
        if let saved = defaults.dictionary(forKey: Self.mistakesKey) as? [String: Int] {
            self.mistakes = saved
        }
        restoreRound()
    }

    private func persist() {
        defaults.set(allCharacters, forKey: Self.storageKey)
    }

    /// Save the learning round so relaunching resumes where the user left off.
    func persistRound() {
        guard session.total > 0 else {
            defaults.removeObject(forKey: Self.roundKey)
            return
        }
        defaults.set([
            "pool": session.pool,
            "history": session.history,
            "cursor": session.cursor,
            "total": session.total,
            "finished": session.isFinished
        ], forKey: Self.roundKey)
    }

    private func restoreRound() {
        guard let saved = defaults.dictionary(forKey: Self.roundKey),
              let pool = saved["pool"] as? [String],
              let history = saved["history"] as? [String],
              let cursor = saved["cursor"] as? Int,
              let total = saved["total"] as? Int,
              let finished = saved["finished"] as? Bool,
              total == pool.count + history.count,
              (history.isEmpty && cursor == -1)
                || (!history.isEmpty && cursor >= 0 && cursor < history.count)
        else { return }
        session.restore(pool: pool, history: history, cursor: cursor, total: total, isFinished: finished)
    }

    // MARK: - Round

    func startRound() {
        session.restart(characters: allCharacters)
        persistRound()
    }

    /// Reset to a brand-new shuffled round of the current library.
    func reset() { startRound() }

    // MARK: - Mistake book

    /// Record one wrong attempt for `character` and persist.
    func addMistake(_ character: String) {
        mistakes[character, default: 0] += 1
        persistMistakes()
    }

    /// Drop `character` from the mistake book and persist.
    func removeMistake(_ character: String) {
        guard mistakes.removeValue(forKey: character) != nil else { return }
        persistMistakes()
    }

    func clearMistakes() {
        guard !mistakes.isEmpty else { return }
        mistakes.removeAll()
        persistMistakes()
    }

    /// Mistakes ordered by count descending; ties keep library order so the
    /// list is stable between redraws.
    var mistakesSorted: [(char: String, count: Int)] {
        let order = Dictionary(
            allCharacters.enumerated().map { ($1, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        return mistakes
            .map { (char: $0.key, count: $0.value) }
            .sorted { lhs, rhs in
                if lhs.count != rhs.count { return lhs.count > rhs.count }
                return (order[lhs.char] ?? .max) < (order[rhs.char] ?? .max)
            }
    }

    private func persistMistakes() {
        defaults.set(mistakes, forKey: Self.mistakesKey)
    }

    // MARK: - Library

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
        // Make the new characters drawable right away instead of after a relaunch.
        session.appendToPool(toAppend)
        persistRound()
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

    /// Remove a character from the library, the current round and the mistake book.
    /// No-op if it is not in the library.
    func remove(_ character: String) {
        guard let idx = allCharacters.firstIndex(of: character) else { return }
        allCharacters.remove(at: idx)
        persist()
        session.removeCharacter(character)
        if mistakes.removeValue(forKey: character) != nil {
            persistMistakes()
        }
        persistRound()
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
