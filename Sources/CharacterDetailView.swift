import SwiftUI

struct CharacterDetailView: View {
    let character: String

    @Environment(\.dismiss) private var dismiss
    private let speech = SpeechService.shared
    @State private var copied = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 40) {
                Spacer()
                ZStack {
                    TianZiGe()
                    Text(character)
                        .font(.hanzi(size: 200))
                        .minimumScaleFactor(0.3)
                }
                .frame(maxWidth: .infinity, minHeight: 300)
                .cardBackground()
                .padding(.horizontal, 24)
                .onTapGesture { speech.speak(character) }

                HStack(spacing: 16) {
                    Button {
                        speech.speak(character)
                    } label: {
                        Label("读一读", systemImage: "speaker.wave.2.fill")
                    }
                    .buttonStyle(SecondaryPillButtonStyle())
                    .accessibilityIdentifier("detailSpeakButton")

                    Button(copied ? "已复制" : "复制") {
                        UIPasteboard.general.string = character
                        copied = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                            copied = false
                        }
                    }
                    .buttonStyle(PrimaryPillButtonStyle())
                    .accessibilityIdentifier("detailCopyButton")
                }
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
