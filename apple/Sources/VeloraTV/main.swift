#if os(tvOS) && canImport(SwiftUI) && canImport(AVKit)
import SwiftUI
import VeloraKit

@available(tvOS 16.0, *)
@main
struct VeloraTVApp: App {
    var body: some Scene {
        WindowGroup { VeloraTVRoot() }
    }
}

@available(tvOS 16.0, *)
private struct VeloraTVRoot: View {
    var body: some View {
        if let shell = try? VeloraAppShell(platform: .tvOS) {
            shell
        } else {
            Text("Connect to Jellyfin", bundle: .module)
        }
    }
}
#endif

#if os(macOS)
import Foundation

/// Package-build entrypoint for macOS CI. tvOS keeps its native @main above.
@main
struct VeloraTVPackageCheck {
    static func main() {}
}
#endif
