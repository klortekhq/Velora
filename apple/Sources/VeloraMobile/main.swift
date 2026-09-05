#if os(iOS) && canImport(SwiftUI) && canImport(AVKit) && canImport(UIKit)
import SwiftUI
import UIKit
import VeloraKit

@available(iOS 16.0, *)
@main
struct VeloraMobileApp: App {
    var body: some Scene {
        WindowGroup { VeloraMobileRoot() }
    }
}

@available(iOS 16.0, *)
private struct VeloraMobileRoot: View {
    var body: some View {
        if let shell = try? VeloraAppShell(
            platform: UIDevice.current.userInterfaceIdiom == .pad ? .iPad : .iPhone
        ) {
            shell
        } else {
            Text(VeloraLocalized.connectToJellyfin)
        }
    }
}
#endif

#if os(macOS)
import Foundation

/// Package-build entrypoint for macOS CI. The shipped clients remain iOS/iPadOS
/// and tvOS apps; this keeps the shared Swift package linkable on the runner.
@main
struct VeloraMobilePackageCheck {
    static func main() {}
}
#endif
