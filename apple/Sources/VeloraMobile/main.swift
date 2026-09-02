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
        if let serverURL = URL(string: "http://127.0.0.1:8096"),
           let shell = try? VeloraAppShell(
               platform: UIDevice.current.userInterfaceIdiom == .pad ? .iPad : .iPhone,
               serverURL: serverURL
           ) {
            shell
        } else {
            Text("Velora")
        }
    }
}
#endif
