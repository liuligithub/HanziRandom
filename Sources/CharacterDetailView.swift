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
