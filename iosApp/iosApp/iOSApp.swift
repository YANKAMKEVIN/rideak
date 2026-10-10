import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        IosKoinKt.startRideak()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
