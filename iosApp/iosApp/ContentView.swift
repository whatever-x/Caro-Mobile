import UIKit
import SwiftUI
import StoreKit
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController { controller in
            MainActor.assumeIsolated {
                if let scene = controller.viewIfLoaded?.window?.windowScene,
                   scene.activationState == .foregroundActive {
                    AppStore.requestReview(in: scene)
                }
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
