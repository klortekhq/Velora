import Foundation

public actor JellyfinClient {
    public enum ClientError: Error { case invalidServerURL, invalidResponse, unauthorized }

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

    private var baseURL: URL
    private let session: URLSession
    private var accessToken: String?
    private var sessionState: JellyfinSession?

    public init(serverURL: URL, session: URLSession = .shared, restoredSession: JellyfinSession? = nil) throws {
        guard serverURL.scheme == "http" || serverURL.scheme == "https" else { throw ClientError.invalidServerURL }
        self.baseURL = serverURL
        self.session = session
        self.accessToken = restoredSession?.accessToken
        self.sessionState = restoredSession
    }

    public func setServerURL(_ serverURL: URL) throws {
        guard serverURL.scheme == "http" || serverURL.scheme == "https" else { throw ClientError.invalidServerURL }
        baseURL = serverURL
        accessToken = nil
        sessionState = nil
    }

    public func serverURL() -> URL { baseURL }

    public func setAccessToken(_ token: String?) {
        accessToken = token
        sessionState = nil
    }

    public func authenticate(username: String, password: String) async throws -> JellyfinSession {
        var request = URLRequest(url: baseURL.appendingPathComponent("Users/AuthenticateByName"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(
            "MediaBrowser Client=\"Velora\", Device=\"Apple\", DeviceId=\"velora-apple\", Version=\"1.3.0\", Language=\"en\"",
            forHTTPHeaderField: "X-Emby-Authorization"
        )
        request.httpBody = try JSONEncoder().encode(["Username": username, "Password": password])
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
    public func videoURL(itemID: String) -> URL? {
        guard let streamURL = itemURL(root: "Videos", itemID: itemID, suffix: ["stream"]) else { return nil }
        var components = URLComponents(url: streamURL, resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "static", value: "true")]
        return components?.url
    }

    /// Build an authenticated request for artwork or media without exposing
    /// the Jellyfin token in a URL. Callers can pass this to URLSession/AVURLAsset.
    public func authorizedRequest(for url: URL) -> URLRequest {
        var request = URLRequest(url: url)
        request.setValue("Velora/1.3.0", forHTTPHeaderField: "X-Emby-Client")
        if let accessToken { request.setValue(accessToken, forHTTPHeaderField: "X-Emby-Token") }
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

    public func items(userID: String, parentID: String? = nil, includeTypes: [String] = []) async throws -> [JellyfinItem] {
        var components = URLComponents(url: baseURL.appendingPathComponent("Items"), resolvingAgainstBaseURL: false)
        var query = [URLQueryItem(name: "UserId", value: userID), URLQueryItem(name: "Recursive", value: "true")]
        if let parentID { query.append(URLQueryItem(name: "ParentId", value: parentID)) }
        if !includeTypes.isEmpty { query.append(URLQueryItem(name: "IncludeItemTypes", value: includeTypes.joined(separator: ","))) }
        components?.queryItems = query
        guard let url = components?.url else { throw ClientError.invalidServerURL }
        return try await request(url, as: JellyfinResult<JellyfinItem>.self).items
    }

    public func liveTvChannels(userID: String) async throws -> [JellyfinLiveTvChannel] {
        var components = URLComponents(url: baseURL.appendingPathComponent("LiveTv/Channels"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "AddCurrentProgram", value: "true"),
            URLQueryItem(name: "EnableImages", value: "true"),
            URLQueryItem(name: "Fields", value: "Overview")
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
    public func liveTvPlaybackURL(userID: String, channelID: String) async throws -> URL? {
        guard !channelID.isEmpty, !channelID.contains("/"), !channelID.contains("\\") else { return nil }
        var components = URLComponents(url: baseURL.appendingPathComponent("Items/\(channelID)/PlaybackInfo"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "UserId", value: userID),
            URLQueryItem(name: "StartTimeTicks", value: "0"),
            URLQueryItem(name: "IsPlayback", value: "true"),
            URLQueryItem(name: "AutoOpenLiveStream", value: "true")
        ]
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
        let selected = info.mediaSources.first?.transcodingURL ?? info.mediaSources.first?.directStreamURL
        guard let selected,
              selected.scheme == baseURL.scheme,
              selected.host?.lowercased() == baseURL.host?.lowercased(),
              selected.port == baseURL.port else { return nil }
        return selected
    }

    /// Releases a Live TV session and updates Jellyfin's playback state.
    /// The item identifier is validated before it is sent to the server.
    public func reportPlaybackStopped(itemID: String, positionTicks: Int64 = 0) async {
        guard !itemID.isEmpty, !itemID.contains("/"), !itemID.contains("\\") else { return }
        var request = URLRequest(url: baseURL.appendingPathComponent("Sessions/Playing/Stopped"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Velora/1.3.0", forHTTPHeaderField: "X-Emby-Client")
        if let accessToken { request.setValue(accessToken, forHTTPHeaderField: "X-Emby-Token") }
        request.httpBody = try? JSONEncoder().encode(
            JellyfinPlaybackStoppedRequest(itemID: itemID, positionTicks: max(0, positionTicks))
        )
        _ = try? await session.data(for: request)
    }

    private func request<T: Decodable>(_ url: URL, as type: T.Type) async throws -> T {
        var request = URLRequest(url: url)
        request.setValue("Velora/1.3.0", forHTTPHeaderField: "X-Emby-Client")
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
