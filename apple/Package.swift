// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "VeloraApple",
    platforms: [.macOS(.v12), .iOS(.v16), .tvOS(.v16)],
    products: [.library(name: "VeloraKit", targets: ["VeloraKit"])],
    targets: [
        .target(name: "VeloraKit"),
        .testTarget(name: "VeloraKitTests", dependencies: ["VeloraKit"])
    ]
)
