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
            Text("Connect to Jellyfin", bundle: .module)
        }
    }
}
#endif
