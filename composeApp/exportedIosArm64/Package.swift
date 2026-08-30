
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CaroNativeDependencies",
    platforms: [.iOS("15.0"),.macOS("10.13"),.tvOS("12.0"),.watchOS("4.0")],
    products: [
        .library(
            name: "CaroNativeDependencies",
            type: .static,
            targets: ["CaroNativeDependencies"])
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk", exact: "12.14.0"),.package(url: "https://github.com/google/GoogleSignIn-iOS", exact: "9.1.0")
    ],
    targets: [
        .target(
            name: "CaroNativeDependencies",
            dependencies: [
                .product(name: "FirebaseCore", package: "firebase-ios-sdk"),.product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"),.product(name: "FirebaseCrashlytics", package: "firebase-ios-sdk"),.product(name: "FirebaseMessaging", package: "firebase-ios-sdk"),.product(name: "GoogleSignIn", package: "GoogleSignIn-iOS")
            ],
            path: "Sources"
            
            
        )
        
    ]
)
        