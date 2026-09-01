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

public enum PlaybackQuality: Sendable, Equatable {
    case original, automatic, fourK, fullHD20, fullHD10, hd5, sd2
}

public struct PlaybackCapabilities: Sendable {
    public var videoCodecs: Set<String>
    public var audioCodecs: Set<String>
    public var containers: Set<String>
    public var hdrFormats: Set<String>
    public var directPlay: Bool
    public var directStream: Bool
    public var remux: Bool
    public var audioPassthroughCodecs: Set<String>
    public var audioPassthrough: Bool
    public var maxAudioChannels: Int?
    public var maxWidth: Int?
    public var maxHeight: Int?
    public var maxFrameRate: Double?

    public init(videoCodecs: Set<String> = [], audioCodecs: Set<String> = [], containers: Set<String> = [], hdrFormats: Set<String> = [], directPlay: Bool = true, directStream: Bool = true, remux: Bool = true, audioPassthroughCodecs: Set<String> = [], audioPassthrough: Bool = false, maxAudioChannels: Int? = nil, maxWidth: Int? = nil, maxHeight: Int? = nil, maxFrameRate: Double? = nil) {
        self.videoCodecs = Set(videoCodecs.map { $0.lowercased() })
        self.audioCodecs = Set(audioCodecs.map { $0.lowercased() })
        self.containers = Set(containers.map { $0.lowercased() })
        self.hdrFormats = Set(hdrFormats.map { $0.lowercased() })
        self.directPlay = directPlay
        self.directStream = directStream
        self.remux = remux
        self.audioPassthroughCodecs = Set(audioPassthroughCodecs.map { $0.lowercased() })
        self.audioPassthrough = audioPassthrough
        self.maxAudioChannels = maxAudioChannels
        self.maxWidth = maxWidth
        self.maxHeight = maxHeight
        self.maxFrameRate = maxFrameRate
    }
}

public struct PlaybackSource: Sendable {
    public var container: String?
    public var videoCodec: String?
    public var audioCodec: String?
    public var hdrFormat: String?
    public var videoProfile: String?
    public var videoLevel: String?
    public var audioChannels: Int?
    public var width: Int?
    public var height: Int?
    public var frameRate: Double?
    public var bitrateKbps: Int?
    public var subtitlesRequireTranscoding: Bool

    public init(container: String? = nil, videoCodec: String? = nil, audioCodec: String? = nil, hdrFormat: String? = nil, videoProfile: String? = nil, videoLevel: String? = nil, audioChannels: Int? = nil, width: Int? = nil, height: Int? = nil, frameRate: Double? = nil, bitrateKbps: Int? = nil, subtitlesRequireTranscoding: Bool = false) {
        self.container = container
        self.videoCodec = videoCodec
        self.audioCodec = audioCodec
        self.hdrFormat = hdrFormat
        self.videoProfile = videoProfile
        self.videoLevel = videoLevel
        self.audioChannels = audioChannels
        self.width = width
        self.height = height
        self.frameRate = frameRate
        self.bitrateKbps = bitrateKbps
        self.subtitlesRequireTranscoding = subtitlesRequireTranscoding
    }
}

public enum PlaybackDecisionEngine {
    public static func decide(source: PlaybackSource, capabilities: PlaybackCapabilities, quality: PlaybackQuality = .original) -> PlaybackPath {
        func supported(_ value: String?, by values: Set<String>) -> Bool {
            guard let value else { return true }
            return values.isEmpty || values.contains(value.lowercased())
        }
        let directCompatible = supported(source.videoCodec, by: capabilities.videoCodecs)
            && supported(source.audioCodec, by: capabilities.audioCodecs)
            && supported(source.container, by: capabilities.containers)
            && supported(source.hdrFormat, by: capabilities.hdrFormats)
            && channelsFit(source, capabilities)
            && passthroughFits(source, capabilities)
            && dimensionsFit(source, capabilities)
            && presetFits(source, quality)
            && !source.subtitlesRequireTranscoding
        if capabilities.directPlay && directCompatible { return .directPlay }
        if capabilities.directStream && directCompatible { return .directStream }
        if capabilities.directStream && directStreamCompatible(source: source, capabilities: capabilities) { return .directStream }
        if capabilities.remux && supported(source.videoCodec, by: capabilities.videoCodecs) && channelsFit(source, capabilities) { return .remux }
        return capabilities.directStream || capabilities.directPlay ? .transcode : .fallback
    }

    private static func directStreamCompatible(source: PlaybackSource, capabilities: PlaybackCapabilities) -> Bool {
        supported(source.videoCodec, by: capabilities.videoCodecs)
            && supported(source.audioCodec, by: capabilities.audioCodecs)
            && supported(source.hdrFormat, by: capabilities.hdrFormats)
            && channelsFit(source, capabilities)
            && passthroughFits(source, capabilities)
            && !source.subtitlesRequireTranscoding
    }

    private static func channelsFit(_ source: PlaybackSource, _ capabilities: PlaybackCapabilities) -> Bool {
        capabilities.maxAudioChannels == nil || source.audioChannels == nil || source.audioChannels! <= capabilities.maxAudioChannels!
    }

    private static func passthroughFits(_ source: PlaybackSource, _ capabilities: PlaybackCapabilities) -> Bool {
        guard let codec = source.audioCodec?.lowercased(), capabilities.audioPassthroughCodecs.contains(codec) else { return true }
        return capabilities.audioPassthrough
    }

    private static func dimensionsFit(_ source: PlaybackSource, _ capabilities: PlaybackCapabilities) -> Bool {
        (capabilities.maxWidth == nil || source.width == nil || source.width! <= capabilities.maxWidth!)
            && (capabilities.maxHeight == nil || source.height == nil || source.height! <= capabilities.maxHeight!)
            && (capabilities.maxFrameRate == nil || source.frameRate == nil || source.frameRate! <= capabilities.maxFrameRate!)
    }

    private static func presetFits(_ source: PlaybackSource, _ quality: PlaybackQuality) -> Bool {
        guard quality != .original && quality != .automatic else { return true }
        let maxBitrate: Int
        let maxWidth: Int
        switch quality {
        case .fourK: maxBitrate = 40_000; maxWidth = 3840
        case .fullHD20: maxBitrate = 20_000; maxWidth = 1920
        case .fullHD10: maxBitrate = 10_000; maxWidth = 1920
        case .hd5: maxBitrate = 5_000; maxWidth = 1280
        case .sd2: maxBitrate = 2_000; maxWidth = 854
        case .original, .automatic: return true
        }
        return (source.bitrateKbps == nil || source.bitrateKbps! <= maxBitrate)
            && (source.width == nil || source.width! <= maxWidth)
    }
}
