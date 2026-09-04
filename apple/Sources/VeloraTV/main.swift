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
        if let serverURL = URL(string: "http://jellyfin.local:8096"),
           let shell = try? VeloraAppShell(platform: .tvOS, serverURL: serverURL) {
            shell
        } else {
            Text("Connect to Jellyfin", bundle: .module)
        }
    }
}
#endif
