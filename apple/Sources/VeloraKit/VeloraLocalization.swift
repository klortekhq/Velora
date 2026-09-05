import Foundation

/// Localized copy that can be consumed by the executable SwiftPM entrypoints.
/// The resources belong to VeloraKit, so executable targets must not reference
/// their own `Bundle.module`.
public enum VeloraLocalized {
    public static var connectToJellyfin: String {
        String(localized: "Connect to Jellyfin", bundle: .module)
    }
}
