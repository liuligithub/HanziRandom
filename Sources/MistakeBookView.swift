import SwiftUI

struct MistakeBookView: View {
    @EnvironmentObject private var store: CharacterStore

    @State private var reviewCharacters: [String]?
    @State private var detailCharacter: String?
    @State private var settingsPresented = false
    @State private var showClearConfirmation = false

    private var entries: [(char: String, count: Int)] { store.mistakesSorted }

    var body: some View {
        Group {
            if entries.isEmpty {
                emptyState
            } else {
                List {
                    Section {
                        Button {
                            startReview()
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "play.circle.fill")
                                Text("开始复习（\(entries.count) 个）")
                                    .font(.headline)
                            }
                            .foregroundStyle(Theme.accentDeep)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                        }
                        .accessibilityIdentifier("reviewMistakesButton")
                    }
                    ForEach(entries, id: \.char) { entry in
                        row(for: entry)
                    }
                    .onDelete { offsets in
                        let targets = offsets.map { entries[$0].char }
                        targets.forEach(store.removeMistake)
                    }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
                .background(Theme.softYellow.opacity(0.25).ignoresSafeArea())
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                if !entries.isEmpty {
                    Button {
                        startReview()
                    } label: {
                        Label("复习错题", systemImage: "play.circle.fill")
                            .foregroundStyle(Theme.accentDeep)
                    }
                    .accessibilityIdentifier("reviewMistakesToolbarButton")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                if !entries.isEmpty {
                    Button {
                        showClearConfirmation = true
                    } label: {
                        Image(systemName: "trash")
                            .foregroundStyle(Theme.accentDeep)
                    }
                    .accessibilityIdentifier("clearMistakesButton")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    settingsPresented = true
                } label: {
                    Image(systemName: "gearshape.fill")
                        .foregroundStyle(Theme.accentDeep)
                }
                .accessibilityIdentifier("settingsButton")
            }
        }
        .confirmationDialog("清空错题本？", isPresented: $showClearConfirmation, titleVisibility: .visible) {
            Button("清空", role: .destructive) { store.clearMistakes() }
            Button("取消", role: .cancel) {}
        } message: {
            Text("所有错题记录将被删除，且无法恢复。")
        }
        .sheet(isPresented: $settingsPresented) { ManageView() }
        .sheet(item: Binding(
            get: { detailCharacter.map(IdentifiableString.init) },
            set: { detailCharacter = $0?.value }
        )) { item in
            CharacterDetailView(character: item.value)
        }
        .fullScreenCover(item: Binding(
            get: { reviewCharacters.map(ReviewPayload.init) },
            set: { reviewCharacters = $0?.characters }
        )) { payload in
            MistakeReviewView(characters: payload.characters)
        }
    }

    private func startReview() {
        reviewCharacters = entries.map(\.char)
    }

    private func row(for entry: (char: String, count: Int)) -> some View {
        HStack(spacing: 16) {
            Text(entry.char)
                .font(.hanzi(size: 48))
                .frame(width: 72, height: 72)
                .cardBackground(cornerRadius: 18)

            Text("错 \(entry.count) 次")
                .font(.subheadline.bold())
                .foregroundStyle(Theme.accentDeep)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Capsule().fill(Theme.softYellow))

            Spacer()

            Button(role: .destructive) {
                store.removeMistake(entry.char)
            } label: {
                Image(systemName: "trash")
                    .foregroundStyle(Theme.accentDeep)
            }
            .buttonStyle(.borderless)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture { detailCharacter = entry.char }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "star.slash")
                .font(.system(size: 56))
                .foregroundStyle(Theme.accent.opacity(0.6))
            Text("还没有错题")
                .font(.title2.bold())
                .foregroundStyle(Theme.accentDeep)
            Text("学习时点「加入错题本」\n错得多的会排在前面")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Theme.softYellow.opacity(0.25).ignoresSafeArea())
    }
}

/// Wrapper so a plain String can drive `.sheet(item:)`.
struct IdentifiableString: Identifiable {
    let value: String
    var id: String { value }
}

private struct ReviewPayload: Identifiable {
    let characters: [String]
    var id: String { characters.joined(separator: "|") }
}
