import AVFoundation

/// Long-lived shared instance. AVSpeechSynthesizer must outlive the `speak` call —
/// a per-View instance gets deallocated as soon as the View struct is torn down,
/// which kills the utterance before it starts.
@MainActor
final class SpeechService: NSObject, AVSpeechSynthesizerDelegate {
    static let shared = SpeechService()
    private let synthesizer: AVSpeechSynthesizer

    private override init() {
        // Default category is .soloAmbient, which the Ring/Silent switch mutes on
        // the built-in speaker (headphones are unaffected, which hides the cause).
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
        synthesizer = AVSpeechSynthesizer()
        super.init()
        synthesizer.delegate = self
    }

    func speak(_ text: String) {
        guard !text.isEmpty else { return }
        // Stop any in-flight utterance so rapid taps don't queue/overlap.
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        try? AVAudioSession.sharedInstance().setActive(true)
        let utterance = AVSpeechUtterance(string: text)
        // May be nil if the zh-CN voice is unavailable; AVSpeechUtterance tolerates a
        // nil voice (falls back to default) — spec requires silent degradation, not a crash.
        utterance.voice = AVSpeechSynthesisVoice(language: "zh-CN")
        synthesizer.speak(utterance)
    }

    // Release the session so speech doesn't keep other apps' audio interrupted.
    // AVSpeechSynthesizer may invoke delegate callbacks off the main thread, so
    // hop back to the main actor before touching our state.
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in deactivateAudioSessionIfIdle() }
    }

    // Stopping a previous utterance also ends via didCancel, not didFinish.
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        Task { @MainActor in deactivateAudioSessionIfIdle() }
    }

    private func deactivateAudioSessionIfIdle() {
        // A rapid re-tap cancels the old utterance right after the new one starts;
        // don't tear the session down while the new utterance is still speaking.
        guard !synthesizer.isSpeaking else { return }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}
