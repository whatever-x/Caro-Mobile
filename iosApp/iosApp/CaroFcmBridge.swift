import Foundation
import UIKit
import UserNotifications
import FirebaseMessaging
import ComposeApp

final class CaroFcmBridge: NSObject, MessagingDelegate, UNUserNotificationCenterDelegate {

    static let shared = CaroFcmBridge()

    private override init() {
        super.init()
    }

    func attach(application: UIApplication) {
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
    }

    // MARK: MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let token = fcmToken {
            IosMessagingEvents.shared.onTokenRefreshed(token: token)
        }
    }

    // MARK: UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        publish(userInfo: response.notification.request.content.userInfo)
        completionHandler()
    }

    private func publish(userInfo: [AnyHashable: Any]) {
        let deckId = userInfo["deck_id"] as? String
        IosMessagingEvents.shared.onMessageReceived(deckId: deckId)
    }
}
