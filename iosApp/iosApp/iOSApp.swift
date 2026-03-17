import SwiftUI
import ComposeApp
import GoogleSignIn

@main
struct iOSApp: App {
    
    init() {
        KoinKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
