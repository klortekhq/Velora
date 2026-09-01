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

    private let baseURL: URL
    private let session: URLSession
    private var accessToken: String?
    private var sessionState: JellyfinSession?

    public init(serverURL: URL, session: URLSession = .shared) throws {
        guard serverURL.scheme == "http" || serverURL.scheme == "https" else { throw ClientError.invalidServerURL }
        self.baseURL = serverURL
        self.session = session
    }

    public func setAccessToken(_ token: String?) {
        accessToken = token
        sessionState = nil
    }

    public func authenticate(username: String, password: String) async throws -> JellyfinSession {
        var request = URLRequest(url: baseURL.appendingPathComponent("Users/AuthenticateByName"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Velora/1.3.0", forHTTPHeaderField: "X-Emby-Client")
        request.httpBody = try JSONEncoder().encode(["Username": username, "Pw": password])
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ClientError.invalidResponse }
        guard http.statusCode != 401 else { throw ClientError.unauthorized }
        guard (200..<300).contains(http.statusCode) else { throw ClientError.invalidResponse }
        let result = try JSONDecoder().decode(AuthenticateResponse.self, from: data)
        accessToken = result.accessToken
        let authenticated = JellyfinSession(accessToken: result.accessToken, userID: result.user.id, username: result.user.name)
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

    private func request<T: Decodable>(_ url: URL, as type: T.Type) async throws -> T {
        var request = URLRequest(url: url)
        request.setValue("Velora/1.3.0", forHTTPHeaderField: "X-Emby-Client")
        if let accessToken { request.setValue(accessToken, forHTTPHeaderField: "X-Emby-Token") }
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ClientError.invalidResponse }
        guard http.statusCode != 401 else { throw ClientError.unauthorized }
        guard (200..<300).contains(http.statusCode) else { throw ClientError.invalidResponse }
        return try JSONDecoder().decode(type, from: data)
    }
}
