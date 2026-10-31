import SwiftUI

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        BackgroundRefresh.register()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
        .onChange(of: scenePhase) { phase in
            if phase == .background {
                BackgroundRefresh.schedule()
            }
        }
    }
}
