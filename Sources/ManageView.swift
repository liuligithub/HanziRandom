import SwiftUI

struct ManageView: View {
    @EnvironmentObject private var store: CharacterStore

    @State private var inputText: String = ""
    @State private var copiedAll = false
    @State private var detailCharacter: String?

    var body: some View {
        List {
            addSection
            exportSection
            charactersSection
        }
        .navigationTitle("管理汉字")
        .sheet(item: Binding(
            get: { detailCharacter.map(IdentifiableString.init) },
            set: { detailCharacter = $0?.value }
        )) { item in
            CharacterDetailView(character: item.value)
        }
    }

    private var addSection: some View {
        Section("添加汉字（可粘贴整段）") {
            TextEditor(text: $inputText)
                .frame(minHeight: 100)
            Button("添加") {
                store.addCharacters(from: inputText)
                inputText = ""
            }
            .disabled(inputText.isEmpty)
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
                    Text(ch).font(.title2)
                    Spacer()
                    Button {
                        UIPasteboard.general.string = ch
                    } label: {
                        Image(systemName: "doc.on.doc")
                    }
                    .buttonStyle(.borderless)
                }
                .contentShape(Rectangle())
                .onTapGesture { detailCharacter = ch }
            }
            .onDelete { offsets in
                for index in offsets {
                    store.remove(store.allCharacters[index])
                }
            }
        }
    }
}

/// Wrapper so a plain String can drive `.sheet(item:)`.
private struct IdentifiableString: Identifiable {
    let value: String
    var id: String { value }
}
