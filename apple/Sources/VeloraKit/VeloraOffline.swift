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

public enum VeloraOfflineStoreError: Error, Equatable {
    case insufficientStorage
}

/// Metadata attached to a background URLSession task. It deliberately contains
/// no token or raw media URL; the authenticated URLRequest remains owned by
/// URLSession and this description only identifies the catalog entry to Velora.
public struct VeloraOfflineTransferMetadata: Codable, Sendable {
    public let itemID: String
    public let title: String
    public let serverURL: String
    public let quality: VeloraDownloadQuality

    public init(itemID: String, title: String, serverURL: String, quality: VeloraDownloadQuality) {
        self.itemID = itemID
        self.title = title
        self.serverURL = serverURL
        self.quality = quality
    }
}

/// Owns the iOS/iPadOS background transfer session. The same identifier is
/// recreated after process termination so the OS can deliver completed tasks
/// back to Velora. tvOS never creates or uses this coordinator.
public final class VeloraOfflineTransferCoordinator: NSObject, URLSessionDownloadDelegate, @unchecked Sendable {
    public static let shared = VeloraOfflineTransferCoordinator()

    private let lock = NSLock()
    private lazy var session: URLSession = {
        #if os(iOS)
        let configuration = URLSessionConfiguration.background(withIdentifier: "com.klortek.velora.offline")
        configuration.waitsForConnectivity = true
        configuration.sessionSendsLaunchEvents = true
        #else
        let configuration = URLSessionConfiguration.default
        #endif
        return URLSession(configuration: configuration, delegate: self, delegateQueue: nil)
    }()

    private var completionHandler: (() -> Void)?
    public var onFinished: (@Sendable (VeloraOfflineTransferMetadata, URL, URLResponse) -> Void)?
    public var onFailed: (@Sendable (VeloraOfflineTransferMetadata, Error?) -> Void)?

    public override init() {
        super.init()
    }

    @discardableResult
    public func enqueue(request: URLRequest, metadata: VeloraOfflineTransferMetadata) -> Int {
        let task = session.downloadTask(with: request)
        if let data = try? JSONEncoder().encode(metadata) {
            task.taskDescription = String(data: data, encoding: .utf8)
        }
        task.resume()
        return task.taskIdentifier
    }

    public func setBackgroundCompletionHandler(_ handler: @escaping () -> Void) {
        lock.lock()
        completionHandler = handler
        lock.unlock()
        _ = session
    }

    public func cancelAll() {
        session.getAllTasks { tasks in
            tasks.compactMap { $0 as? URLSessionDownloadTask }.forEach { $0.cancel() }
        }
    }

    public func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        guard let metadata = metadata(for: downloadTask),
              let response = downloadTask.response else { return }
        // Apple only guarantees the delegate URL until this callback returns.
        // Stage it under an opaque temporary name before handing it to the
        // catalog, which may finish on the main actor.
        let stagedURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("velora-offline-\(UUID().uuidString).download")
        do {
            try FileManager.default.moveItem(at: location, to: stagedURL)
            onFinished?(metadata, stagedURL, response)
        } catch {
            onFailed?(metadata, error)
        }
    }

    public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let error, let downloadTask = task as? URLSessionDownloadTask,
              let metadata = metadata(for: downloadTask) else { return }
        onFailed?(metadata, error)
    }

    public func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        lock.lock()
        let handler = completionHandler
        completionHandler = nil
        lock.unlock()
        handler?()
    }

    private func metadata(for task: URLSessionTask) -> VeloraOfflineTransferMetadata? {
        guard let description = task.taskDescription,
              let data = description.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(VeloraOfflineTransferMetadata.self, from: data)
    }
}

/// Small app-managed offline catalog for iPhone/iPad. Background transfers
/// hand their staged file to this same catalog, preserving one playback and
/// integrity contract for foreground and resumed downloads.
public final class VeloraOfflineStore: @unchecked Sendable {
    private let fileManager: FileManager
    public let rootURL: URL
    private let metadataURL: URL
    public let minimumFreeBytes: Int64 = 512 * 1024 * 1024

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
        guard let temporaryAttributes = try? fileManager.attributesOfItem(atPath: temporaryURL.path),
              let byteCount = (temporaryAttributes[.size] as? NSNumber)?.int64Value else {
            throw VeloraOfflineStoreError.insufficientStorage
        }
        guard hasCapacity(forAdditionalBytes: byteCount) else {
            throw VeloraOfflineStoreError.insufficientStorage
        }
        try fileManager.createDirectory(at: rootURL, withIntermediateDirectories: true)
        let fileName = "media-\(UUID().uuidString).bin"
        let destination = rootURL.appendingPathComponent(fileName)
        try fileManager.moveItem(at: temporaryURL, to: destination)
        let attributes = try fileManager.attributesOfItem(atPath: destination.path)
        let storedByteCount = (attributes[.size] as? NSNumber)?.int64Value
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
            byteCount: storedByteCount,
            checksumSha256: checksum
        )
        entries.removeAll { $0.itemID == itemID && $0.serverURL == serverURL }
        entries.append(verifiedEntry)
        try write(entries)
        return verifiedEntry
    }

    /// Keeps a fixed reserve for the operating system and future metadata.
    /// The check is repeated after the temporary transfer completes, when the
    /// actual media size is known even if Jellyfin did not send Content-Length.
    public func hasCapacity(forAdditionalBytes additionalBytes: Int64 = 0) -> Bool {
        guard let values = try? rootURL.resourceValues(forKeys: [.volumeAvailableCapacityForImportantUsageKey]),
              let available = values.volumeAvailableCapacityForImportantUsage else { return true }
        return available >= minimumFreeBytes + max(0, additionalBytes)
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
