import Foundation
import UIKit
import GoogleSignIn
import GoogleSignInSwift

@objc(GoogleLoginBridge)
@objcMembers public class GoogleLoginBridge : NSObject {
    public func request(
        success : @escaping (NSString) -> Void,
        failure : @escaping () -> Void,
        cancelled : @escaping () -> Void
    ) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
            failure()
            return
        }
        
        guard let rootViewController = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            failure()
            return
        }
    
        GIDSignIn.sharedInstance.signIn(
            withPresenting: rootViewController
        ) { signInResult, error in
            if let error = error as NSError? {
                if error.code == GIDSignInError.canceled.rawValue {
                    cancelled()
                    return
                }
                failure()
                return
            }
            guard let result = signInResult else {
                failure()
                return
            }
            guard let tokenString = result.user.idToken?.tokenString else {
                failure()
                return
            }
            success(tokenString as NSString)
        }
    }
}
