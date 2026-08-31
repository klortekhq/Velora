import Foundation

public enum VeloraPlatform: Sendable {
    case iPhone, iPad, tvOS

    public var supportsOfflineDownloads: Bool {
        switch self {
        case .iPhone, .iPad: return true
        case .tvOS: return false
        }
    }
}

public struct JellyfinItem: Codable, Identifiable, Sendable {
    public let id: String
    public let name: String
    public let type: String?
    public let overview: String?
    public let imageTags: [String: String]?

    public init(id: String, name: String, type: String? = nil, overview: String? = nil, imageTags: [String: String]? = nil) {
        self.id = id
        self.name = name
        self.type = type
        self.overview = overview
        self.imageTags = imageTags
    }
}

public struct JellyfinResult<T: Decodable & Sendable>: Decodable, Sendable {
    public let items: [T]
    public let totalRecordCount: Int?

    enum CodingKeys: String, CodingKey {
        case items = "Items"
        case totalRecordCount = "TotalRecordCount"
    }
}

public enum PlaybackPath: String, Sendable { case directPlay, directStream, remux, transcode, fallback }

public struct PlaybackCapabilities: Sendable {
    public var videoCodecs: Set<String>
    public var audioCodecs: Set<String>
    public var containers: Set<String>
    public var hdrFormats: Set<String>
    public var directPlay: Bool
    public var directStream: Bool
    public var remux: Bool

    public init(videoCodecs: Set<String> = [], audioCodecs: Set<String> = [], containers: Set<String> = [], hdrFormats: Set<String> = [], directPlay: Bool = true, directStream: Bool = true, remux: Bool = true) {
        self.videoCodecs = videoCodecs
        self.audioCodecs = audioCodecs
        self.containers = containers
        self.hdrFormats = hdrFormats
        self.directPlay = directPlay
        self.directStream = directStream
        self.remux = remux
    }
}

public struct PlaybackSource: Sendable {
    public var container: String?
    public var videoCodec: String?
    public var audioCodec: String?
    public var hdrFormat: String?
    public var subtitlesRequireTranscoding: Bool

    public init(container: String? = nil, videoCodec: String? = nil, audioCodec: String? = nil, hdrFormat: String? = nil, subtitlesRequireTranscoding: Bool = false) {
        self.container = container
        self.videoCodec = videoCodec
        self.audioCodec = audioCodec
        self.hdrFormat = hdrFormat
        self.subtitlesRequireTranscoding = subtitlesRequireTranscoding
    }
}

public enum PlaybackDecisionEngine {
    public static func decide(source: PlaybackSource, capabilities: PlaybackCapabilities) -> PlaybackPath {
        func supported(_ value: String?, by values: Set<String>) -> Bool {
            guard let value else { return true }
            return values.isEmpty || values.contains(value.lowercased())
        }
        let directCompatible = supported(source.videoCodec, by: capabilities.videoCodecs)
            && supported(source.audioCodec, by: capabilities.audioCodecs)
            && supported(source.container, by: capabilities.containers)
            && supported(source.hdrFormat, by: capabilities.hdrFormats)
            && !source.subtitlesRequireTranscoding
        if capabilities.directPlay && directCompatible { return .directPlay }
        if capabilities.directStream && directCompatible { return .directStream }
        if capabilities.remux && supported(source.videoCodec, by: capabilities.videoCodecs) { return .remux }
        return capabilities.directStream || capabilities.directPlay ? .transcode : .fallback
    }
}
