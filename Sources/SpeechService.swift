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
