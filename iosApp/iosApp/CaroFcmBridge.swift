import Foundation
import UIKit
import UserNotifications
import FirebaseMessaging
import ComposeApp

final class CaroFcmBridge: NSObject, IosMessagingBridge, MessagingDelegate, UNUserNotificationCenterDelegate {

    static let shared = CaroFcmBridge()

    private override init() {
        super.init()
    }

    func attach(application: UIApplication) {
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()

        IosMessagingBridgeHolderKt.registerIosMessagingBridge(bridge: self)
    }

    // MARK: IosMessagingBridge

    func fetchToken(callback: @escaping (String?, String?) -> Void) {
        Messaging.messaging().token { token, error in
            if let error = error {
                callback(nil, error.localizedDescription)
            } else {
                callback(token, nil)
            }
        }
    }

    func deleteToken(callback: @escaping (String?) -> Void) {
        Messaging.messaging().deleteToken { error in
            callback(error?.localizedDescription)
        }
    }

    func subscribe(topic: String, callback: @escaping (String?) -> Void) {
        Messaging.messaging().subscribe(toTopic: topic) { error in
            callback(error?.localizedDescription)
        }
    }

    func unsubscribe(topic: String, callback: @escaping (String?) -> Void) {
        Messaging.messaging().unsubscribe(fromTopic: topic) { error in
            callback(error?.localizedDescription)
        }
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
