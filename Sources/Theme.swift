import SwiftUI

/// A self-resetting "✓ done" flag (e.g. "已复制") backed by one `@State Bool`.
/// Flip it on with `.set(_:resetting:)`; it turns itself off after `seconds`,
/// and a rapid re-tap cancels any earlier pending reset so the flag doesn't
/// clear while it's still meant to be shown.
@MainActor
final class TransientFlag: ObservableObject {
    @Published private(set) var isOn = false
    private var pending: DispatchWorkItem?

    func trigger(for seconds: Double = 1.5) {
        isOn = true
        pending?.cancel()
        let work = DispatchWorkItem { [weak self] in self?.isOn = false }
        pending = work
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds, execute: work)
    }
}

/// Shared visual language: warm orange accent, rounded cards, soft shadows.
enum Theme {
    static let accent = Color(red: 1.0, green: 0.62, blue: 0.25)
    static let accentDeep = Color(red: 0.95, green: 0.45, blue: 0.15)
    static let cardBackground = Color(red: 1.0, green: 0.98, blue: 0.94)
    static let softYellow = Color(red: 1.0, green: 0.93, blue: 0.78)
    static let danger = Color(red: 0.82, green: 0.24, blue: 0.18)
    static let dangerSoft = Color(red: 1.0, green: 0.93, blue: 0.90)

    static let cardCornerRadius: CGFloat = 28

    static var cardGradient: LinearGradient {
        LinearGradient(
            colors: [.white, softYellow],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    static var buttonGradient: LinearGradient {
        LinearGradient(
            colors: [accent, accentDeep],
            startPoint: .leading,
            endPoint: .trailing
        )
    }
}

/// Rounded card surface used for the big character and list rows.
struct CardBackground: ViewModifier {
    var cornerRadius: CGFloat = Theme.cardCornerRadius

    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(Theme.cardGradient)
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(Theme.accent.opacity(0.35), lineWidth: 2)
            )
            .shadow(color: Theme.accent.opacity(0.18), radius: 12, x: 0, y: 6)
    }
}

extension View {
    func cardBackground(cornerRadius: CGFloat = Theme.cardCornerRadius) -> some View {
        modifier(CardBackground(cornerRadius: cornerRadius))
    }
}

extension Font {
    /// Bundled 正楷 (LXGW WenKai GB Lite) for displaying hanzi.
    static func hanzi(size: CGFloat) -> Font {
        .custom("LXGWWenKaiGBLite-Regular", size: size)
    }
}

/// Faint 田字格 drawn behind the study character, sized to the shorter side.
struct TianZiGe: View {
    var body: some View {
        GeometryReader { geo in
            let side = max(0, min(geo.size.width, geo.size.height) - 48)
            if side > 0 {
                let square = CGRect(
                    x: (geo.size.width - side) / 2,
                    y: (geo.size.height - side) / 2,
                    width: side,
                    height: side
                )
                Path { path in
                    path.addRect(square)
                    path.move(to: CGPoint(x: square.midX, y: square.minY))
                    path.addLine(to: CGPoint(x: square.midX, y: square.maxY))
                    path.move(to: CGPoint(x: square.minX, y: square.midY))
                    path.addLine(to: CGPoint(x: square.maxX, y: square.midY))
                }
                .stroke(
                    Theme.accent.opacity(0.18),
                    style: StrokeStyle(lineWidth: 1.5, dash: [7, 7])
                )
            }
        }
        .accessibilityHidden(true)
    }
}

/// Filled pill button styled with the warm gradient.
struct PrimaryPillButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title2.bold())
            .foregroundStyle(.white)
            .padding(.horizontal, 28)
            .padding(.vertical, 14)
            .background(
                Capsule().fill(Theme.buttonGradient)
            )
            .shadow(color: Theme.accentDeep.opacity(0.3), radius: 6, x: 0, y: 4)
            .opacity(configuration.isPressed ? 0.75 : 1)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
    }
}

/// Outlined pill button for destructive actions.
struct DangerPillButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title3)
            .foregroundStyle(Theme.danger)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(
                Capsule()
                    .fill(Theme.dangerSoft)
            )
            .overlay(Capsule().strokeBorder(Theme.danger.opacity(0.4), lineWidth: 1.5))
            .opacity(configuration.isPressed ? 0.75 : 1)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
    }
}

/// Outlined pill button for secondary actions.
struct SecondaryPillButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title3)
            .foregroundStyle(Theme.accentDeep)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(
                Capsule()
                    .fill(Theme.softYellow)
            )
            .overlay(Capsule().strokeBorder(Theme.accent.opacity(0.5), lineWidth: 1.5))
            .opacity(configuration.isPressed ? 0.75 : 1)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
    }
}
