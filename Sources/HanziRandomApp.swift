import SwiftUI

@main
struct HanziRandomApp: App {
    @StateObject private var store = CharacterStore()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(store)
                .onChange(of: scenePhase) { phase in
                    if phase == .background {
                        store.persistRound()
                    }
                }
        }
    }
}
