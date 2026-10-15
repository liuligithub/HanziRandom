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
