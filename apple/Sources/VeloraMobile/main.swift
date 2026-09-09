#if os(iOS) && canImport(SwiftUI) && canImport(AVKit) && canImport(UIKit)
import SwiftUI
import UIKit
import VeloraKit

@available(iOS 16.0, *)
@main
struct VeloraMobileApp: App {
    @UIApplicationDelegateAdaptor(VeloraMobileDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup { VeloraMobileRoot() }
    }
}

@available(iOS 16.0, *)
private final class VeloraMobileDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        guard identifier == "com.klortek.velora.offline" else {
            completionHandler()
            return
        }
        VeloraOfflineTransferCoordinator.shared.setBackgroundCompletionHandler(completionHandler)
    }
}

@available(iOS 16.0, *)
private struct VeloraMobileRoot: View {
    @State private var shell: VeloraAppShell?

    init() {
        _shell = State(initialValue: try? VeloraAppShell(
            platform: UIDevice.current.userInterfaceIdiom == .pad ? .iPad : .iPhone
        ))
    }

    var body: some View {
        if let shell {
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
