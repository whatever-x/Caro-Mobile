import SwiftUI
import UIKit
import ComposeApp
import GoogleSignIn
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

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
