import AVFoundation
import XCTest
@testable import HanziRandom

final class SpeechServiceTests: XCTestCase {
    /// The default .soloAmbient category is muted by the Ring/Silent switch; speech
    /// must use .playback so tapping 读一读 is audible through the speaker.
    @MainActor
    func test_speak_usesPlaybackCategory() {
        SpeechService.shared.speak("欢")
        XCTAssertEqual(AVAudioSession.sharedInstance().category, .playback)
    }
}
