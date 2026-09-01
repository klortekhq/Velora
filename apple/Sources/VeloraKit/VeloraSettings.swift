import Foundation

public enum VeloraLanguage: String, CaseIterable, Identifiable, Sendable {
    case spanish = "es"
    case english = "en"
    case french = "fr"
    case german = "de"

    public var id: String { rawValue }
}

public enum SubtitlePreference: String, Codable, Sendable {
    case off
    case preferred
    case forced
    case automatic
}

public enum PerformanceMode: String, Codable, Sendable {
    case automatic
    case quality
    case balanced
    case performance
}

/// Device-local preferences. They are deliberately separate from Jellyfin
/// account data so an Apple TV cannot overwrite an iPhone's offline settings.
public struct VeloraSettings: Codable, Equatable, Sendable {
    public var languageIdentifier: String?
    public var preferredAudioLanguage: String?
    public var subtitlePreference: SubtitlePreference
    public var preferredSubtitleLanguage: String?
    public var performanceMode: PerformanceMode
    public var themeMusicEnabled: Bool
    public var themeMusicVolume: Double

    public init(
        languageIdentifier: String? = nil,
        preferredAudioLanguage: String? = nil,
        subtitlePreference: SubtitlePreference = .automatic,
        preferredSubtitleLanguage: String? = nil,
        performanceMode: PerformanceMode = .automatic,
        themeMusicEnabled: Bool = false,
        themeMusicVolume: Double = 0.7
    ) {
        self.languageIdentifier = languageIdentifier
        self.preferredAudioLanguage = preferredAudioLanguage
        self.subtitlePreference = subtitlePreference
        self.preferredSubtitleLanguage = preferredSubtitleLanguage
        self.performanceMode = performanceMode
        self.themeMusicEnabled = themeMusicEnabled
        self.themeMusicVolume = min(max(themeMusicVolume, 0), 1)
    }

    public static func systemDefault() -> Self {
        Self(languageIdentifier: Locale.preferredLanguages.first)
    }

    /// The locale used by SwiftUI. A nil choice follows the device locale.
    public var appLocale: Locale {
        Locale(identifier: languageIdentifier ?? Locale.preferredLanguages.first ?? "en")
    }
}

/// Stores device-local preferences without coupling them to the Jellyfin
/// account or to another platform. The injected defaults/key make the store
/// deterministic in tests and allow an app target to provide its own scope.
public final class VeloraSettingsStore: @unchecked Sendable {
    private let defaults: UserDefaults
    private let key: String

    public init(defaults: UserDefaults = .standard, key: String = "velora.settings") {
        self.defaults = defaults
        self.key = key
    }

    public func load() -> VeloraSettings? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(VeloraSettings.self, from: data)
    }

    public func save(_ settings: VeloraSettings) {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: key)
    }

    public func remove() {
        defaults.removeObject(forKey: key)
    }
}

public struct JellyfinSession: Codable, Equatable, Sendable {
    public let accessToken: String
    public let userID: String
    public let username: String?

    public init(accessToken: String, userID: String, username: String? = nil) {
        self.accessToken = accessToken
        self.userID = userID
        self.username = username
    }
}
