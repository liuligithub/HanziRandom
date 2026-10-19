import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var store: CharacterStore

    var body: some View {
        TabView {
            NavigationStack {
                StudyView(session: store.session, mode: .learning)
                    .navigationTitle("随机汉字")
            }
            .tabItem { Label("学习", systemImage: "graduationcap.fill") }

            NavigationStack {
                MistakeBookView()
                    .navigationTitle("错题本")
            }
            .tabItem { Label("错题本", systemImage: "star.fill") }
        }
        .tint(Theme.accentDeep)
    }
}
