import Foundation

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
