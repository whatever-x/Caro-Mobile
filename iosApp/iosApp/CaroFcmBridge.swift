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

    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { _, _ in }
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
        publish(userInfo: response.notification.request.content.userInfo, fallback: response.notification.request.content)
        completionHandler()
    }

    private func publish(userInfo: [AnyHashable: Any], fallback content: UNNotificationContent) {
        let dataMap = userInfo.reduce(into: [String: String]()) { result, entry in
            if let key = entry.key as? String {
                result[key] = "\(entry.value)"
            }
        }
        let messageId = userInfo["gcm.message_id"] as? String
        let sentTime = (userInfo["google.c.sender.id"] as? String).flatMap { _ in nil as Int64? } ?? Int64(Date().timeIntervalSince1970 * 1000)

        IosMessagingEvents.shared.onMessageReceived(
            messageId: messageId,
            title: content.title.isEmpty ? nil : content.title,
            body: content.body.isEmpty ? nil : content.body,
            data: dataMap,
            sentTime: sentTime
        )
    }
}
