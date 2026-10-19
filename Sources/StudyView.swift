import SwiftUI

/// The drawing screen, used both for learning (whole library) and for
/// mistake review (a fresh session over the wrong characters).
struct StudyView: View {
    enum Mode {
        case learning
        case review
    }

    @EnvironmentObject private var store: CharacterStore
    @ObservedObject var session: DrawSession
    let mode: Mode

    // Font size preference persisted automatically via @AppStorage.
    @AppStorage("fontSize") private var fontSize: Double = 150
    // Whether swipe-to-navigate is enabled; toggled from the 设置 page.
    @AppStorage("swipeEnabled") private var swipeEnabled: Bool = true

    @State private var didStart = false
    @State private var justAddedMistake = false

    private let speech = SpeechService.shared

    var body: some View {
        VStack(spacing: 24) {
            progressHeader
            Spacer(minLength: 0)
            characterCard
            actionButtons
            Spacer(minLength: 0)
            bottomControls
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
        .background(Theme.softYellow.opacity(0.25).ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $settingsPresented) { ManageView() }
        .toolbar {
            if mode == .learning {
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
        }
        .onAppear {
            if !didStart {
                if mode == .learning && session.total == 0 {
                    store.startRound()
                }
                didStart = true
            }
        }
    }

    @State private var settingsPresented = false

    private var current: String? { session.current }

    @ViewBuilder private var finishedCard: some View {
        VStack(spacing: 16) {
            if mode == .review {
                Text("🎉")
                    .font(.system(size: 64))
                Text("全部复习完啦")
                    .font(.title2.bold())
                    .foregroundStyle(Theme.accentDeep)
            } else {
                Text("请先在设置里添加汉字")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                Button("去添加汉字") { settingsPresented = true }
                    .buttonStyle(PrimaryPillButtonStyle())
                    .accessibilityIdentifier("openSettingsButton")
            }
        }
        .multilineTextAlignment(.center)
        .padding(40)
        .frame(maxWidth: .infinity, minHeight: 260)
        .cardBackground()
    }

    @ViewBuilder private var progressHeader: some View {
        if session.total > 0 {
            VStack(spacing: 8) {
                Text("第 \(session.position) / 共 \(session.total)")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.accentDeep.opacity(0.8))
                    .accessibilityIdentifier("progressLabel")

                ProgressView(
                    value: Double(session.drawnCount),
                    total: Double(max(session.total, 1))
                )
                .tint(Theme.accentDeep)
                .frame(maxWidth: 240)
                .accessibilityIdentifier("progressBar")
            }
        }
    }

    @ViewBuilder private var characterCard: some View {
        if session.current == nil && session.isFinished {
            finishedCard
        } else if let current {
            ZStack {
                TianZiGe()
                Text(current)
                    .font(.hanzi(size: fontSize))
                    .minimumScaleFactor(0.3)
                    .accessibilityIdentifier("characterLabel")
            }
                .frame(maxWidth: .infinity, minHeight: 320)
                .cardBackground()
                .id(current)
                .transition(.asymmetric(
                    insertion: .scale(scale: 0.85).combined(with: .opacity),
                    removal: .opacity
                ))
                .contentShape(Rectangle())
                .onTapGesture { advance() }
                .contextMenu {
                    Button("复制") { UIPasteboard.general.string = current }
                }
                .gesture(
                    DragGesture(minimumDistance: 30)
                        .onEnded { value in
                            guard abs(value.translation.width) > abs(value.translation.height) else { return }
                            if value.translation.width < -40 {
                                advance()        // swipe left → next
                            } else if value.translation.width > 40 {
                                back()           // swipe right → previous
                            }
                        },
                    including: swipeEnabled ? .all : .none
                )
        }
    }

    @ViewBuilder private var actionButtons: some View {
        if let current {
            VStack(spacing: 12) {
                HStack(spacing: 14) {
                    Button {
                        speech.speak(current)
                    } label: {
                        Label("读一读", systemImage: "speaker.wave.2.fill")
                    }
                    .buttonStyle(SecondaryPillButtonStyle())
                    .accessibilityIdentifier("speakButton")

                    Button {
                        store.addMistake(current)
                        UINotificationFeedbackGenerator().notificationOccurred(.success)
                        flashMistakeAdded()
                    } label: {
                        Label(justAddedMistake ? "已加入 ✓" : "加入错题本",
                              systemImage: justAddedMistake ? "checkmark.circle.fill" : "star")
                    }
                    .buttonStyle(SecondaryPillButtonStyle())
                    .accessibilityIdentifier("addMistakeButton")
                }

                if mode == .review {
                    Button(role: .destructive) {
                        store.removeMistake(current)
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        withAnimation(.easeOut(duration: 0.2)) { session.removeCurrent() }
                    } label: {
                        Label("删除错题", systemImage: "trash")
                    }
                    .buttonStyle(DangerPillButtonStyle())
                    .accessibilityIdentifier("deleteMistakeButton")
                }
            }
        }
    }

    @ViewBuilder private var bottomControls: some View {
        if session.total > 0 {
            let showReset = session.isFinished && mode == .learning && !session.canGoNext
            VStack(spacing: 12) {
                if showReset {
                    Text("全部抽完").font(.title3.bold()).foregroundStyle(Theme.accentDeep)
                }
                HStack(spacing: 16) {
                    Button("上一个") { back() }
                        .buttonStyle(SecondaryPillButtonStyle())
                        .disabled(!session.canGoPrevious)
                        .opacity(session.canGoPrevious ? 1 : 0.4)
                        .accessibilityIdentifier("previousButton")

                    if showReset {
                        Button("重置") { store.reset() }
                            .buttonStyle(PrimaryPillButtonStyle())
                            .accessibilityIdentifier("resetButton")
                    } else {
                        Button("下一个") { advance() }
                            .buttonStyle(PrimaryPillButtonStyle())
                            .disabled(!session.canGoNext)
                            .opacity(session.canGoNext ? 1 : 0.4)
                            .accessibilityIdentifier("nextButton")
                    }
                }
            }
        }
    }

    private func flashMistakeAdded() {
        justAddedMistake = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            justAddedMistake = false
        }
    }

    /// Advance with a light tap feedback + animated card swap.
    private func advance() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        withAnimation(.easeOut(duration: 0.2)) { session.goNext() }
    }

    private func back() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        withAnimation(.easeOut(duration: 0.2)) { session.goPrevious() }
    }
}

/// Review wrapper: owns its own session so the learning round stays untouched.
struct MistakeReviewView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var session: DrawSession

    init(characters: [String]) {
        _session = StateObject(wrappedValue: DrawSession(characters: characters))
    }

    var body: some View {
        NavigationStack {
            StudyView(session: session, mode: .review)
                .navigationTitle("错题复习")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("关闭") { dismiss() }
                    }
                }
        }
    }
}
