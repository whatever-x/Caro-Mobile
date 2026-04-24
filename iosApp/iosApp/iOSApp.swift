import SwiftUI
import ComposeApp
import GoogleSignIn
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

@main
struct iOSApp: App {
    
    init() {
        FirebaseApp.configure()
        
        #if DEBUG
            Analytics.setAnalyticsCollectionEnabled(false)
            Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif
        
        KoinKt.doInitKoin()
        NapierInitializerKt.doInitNapier()
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
