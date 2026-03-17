import Foundation
import UIKit
import GoogleSignIn
import GoogleSignInSwift

@objc(GoogleLoginBridge) // 명시적으로 이름 고정
@objcMembers public class GoogleLoginBridge : NSObject {
    public func request(
        success : @escaping (NSString) -> Void, // String -> NSString으로 변경
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
            guard let result = signInResult else {
                failure()
                return
            }
            // tokenString을 NSString으로 캐스팅해서 전달
            let token = (result.user.idToken?.tokenString ?? "") as NSString
            success(token)
        }
    }
}
