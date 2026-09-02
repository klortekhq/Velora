import Foundation

/// A completed media item owned by Velora on a mobile Apple device.
/// The file name is an opaque identifier; Jellyfin paths and tokens are never
/// exposed to the UI or persisted in the metadata.
public struct VeloraOfflineDownload: Codable, Equatable, Identifiable, Sendable {
    public let id: String
    public let itemID: String
    public let title: String
    public let serverURL: String
    public let fileName: String
    public let createdAt: Date

    public init(
        id: String = UUID().uuidString,
        itemID: String,
        title: String,
        serverURL: String,
        fileName: String,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.itemID = itemID
        self.title = title
        self.serverURL = serverURL
        self.fileName = fileName
        self.createdAt = createdAt
    }
}

/// Small app-managed offline catalog for iPhone/iPad.
/// A later background-transfer layer can use the same catalog without
/// changing the playback or UI contract.
public final class VeloraOfflineStore: @unchecked Sendable {
    private let fileManager: FileManager
    public let rootURL: URL
    private let metadataURL: URL

    public init(rootURL: URL? = nil, fileManager: FileManager = .default) {
        self.fileManager = fileManager
        self.rootURL = rootURL ?? fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Velora/offline", isDirectory: true)
        self.metadataURL = self.rootURL.appendingPathComponent("downloads.json")
    }

    public func load() -> [VeloraOfflineDownload] {
        guard let data = try? Data(contentsOf: metadataURL),
              let entries = try? JSONDecoder().decode([VeloraOfflineDownload].self, from: data) else {
            return []
        }
        return entries.filter { fileManager.fileExists(atPath: mediaURL(for: $0).path) }
    }

    public func mediaURL(for entry: VeloraOfflineDownload) -> URL {
        rootURL.appendingPathComponent(entry.fileName, isDirectory: false)
    }

    @discardableResult
    public func add(mediaAt temporaryURL: URL, itemID: String, title: String, serverURL: String) throws -> VeloraOfflineDownload {
        try fileManager.createDirectory(at: rootURL, withIntermediateDirectories: true)
        let fileName = "media-\(UUID().uuidString).bin"
        let destination = rootURL.appendingPathComponent(fileName)
        try fileManager.moveItem(at: temporaryURL, to: destination)
        var entries = load()
        let entry = VeloraOfflineDownload(itemID: itemID, title: title, serverURL: serverURL, fileName: fileName)
        entries.removeAll { $0.itemID == itemID && $0.serverURL == serverURL }
        entries.append(entry)
        try write(entries)
        return entry
    }

    public func remove(_ entry: VeloraOfflineDownload) throws {
        try? fileManager.removeItem(at: mediaURL(for: entry))
        try write(load().filter { $0.id != entry.id })
    }

    private func write(_ entries: [VeloraOfflineDownload]) throws {
        try fileManager.createDirectory(at: rootURL, withIntermediateDirectories: true)
        let data = try JSONEncoder().encode(entries)
        try data.write(to: metadataURL, options: .atomic)
    }
}
