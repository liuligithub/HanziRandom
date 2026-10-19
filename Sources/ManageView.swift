import SwiftUI

struct ManageView: View {
    @EnvironmentObject private var store: CharacterStore
    @Environment(\.dismiss) private var dismiss

    @AppStorage("fontSize") private var fontSize: Double = 150
    @AppStorage("swipeEnabled") private var swipeEnabled: Bool = true
    @State private var inputText: String = ""
    @State private var copiedAll = false
    @State private var detailCharacter: String?
    @State private var toastMessage: String?
    @State private var toastToken = 0
    @FocusState private var inputFocused: Bool

    var body: some View {
        NavigationStack {
            List {
                fontSizeSection
                gestureSection
                roundSection
                addSection
                exportSection
                charactersSection
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.inline)
            .overlay(alignment: .bottom) {
                if let toastMessage {
                    Text(toastMessage)
                        .font(.subheadline.bold())
                        .foregroundStyle(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Theme.accentDeep))
                        .padding(.bottom, 24)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .accessibilityIdentifier("toastMessage")
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
            .sheet(item: Binding(
                get: { detailCharacter.map(IdentifiableString.init) },
                set: { detailCharacter = $0?.value }
            )) { item in
                CharacterDetailView(character: item.value)
            }
        }
    }

    private var roundSection: some View {
        Section {
            Button {
                store.reset()
            } label: {
                Label("重新开始抽卡", systemImage: "shuffle")
            }
        } header: {
            Text("抽卡")
        } footer: {
            Text("用最新的字库重新洗牌开始一轮。新加的字会立即加入还没抽完的这一轮；如果本轮已经抽完，重新开始后生效。")
        }
    }

    private var fontSizeSection: some View {
        Section {
            HStack {
                Spacer()
                Text("永")
                    .font(.hanzi(size: min(fontSize, 72)))
                    .frame(height: 84)
                    .accessibilityHidden(true)
                Spacer()
            }
            Slider(value: $fontSize, in: 60...260) {
                Text("字号")
            } minimumValueLabel: {
                Text("小")
            } maximumValueLabel: {
                Text("大")
            }
        } header: {
            HStack {
                Text("字号")
                Spacer()
                Text("\(Int(fontSize))")
                    .monospacedDigit()
            }
        }
    }

    private var gestureSection: some View {
        Section("手势") {
            Toggle("左右滑动切换", isOn: $swipeEnabled)
        }
    }

    private var addSection: some View {
        Section("添加汉字（可粘贴整段）") {
            TextEditor(text: $inputText)
                .frame(minHeight: 100)
                .focused($inputFocused)
            Button("添加") {
                let hadInput = !inputText.isEmpty
                let before = store.allCharacters.count
                store.addCharacters(from: inputText)
                let added = store.allCharacters.count - before
                inputText = ""
                inputFocused = false
                guard hadInput else { return }
                if added > 0 {
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    showToast("已添加 \(added) 个汉字")
                } else {
                    UINotificationFeedbackGenerator().notificationOccurred(.warning)
                    showToast("没有新增汉字")
                }
            }
            .disabled(inputText.isEmpty)
            .accessibilityIdentifier("addCharactersButton")
        }
    }

    private var exportSection: some View {
        Section("导出全部") {
            Button(copiedAll ? "已复制" : "复制到剪贴板") {
                UIPasteboard.general.string = store.exportString()
                copiedAll = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    copiedAll = false
                }
            }
            ShareLink("存到备忘录", item: store.exportString())
        }
    }

    private var charactersSection: some View {
        Section("字库（\(store.allCharacters.count)）") {
            ForEach(store.allCharacters, id: \.self) { ch in
                HStack {
                    Text(ch).font(.hanzi(size: 22))
                    Spacer()
                    Button {
                        UIPasteboard.general.string = ch
                        showToast("已复制 \(ch)")
                    } label: {
                        Image(systemName: "doc.on.doc")
                    }
                    .buttonStyle(.borderless)
                }
                .contentShape(Rectangle())
                .onTapGesture { detailCharacter = ch }
            }
            .onDelete { offsets in
                offsets.map { store.allCharacters[$0] }.forEach(store.remove)
            }
        }
    }

    private func showToast(_ message: String) {
        toastToken += 1
        let token = toastToken
        withAnimation(.easeOut(duration: 0.2)) { toastMessage = message }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            guard token == toastToken else { return }
            withAnimation(.easeIn(duration: 0.2)) { toastMessage = nil }
        }
    }
}

