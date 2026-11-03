import UIKit
import SwiftUI
import StoreKit
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController { controller in
            MainActor.assumeIsolated {
                guard let scene = controller.viewIfLoaded?.window?.windowScene,
                      scene.activationState == .foregroundActive else {
                    return KotlinBoolean(bool: false)
                }
                AppStore.requestReview(in: scene)
                return KotlinBoolean(bool: true)
            }
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
