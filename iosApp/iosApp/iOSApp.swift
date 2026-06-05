import SwiftUI
import UIKit
import ComposeApp

class CaroAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosMessagingAttacherKt.attachMessaging(application: application)
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        IosMessagingAttacherKt.applyApnsToken(deviceToken: deviceToken)
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(CaroAppDelegate.self) var appDelegate

    init() {
        FirebaseLifecycleKt.configureFirebaseApp()

        #if DEBUG
            FirebaseLifecycleKt.setAnalyticsCollectionEnabled(enabled: false)
            CrashlyticsLifecycleKt.setCrashlyticsCollectionEnabled(enabled: false)
        #else
            FirebaseLifecycleKt.setAnalyticsCollectionEnabled(enabled: true)
            CrashlyticsLifecycleKt.setCrashlyticsCollectionEnabled(enabled: true)
        #endif

        KoinKt.doInitKoin()
        NapierInitializerKt.doInitNapier()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = GoogleSignInUrlHandlerKt.handleGoogleSignInOpenURL(url: url)
                }
        }
    }
}
