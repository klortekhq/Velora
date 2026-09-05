import CryptoKit
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
    public let byteCount: Int64?
    public let checksumSha256: String?

    public init(
        id: String = UUID().uuidString,
        itemID: String,
        title: String,
        serverURL: String,
        fileName: String,
        createdAt: Date = Date(),
        byteCount: Int64? = nil,
        checksumSha256: String? = nil
    ) {
        self.id = id
        self.itemID = itemID
        self.title = title
        self.serverURL = serverURL
        self.fileName = fileName
        self.createdAt = createdAt
        self.byteCount = byteCount
        self.checksumSha256 = checksumSha256
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

    /// Verifies the managed file before offline playback. Older metadata that
    /// predates checksums remains playable and receives calculated metadata in
    /// the returned value without trusting any persisted checksum blindly.
    public func verifyIntegrity(_ entry: VeloraOfflineDownload) -> VeloraOfflineDownload? {
        let url = mediaURL(for: entry)
        guard fileManager.fileExists(atPath: url.path),
              let attributes = try? fileManager.attributesOfItem(atPath: url.path),
              let size = (attributes[.size] as? NSNumber)?.int64Value,
              let checksum = sha256(url: url) else { return nil }
        if let expectedSize = entry.byteCount, expectedSize != size { return nil }
        if let expectedChecksum = entry.checksumSha256,
           !expectedChecksum.caseInsensitiveCompare(checksum).isOrderedSame { return nil }
        return VeloraOfflineDownload(
            id: entry.id,
            itemID: entry.itemID,
            title: entry.title,
            serverURL: entry.serverURL,
            fileName: entry.fileName,
            createdAt: entry.createdAt,
            byteCount: size,
            checksumSha256: checksum
        )
    }

    @discardableResult
    public func add(mediaAt temporaryURL: URL, itemID: String, title: String, serverURL: String) throws -> VeloraOfflineDownload {
        try fileManager.createDirectory(at: rootURL, withIntermediateDirectories: true)
        let fileName = "media-\(UUID().uuidString).bin"
        let destination = rootURL.appendingPathComponent(fileName)
        try fileManager.moveItem(at: temporaryURL, to: destination)
        let attributes = try fileManager.attributesOfItem(atPath: destination.path)
        let byteCount = (attributes[.size] as? NSNumber)?.int64Value
        let checksum = sha256(url: destination)
        var entries = load()
        let entry = VeloraOfflineDownload(itemID: itemID, title: title, serverURL: serverURL, fileName: fileName)
        let verifiedEntry = VeloraOfflineDownload(
            id: entry.id,
            itemID: entry.itemID,
            title: entry.title,
            serverURL: entry.serverURL,
            fileName: entry.fileName,
            createdAt: entry.createdAt,
            byteCount: byteCount,
            checksumSha256: checksum
        )
        entries.removeAll { $0.itemID == itemID && $0.serverURL == serverURL }
        entries.append(verifiedEntry)
        try write(entries)
        return verifiedEntry
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

    private func sha256(url: URL) -> String? {
        guard let handle = try? FileHandle(forReadingFrom: url) else { return nil }
        defer { try? handle.close() }
        var hasher = SHA256()
        while true {
            let chunk: Data
            do {
                guard let data = try handle.read(upToCount: 1024 * 1024), !data.isEmpty else { break }
                chunk = data
            } catch {
                return nil
            }
            hasher.update(data: chunk)
        }
        return hasher.finalize().map { String(format: "%02x", $0) }.joined()
    }
}
