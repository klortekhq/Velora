// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "VeloraApple",
    defaultLocalization: "en",
    platforms: [.macOS(.v13), .iOS(.v16), .tvOS(.v16)],
    products: [
        .library(name: "VeloraKit", targets: ["VeloraKit"]),
        .executable(name: "VeloraMobile", targets: ["VeloraMobile"]),
        .executable(name: "VeloraTV", targets: ["VeloraTV"])
    ],
    targets: [
        .target(name: "VeloraKit", resources: [.process("Resources")]),
        .executableTarget(name: "VeloraMobile", dependencies: ["VeloraKit"]),
        .executableTarget(name: "VeloraTV", dependencies: ["VeloraKit"]),
        .testTarget(name: "VeloraKitTests", dependencies: ["VeloraKit"])
    ]
)
