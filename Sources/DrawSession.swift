import Foundation

/// A single shuffled drawing round over a fixed set of characters.
/// Kept separate from `CharacterStore` so the mistake-review screen can run
/// its own round without disturbing the learning round.
final class DrawSession: ObservableObject {
    @Published private(set) var pool: [String] = []
    @Published private(set) var current: String?
    @Published private(set) var total: Int = 0
    @Published private(set) var drawnCount: Int = 0
    @Published private(set) var isFinished: Bool = false

    /// Characters drawn so far this round, in order, so the user can swipe back.
    @Published private(set) var history: [String] = []
    /// Index into `history` of the character currently shown; -1 when empty.
    @Published private(set) var cursor: Int = -1

    private var characters: [String]

    init(characters: [String] = []) {
        self.characters = characters
        restart()
    }

    /// Begin a fresh round over `characters` (or the previously supplied set):
    /// shuffle into the pool and draw the first.
    func restart(characters newCharacters: [String]? = nil) {
        if let newCharacters { characters = newCharacters }
        total = characters.count
        drawnCount = 0
        current = nil
        isFinished = false
        history = []
        cursor = -1
        pool = characters.shuffled()
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
        let drawn = pool.remove(at: index)
        current = drawn
        history.append(drawn)
        cursor = history.count - 1
        drawnCount += 1
        if pool.isEmpty {
            isFinished = true
        }
    }

    /// Append newly added characters so the ongoing round can still draw them.
    func appendToPool(_ newCharacters: [String]) {
        guard !isFinished, !newCharacters.isEmpty else { return }
        pool.append(contentsOf: newCharacters)
        total += newCharacters.count
    }

    /// Remove a character from this round entirely (e.g. it was deleted from the
    /// library). If it is currently shown, behaves like `removeCurrent`.
    func removeCharacter(_ character: String) {
        if let index = history.firstIndex(of: character) {
            if index == cursor {
                removeCurrent()
                return
            }
            history.remove(at: index)
            if index < cursor { cursor -= 1 }
            total = max(0, total - 1)
            drawnCount = history.count
            isFinished = pool.isEmpty
            return
        }
        guard pool.contains(character) else { return }
        pool.removeAll { $0 == character }
        total = max(0, total - 1)
        isFinished = pool.isEmpty
    }

    /// Restore a persisted round. The caller is responsible for validation.
    func restore(pool: [String], history: [String], cursor: Int, total: Int, isFinished: Bool) {
        self.pool = pool
        self.history = history
        self.total = total
        self.drawnCount = history.count
        self.cursor = cursor
        self.isFinished = isFinished
        current = (0..<history.count).contains(cursor) ? history[cursor] : nil
    }

    /// Drop the character currently shown from this round entirely (history + pool)
    /// and move on to the next available one, finishing if nothing is left.
    func removeCurrent() {
        guard let shown = current, cursor >= 0 else { return }

        pool.removeAll { $0 == shown }
        history.remove(at: cursor)
        total = max(0, total - 1)
        drawnCount = max(0, drawnCount - 1)

        if cursor < history.count {
            // A later already-seen character slides into this slot.
            current = history[cursor]
        } else if !pool.isEmpty {
            drawNext()
        } else {
            current = nil
            cursor = history.count - 1
            isFinished = true
        }
    }

    /// True when there is an earlier character in this round's history to go back to.
    var canGoPrevious: Bool { cursor > 0 }

    /// True when `goNext` can still advance: either forward through already-seen
    /// history, or by drawing a new character from the pool.
    var canGoNext: Bool { cursor < history.count - 1 || !isFinished }

    /// 1-based index of the character currently shown, for the progress header.
    var position: Int { cursor + 1 }

    /// Step back to the previously shown character. No-op at the start of the history.
    func goPrevious() {
        guard cursor > 0 else { return }
        cursor -= 1
        current = history[cursor]
    }

    /// Advance to the next character. If we've stepped back into history, move forward
    /// through the already-seen characters; otherwise draw a new one from the pool.
    func goNext() {
        if cursor < history.count - 1 {
            cursor += 1
            current = history[cursor]
        } else {
            drawNext()
        }
    }
}
