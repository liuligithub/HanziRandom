import SwiftUI

struct DrawView: View {
    @EnvironmentObject private var store: CharacterStore
    private let speech = SpeechService()

    // Font size preference persisted automatically via @AppStorage.
    @AppStorage("fontSize") private var fontSize: Double = 120
    @State private var didStart = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 32) {
                progressHeader
                Spacer()
                characterArea
                Spacer()
                fontSlider
                bottomControls
            }
            .padding()
            .navigationTitle("随机汉字")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink("管理") { ManageView() }
                }
            }
            .onAppear {
                if !didStart {
                    store.startRound()
                    didStart = true
                }
            }
        }
    }

    @ViewBuilder private var progressHeader: some View {
        if store.total > 0 {
            Text("第 \(store.drawnCount) / 共 \(store.total)")
                .font(.headline)
                .foregroundStyle(.secondary)
        }
    }

    @ViewBuilder private var characterArea: some View {
        if store.allCharacters.isEmpty {
            Text("请先在管理页添加汉字")
                .font(.title3)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        } else if let current = store.current {
            Text(current)
                .font(.system(size: fontSize))
                .onTapGesture { speech.speak(current) }
                .contextMenu {
                    Button("复制") { UIPasteboard.general.string = current }
                }
        }
    }

    @ViewBuilder private var fontSlider: some View {
        if store.current != nil {
            Slider(value: $fontSize, in: 60...260) {
                Text("字号")
            }
        }
    }

    @ViewBuilder private var bottomControls: some View {
        if store.allCharacters.isEmpty {
            Button("下一个") {}.buttonStyle(.borderedProminent).disabled(true)
        } else if store.isFinished {
            VStack(spacing: 16) {
                Text("全部抽完").font(.title2).bold()
                Button("重置") { store.reset() }
                    .buttonStyle(.borderedProminent)
            }
        } else {
            Button("下一个") { store.drawNext() }
                .buttonStyle(.borderedProminent)
                .font(.title2)
        }
    }
}
