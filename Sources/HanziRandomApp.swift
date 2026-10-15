import SwiftUI

@main
struct HanziRandomApp: App {
    @StateObject private var store = CharacterStore()

    var body: some Scene {
        WindowGroup {
            DrawView()
                .environmentObject(store)
        }
    }
}
