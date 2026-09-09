import Foundation

public actor JellyfinClient {
    public enum ClientError: Error { case invalidServerURL, invalidResponse, unauthorized }
    public static let clientVersion = "1.4.0"

    private static func isValidServerURL(_ url: URL) -> Bool {
        guard let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https",
              let host = url.host, !host.isEmpty,
              url.user == nil, url.password == nil,
              url.query == nil, url.fragment == nil else { return false }
        return url.port == nil || (url.port! > 0 && url.port! <= 65_535)
    }

    private struct AuthenticateResponse: Decodable {
        struct User: Decodable {
            let id: String
            let name: String?
            enum CodingKeys: String, CodingKey { case id = "Id", name = "Name" }
        }
        let accessToken: String
        let user: User
        enum CodingKeys: String, CodingKey { case accessToken = "AccessToken", user = "User" }
    }

    private struct PlaybackMediaSource: Decodable {
        let id: String?
        let directStreamURL: URL?
        let transcodingURL: URL?
        let protocolName: String?

        enum CodingKeys: String, CodingKey {
            case id = "Id"
            case directStreamURL = "DirectStreamUrl"
            case transcodingURL = "TranscodingUrl"
            case protocolName = "Protocol"
        }
    }

    private struct PlaybackInfoResponse: Decodable {
        let mediaSources: [PlaybackMediaSource]

        enum CodingKeys: String, CodingKey { case mediaSources = "MediaSources" }
    }

    private struct ThemeSongResult: Decodable {
        struct ThemeSong: Decodable {
            let id: String
            enum CodingKeys: String, CodingKey { case id = "Id" }
        }
        let items: [ThemeSong]
        enum CodingKeys: String, CodingKey { case items = "Items" }
    }

    private var baseURL: URL
    private let session: URLSession
    private var accessToken: String?
    private var sessionState: JellyfinSession?

    public init(serverURL: URL, session: URLSession = .shared, restoredSession: JellyfinSession? = nil) throws {
        guard Self.isValidServerURL(serverURL) else { throw ClientError.invalidServerURL }
        self.baseURL = serverURL
        self.session = session
        self.accessToken = restoredSession?.accessToken
        self.sessionState = restoredSession
    }

    public func setServerURL(_ serverURL: URL) throws {
        guard Self.isValidServerURL(serverURL) else { throw ClientError.invalidServerURL }
        baseURL = serverURL
        accessToken = nil
        sessionState = nil
    }

    public func serverURL() -> URL { baseURL }

    public func setAccessToken(_ token: String?) {
        accessToken = token
        sessionState = nil
    }

    public func authenticate(username: String, password: String, languageIdentifier: String? = nil) async throws -> JellyfinSession {
        var request = URLRequest(url: baseURL.appendingPathComponent("Users/AuthenticateByName"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let language = languageIdentifier?
            .split(separator: "-", maxSplits: 1, omittingEmptySubsequences: true)
            .first.map(String.init) ?? "en"
        request.setValue(
            "MediaBrowser Client=\"Velora\", Device=\"Apple\", DeviceId=\"velora-apple\", Version=\"\(Self.clientVersion)\", Language=\"\(language)\"",
            forHTTPHeaderField: "X-Emby-Authorization"
        )
        // Jellyfin's AuthenticateByName contract calls the password field
        // `Pw`; keep this identical to Android and the QA smoke test.
        request.httpBody = try JSONEncoder().encode(["Username": username, "Pw": password])
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ClientError.invalidResponse }
        guard http.statusCode != 401 else { throw ClientError.unauthorized }
        guard (200..<300).contains(http.statusCode) else { throw ClientError.invalidResponse }
        let result = try JSONDecoder().decode(AuthenticateResponse.self, from: data)
        accessToken = result.accessToken
        let authenticated = JellyfinSession(
            accessToken: result.accessToken,
            userID: result.user.id,
            username: result.user.name,
            serverURL: baseURL.absoluteString
        )
        sessionState = authenticated
        return authenticated
    }

    public func currentSession() -> JellyfinSession? {
        sessionState
    }

    public func imageURL(itemID: String, kind: String = "Primary", maxWidth: Int? = nil) -> URL? {
        guard let imageURL = itemURL(root: "Items", itemID: itemID, suffix: ["Images", kind]) else { return nil }
        var components = URLComponents(url: imageURL, resolvingAgainstBaseURL: false)
        if let maxWidth { components?.queryItems = [URLQueryItem(name: "MaxWidth", value: String(maxWidth))] }
        return components?.url
    }

    /// URL for the server's browser/AVPlayer-compatible stream endpoint.
    /// Authentication is still supplied by `authorizedRequest(for:)` and is
    /// never embedded in this URL.
    public func videoURL(itemID: String, quality: VeloraDownloadQuality = .original) -> URL? {
        guard let streamURL = itemURL(root: "Videos", itemID: itemID, suffix: ["stream"]) else { return nil }
        var components = URLComponents(url: streamURL, resolvingAgainstBaseURL: false)
        // Static streams preserve Original. A bounded preset must leave the
        // server free to transcode; keeping static=true would silently ignore
        // the requested quality on Jellyfin.
        var queryItems = [URLQueryItem(name: "static", value: quality == .original ? "true" : "false")]
        if let maxWidth = quality.maxWidth {
            queryItems.append(URLQueryItem(name: "MaxWidth", value: String(maxWidth)))
        }
        if let videoBitrate = quality.videoBitrate {
            queryItems.append(URLQueryItem(name: "VideoBitrate", value: String(videoBitrate)))
            queryItems.append(URLQueryItem(name: "TranscodingContainer", value: "mp4"))
        }
        components?.queryItems = queryItems
        return components?.url
    }

    /// Resolves the first server-managed theme song without placing the
    /// Jellyfin token in the returned URL. The caller authenticates the final
    /// audio request through `authorizedRequest(for:)`.
    public func themeSongURL(itemID: String, userID: String) async -> URL? {
        guard !userID.isEmpty,
              let itemURL = itemURL(root: "Items", itemID: itemID, suffix: ["ThemeSongs"])
        else { return nil }
        var components = URLComponents(url: itemURL, resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "UserId", value: userID)]
        guard let url = components?.url,
              let result = try? await request(url, as: ThemeSongResult.self),
              let songID = result.items.first?.id,
              let audioURL = itemURL(root: "Audio", itemID: songID, suffix: ["universal"]) else { return nil }
        var audioComponents = URLComponents(url: audioURL, resolvingAgainstBaseURL: false)
        audioComponents?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "Container", value: "mp3,aac,m4a,flac,ogg,webm"),
            URLQueryItem(name: "TranscodingContainer", value: "ts"),
            URLQueryItem(name: "TranscodingProtocol", value: "hls"),
            URLQueryItem(name: "AudioCodec", value: "aac")
        ]
        return audioComponents?.url
    }

    /// Ask Jellyfin for its canonical source decision before opening VOD.
    /// This preserves Original First on Apple while retaining a safe generic
    /// stream fallback for servers that do not return a playable URL.
    public func playbackURL(itemID: String, userID: String) async -> URL? {
        guard !userID.isEmpty,
              let itemURL = itemURL(root: "Items", itemID: itemID, suffix: ["PlaybackInfo"])
        else { return nil }

        var components = URLComponents(url: itemURL, resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "AutoOpenLiveStream", value: "false"),
            URLQueryItem(name: "EnableDirectPlay", value: "true"),
            URLQueryItem(name: "EnableDirectStream", value: "true"),
            URLQueryItem(name: "EnableTranscoding", value: "true")
        ]
        guard let url = components?.url else { return nil }
        var request = authorizedRequest(for: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = Data("{}".utf8)

        guard let (data, response) = try? await session.data(for: request),
              let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode),
              let info = try? JSONDecoder().decode(PlaybackInfoResponse.self, from: data)
        else { return nil }

        for source in info.mediaSources {
            let candidate = source.directStreamURL ?? source.transcodingURL
            if let candidate, let sanitized = sanitizedServerMediaURL(candidate) { return sanitized }
        }
        return nil
    }

    /// Build an authenticated request for artwork or media without exposing
    /// the Jellyfin token in a URL. Callers can pass this to URLSession/AVURLAsset.
    public func authorizedRequest(for url: URL) -> URLRequest {
        let safeURL = sanitizedServerMediaURL(url) ?? url
        var request = URLRequest(url: safeURL)
        request.setValue("Velora/\(Self.clientVersion)", forHTTPHeaderField: "X-Emby-Client")
        if isServerURL(safeURL), let accessToken {
            request.setValue(accessToken, forHTTPHeaderField: "X-Emby-Token")
        }
        return request
    }

    /// Jellyfin item IDs are opaque single path components. Rejecting path
    /// separators here prevents malformed or untrusted IDs from escaping the
    /// intended resource endpoint.
    private func itemURL(root: String, itemID: String, suffix: [String]) -> URL? {
        guard !itemID.isEmpty,
              !itemID.contains("/"),
              !itemID.contains("\\") else { return nil }
        let parts = [root, itemID] + suffix
        return parts.enumerated().reduce(baseURL) { url, entry in
            url.appendingPathComponent(entry.element, isDirectory: entry.offset < parts.count - 1)
        }
    }

    /// Jellyfin may return a playable URL containing a server-side credential
    /// for legacy clients. Velora always authenticates through headers, so
    /// those query items must never reach AVPlayer, logs, or share sheets.
    private func sanitizedServerMediaURL(_ url: URL) -> URL? {
        guard isServerURL(url) else { return nil }
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return nil }
        let sensitiveNames: Set<String> = [
            "api_key", "apikey", "access_token", "token", "x-emby-token", "authorization"
        ]
        components.queryItems = components.queryItems?.filter {
            !sensitiveNames.contains($0.name.lowercased())
        }
        return components.url
    }

    private func isServerURL(_ url: URL) -> Bool {
        guard url.scheme?.lowercased() == baseURL.scheme?.lowercased(),
              url.host?.lowercased() == baseURL.host?.lowercased(),
              url.port == baseURL.port else { return false }

        // Jellyfin can live below a reverse-proxy prefix. Same-origin is not
        // enough here: a URL on the same host but outside that prefix must not
        // be handed to AVPlayer with the session token.
        let configuredPath = baseURL.path.isEmpty ? "/" : baseURL.path
        let candidatePath = url.path.isEmpty ? "/" : url.path
        let normalizedPrefix = configuredPath.hasSuffix("/")
            ? String(configuredPath.dropLast())
            : configuredPath
        return normalizedPrefix == "/"
            || candidatePath == normalizedPrefix
            || candidatePath.hasPrefix(normalizedPrefix + "/")
    }

    public func itemsPage(userID: String, parentID: String? = nil, includeTypes: [String] = [], startIndex: Int = 0, limit: Int = 100) async throws -> JellyfinResult<JellyfinItem> {
        var components = URLComponents(url: baseURL.appendingPathComponent("Items"), resolvingAgainstBaseURL: false)
        var query = [URLQueryItem(name: "UserId", value: userID), URLQueryItem(name: "Recursive", value: "true")]
        if let parentID { query.append(URLQueryItem(name: "ParentId", value: parentID)) }
        if !includeTypes.isEmpty { query.append(URLQueryItem(name: "IncludeItemTypes", value: includeTypes.joined(separator: ","))) }
        query.append(URLQueryItem(name: "StartIndex", value: String(max(0, startIndex))))
        query.append(URLQueryItem(name: "Limit", value: String(max(1, min(limit, 500)))))
        // Keep catalog pages light. MediaSources are resolved by PlaybackInfo
        // when the user presses Play; they do not belong in every card.
        query.append(URLQueryItem(name: "Fields", value: "Overview,ProductionYear,ImageTags,People,UserData"))
        components?.queryItems = query
        guard let url = components?.url else { throw ClientError.invalidServerURL }
        return try await request(url, as: JellyfinResult<JellyfinItem>.self)
    }

    public func items(userID: String, parentID: String? = nil, includeTypes: [String] = []) async throws -> [JellyfinItem] {
        try await itemsPage(userID: userID, parentID: parentID, includeTypes: includeTypes).items
    }

    public func items(userID: String, forPerson personID: String) async throws -> [JellyfinItem] {
        guard !userID.isEmpty, !userID.contains("/"), !userID.contains("\\"),
              !personID.isEmpty, !personID.contains("/"), !personID.contains("\\") else { return [] }
        let itemsPath = baseURL
            .appendingPathComponent("Users", isDirectory: true)
            .appendingPathComponent(userID, isDirectory: true)
            .appendingPathComponent("Items")
        var components = URLComponents(url: itemsPath, resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "Recursive", value: "true"),
            URLQueryItem(name: "PersonIds", value: personID),
            URLQueryItem(name: "IncludeItemTypes", value: "Movie,Series"),
            URLQueryItem(name: "Fields", value: "Overview,ProductionYear,ImageTags,People,UserData"),
            URLQueryItem(name: "SortBy", value: "DateCreated"),
            URLQueryItem(name: "SortOrder", value: "Descending"),
            URLQueryItem(name: "Limit", value: "100")
        ]
        guard let url = components?.url else { throw ClientError.invalidServerURL }
        return try await request(url, as: JellyfinResult<JellyfinItem>.self).items
    }

    public func liveTvChannels(userID: String) async throws -> [JellyfinLiveTvChannel] {
        var components = URLComponents(url: baseURL.appendingPathComponent("LiveTv/Channels"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "AddCurrentProgram", value: "true"),
            URLQueryItem(name: "EnableImages", value: "true"),
            URLQueryItem(name: "Fields", value: "Overview,MediaSources")
        ]
        guard let url = components?.url else { throw ClientError.invalidServerURL }
        return try await request(url, as: JellyfinResult<JellyfinLiveTvChannel>.self).items
    }

    public func liveTvPrograms(userID: String, channelIDs: [String], from start: Date, until end: Date) async throws -> [JellyfinLiveTvProgram] {
        guard !channelIDs.isEmpty else { return [] }
        var components = URLComponents(url: baseURL.appendingPathComponent("LiveTv/Programs"), resolvingAgainstBaseURL: false)
        let formatter = ISO8601DateFormatter()
        components?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "ChannelIds", value: channelIDs.joined(separator: ",")),
            URLQueryItem(name: "MinStartDate", value: formatter.string(from: start)),
            URLQueryItem(name: "MaxEndDate", value: formatter.string(from: end)),
            URLQueryItem(name: "Limit", value: "500")
        ]
        guard let url = components?.url else { throw ClientError.invalidServerURL }
        return try await request(url, as: JellyfinResult<JellyfinLiveTvProgram>.self).items
    }

    /// Opens a Jellyfin Live TV tuner and returns the server-selected stream.
    /// The returned URL is checked against the configured server before AVPlayer uses it.
    public func liveTvPlaybackURL(userID: String, channelID: String, mediaSourceID: String? = nil) async throws -> URL? {
        guard !channelID.isEmpty, !channelID.contains("/"), !channelID.contains("\\") else { return nil }
        var components = URLComponents(url: baseURL.appendingPathComponent("Items/\(channelID)/PlaybackInfo"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "StartTimeTicks", value: "0"),
            URLQueryItem(name: "IsPlayback", value: "true"),
            URLQueryItem(name: "AutoOpenLiveStream", value: "true")
        ]
        if let mediaSourceID, !mediaSourceID.isEmpty {
            var queryItems = components?.queryItems ?? []
            queryItems.append(URLQueryItem(name: "MediaSourceId", value: mediaSourceID))
            components?.queryItems = queryItems
        }
        guard let url = components?.url else { throw ClientError.invalidServerURL }
        var request = authorizedRequest(for: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = Data("{}".utf8)
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ClientError.invalidResponse }
        guard (200..<300).contains(http.statusCode) else {
            throw http.statusCode == 401 ? ClientError.unauthorized : ClientError.invalidResponse
        }
        let info = try JSONDecoder().decode(JellyfinLiveTvPlaybackInfo.self, from: data)
        // Jellyfin may return more than one playable source even after a
        // MediaSourceId was supplied. Keep the source selected in Velora's
        // picker when the server echoes its ID (or LiveStreamId); only fall
        // back to the first source for servers that omit those identifiers.
        let selectedSource = Self.selectLiveTvMediaSource(
            info.mediaSources,
            requestedID: mediaSourceID
        )
        let selected = selectedSource?.transcodingURL ?? selectedSource?.directStreamURL
        guard let selected else { return nil }
        return sanitizedServerMediaURL(selected)
    }

    static func selectLiveTvMediaSource(
        _ sources: [JellyfinLiveTvMediaSource],
        requestedID: String?
    ) -> JellyfinLiveTvMediaSource? {
        guard let requestedID, !requestedID.isEmpty else { return sources.first }
        return sources.first {
            $0.id == requestedID || $0.liveStreamID == requestedID
        } ?? sources.first
    }

    /// Releases a Live TV session and updates Jellyfin's playback state.
    /// The item identifier is validated before it is sent to the server.
    public func reportPlaybackStopped(itemID: String, positionTicks: Int64 = 0) async {
        guard !itemID.isEmpty, !itemID.contains("/"), !itemID.contains("\\") else { return }
        var request = URLRequest(url: baseURL.appendingPathComponent("Sessions/Playing/Stopped"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Velora/\(Self.clientVersion)", forHTTPHeaderField: "X-Emby-Client")
        if let accessToken { request.setValue(accessToken, forHTTPHeaderField: "X-Emby-Token") }
        request.httpBody = try? JSONEncoder().encode(
            JellyfinPlaybackStoppedRequest(itemID: itemID, positionTicks: max(0, positionTicks))
        )
        _ = try? await session.data(for: request)
    }

    private func request<T: Decodable>(_ url: URL, as type: T.Type) async throws -> T {
        var request = URLRequest(url: url)
        request.setValue("Velora/\(Self.clientVersion)", forHTTPHeaderField: "X-Emby-Client")
        if let accessToken { request.setValue(accessToken, forHTTPHeaderField: "X-Emby-Token") }
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ClientError.invalidResponse }
        guard http.statusCode != 401 else { throw ClientError.unauthorized }
        guard (200..<300).contains(http.statusCode) else { throw ClientError.invalidResponse }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode(type, from: data)
    }
}
