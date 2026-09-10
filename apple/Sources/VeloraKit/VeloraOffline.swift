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
    /// Jellyfin user ownership. Empty only for legacy catalogs created before
    /// account-scoped offline storage was introduced.
    public let userID: String
    public let fileName: String
    public let createdAt: Date
    public let byteCount: Int64?
    public let checksumSha256: String?
    public let quality: VeloraDownloadQuality

    private enum CodingKeys: String, CodingKey {
        case id, itemID, title, serverURL, userID, fileName, createdAt, byteCount, checksumSha256, quality
    }

    public init(
        id: String = UUID().uuidString,
        itemID: String,
        title: String,
        serverURL: String,
        userID: String = "",
        fileName: String,
        createdAt: Date = Date(),
        byteCount: Int64? = nil,
        checksumSha256: String? = nil,
        quality: VeloraDownloadQuality = .original
    ) {
        self.id = id
        self.itemID = itemID
        self.title = title
        self.serverURL = serverURL
        self.userID = userID
        self.fileName = fileName
        self.createdAt = createdAt
        self.byteCount = byteCount
        self.checksumSha256 = checksumSha256
        self.quality = quality
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        id = try values.decode(String.self, forKey: .id)
        itemID = try values.decode(String.self, forKey: .itemID)
        title = try values.decode(String.self, forKey: .title)
        serverURL = try values.decode(String.self, forKey: .serverURL)
        userID = try values.decodeIfPresent(String.self, forKey: .userID) ?? ""
        fileName = try values.decode(String.self, forKey: .fileName)
        createdAt = try values.decode(Date.self, forKey: .createdAt)
        byteCount = try values.decodeIfPresent(Int64.self, forKey: .byteCount)
        checksumSha256 = try values.decodeIfPresent(String.self, forKey: .checksumSha256)
        // Older catalogs predate quality metadata and are Original by definition.
        quality = try values.decodeIfPresent(VeloraDownloadQuality.self, forKey: .quality) ?? .original
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
    public let userID: String
    public let quality: VeloraDownloadQuality

    public init(itemID: String, title: String, serverURL: String, userID: String = "", quality: VeloraDownloadQuality) {
        self.itemID = itemID
        self.title = title
        self.serverURL = serverURL
        self.userID = userID
        self.quality = quality
    }
}

/// Progress reported by Apple's background transfer coordinator. The ETA is
/// nil until the server exposes a meaningful content length and enough bytes
/// have arrived to calculate a stable estimate.
public struct VeloraOfflineTransferProgress: Sendable {
    public let metadata: VeloraOfflineTransferMetadata
    public let fractionCompleted: Double?
    public let bytesWritten: Int64
    public let totalBytesExpected: Int64?
    public let bytesPerSecond: Int64
    public let etaSeconds: Int64?

    public init(
        metadata: VeloraOfflineTransferMetadata,
        fractionCompleted: Double?,
        bytesWritten: Int64,
        totalBytesExpected: Int64?,
        bytesPerSecond: Int64,
        etaSeconds: Int64?
    ) {
        self.metadata = metadata
        self.fractionCompleted = fractionCompleted
        self.bytesWritten = bytesWritten
        self.totalBytesExpected = totalBytesExpected
        self.bytesPerSecond = bytesPerSecond
        self.etaSeconds = etaSeconds
    }
}

public struct VeloraOfflineActiveTransfer: Sendable {
    public let taskIdentifier: Int
    public let metadata: VeloraOfflineTransferMetadata

    public init(taskIdentifier: Int, metadata: VeloraOfflineTransferMetadata) {
        self.taskIdentifier = taskIdentifier
        self.metadata = metadata
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
    private var resumeDataByTaskID: [Int: Data] = [:]
    private var pausedTaskIDs = Set<Int>()
    private var transferStartTimes: [Int: Date] = [:]
    public var onFinished: (@Sendable (VeloraOfflineTransferMetadata, URL, URLResponse) -> Void)?
    public var onFailed: (@Sendable (VeloraOfflineTransferMetadata, Error?) -> Void)?
    public var onProgress: (@Sendable (VeloraOfflineTransferProgress) -> Void)?

    public override init() {
        super.init()
    }

    @discardableResult
    public func enqueue(request: URLRequest, metadata: VeloraOfflineTransferMetadata) -> Int {
        let task = session.downloadTask(with: request)
        if let data = try? JSONEncoder().encode(metadata) {
            task.taskDescription = String(data: data, encoding: .utf8)
        }
        lock.lock()
        transferStartTimes[task.taskIdentifier] = Date()
        lock.unlock()
        task.resume()
        return task.taskIdentifier
    }

    /// Pauses a foreground transfer without deleting its partial download.
    /// URLSession owns the resume data; it is kept in memory only so a token
    /// or signed URL is never written to Velora's catalog.
    public func pause(taskIdentifier: Int, completion: @escaping @Sendable (Bool) -> Void) {
        session.getAllTasks { [weak self] tasks in
            guard let self,
                  let task = tasks.compactMap({ $0 as? URLSessionDownloadTask })
                    .first(where: { $0.taskIdentifier == taskIdentifier }) else {
                completion(false)
                return
            }
            self.lock.lock()
            self.pausedTaskIDs.insert(taskIdentifier)
            self.lock.unlock()
            task.cancel(byProducingResumeData: { [weak self] resumeData in
                guard let self else {
                    completion(false)
                    return
                }
                self.lock.lock()
                if let resumeData { self.resumeDataByTaskID[taskIdentifier] = resumeData }
                self.transferStartTimes.removeValue(forKey: taskIdentifier)
                self.lock.unlock()
                // A pause is still successful when the OS cannot provide
                // resume bytes; resume() will safely restart the request.
                completion(true)
            })
        }
    }

    /// Resumes a paused task using Apple's opaque resume data when available.
    /// A fresh request is used if the OS could not produce resume data.
    @discardableResult
    public func resume(
        taskIdentifier: Int,
        request: URLRequest,
        metadata: VeloraOfflineTransferMetadata
    ) -> Int {
        lock.lock()
        let resumeData = resumeDataByTaskID.removeValue(forKey: taskIdentifier)
        pausedTaskIDs.remove(taskIdentifier)
        lock.unlock()

        let task: URLSessionDownloadTask
        if let resumeData {
            task = session.downloadTask(withResumeData: resumeData)
        } else {
            task = session.downloadTask(with: request)
        }
        if let data = try? JSONEncoder().encode(metadata) {
            task.taskDescription = String(data: data, encoding: .utf8)
        }
        lock.lock()
        transferStartTimes[task.taskIdentifier] = Date()
        lock.unlock()
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

    /// Returns metadata for transfers that survived a process restart. The
    /// task descriptions contain only catalog identifiers and quality, never
    /// credentials or signed media URLs.
    public func activeTransfers() async -> [VeloraOfflineActiveTransfer] {
        await withCheckedContinuation { continuation in
            session.getAllTasks { [weak self] tasks in
                let transfers = tasks.compactMap { task -> VeloraOfflineActiveTransfer? in
                    guard let self, let downloadTask = task as? URLSessionDownloadTask else { return nil }
                    guard let metadata = self.metadata(for: downloadTask) else { return nil }
                    return VeloraOfflineActiveTransfer(taskIdentifier: downloadTask.taskIdentifier, metadata: metadata)
                }
                continuation.resume(returning: transfers)
            }
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
        lock.lock()
        let wasPaused = pausedTaskIDs.contains(downloadTask.taskIdentifier)
        transferStartTimes.removeValue(forKey: downloadTask.taskIdentifier)
        lock.unlock()
        guard !wasPaused else { return }
        onFailed?(metadata, error)
    }

    public func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        guard let metadata = metadata(for: downloadTask) else { return }
        lock.lock()
        let startedAt = transferStartTimes[downloadTask.taskIdentifier] ?? Date()
        lock.unlock()
        let elapsed = max(Date().timeIntervalSince(startedAt), 0.25)
        let speed = Int64(Double(totalBytesWritten) / elapsed)
        let expected = totalBytesExpectedToWrite > 0 ? totalBytesExpectedToWrite : nil
        let remaining = expected.map { max(0, $0 - totalBytesWritten) }
        let eta = speed > 0 ? remaining.map { Int64(ceil(Double($0) / Double(speed))) } : nil
        let fraction = expected.map { min(1, max(0, Double(totalBytesWritten) / Double($0))) }
        onProgress?(VeloraOfflineTransferProgress(
            metadata: metadata,
            fractionCompleted: fraction,
            bytesWritten: totalBytesWritten,
            totalBytesExpected: expected,
            bytesPerSecond: speed,
            etaSeconds: eta
        ))
        _ = bytesWritten
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
           expectedChecksum.caseInsensitiveCompare(checksum) != .orderedSame { return nil }
        return VeloraOfflineDownload(
            id: entry.id,
            itemID: entry.itemID,
            title: entry.title,
            serverURL: entry.serverURL,
            userID: entry.userID,
            fileName: entry.fileName,
            createdAt: entry.createdAt,
            byteCount: size,
            checksumSha256: checksum,
            quality: entry.quality
        )
    }

    @discardableResult
    public func add(mediaAt temporaryURL: URL, itemID: String, title: String, serverURL: String, userID: String = "", quality: VeloraDownloadQuality = .original) throws -> VeloraOfflineDownload {
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
        let entry = VeloraOfflineDownload(itemID: itemID, title: title, serverURL: serverURL, userID: userID, fileName: fileName, quality: quality)
        let verifiedEntry = VeloraOfflineDownload(
            id: entry.id,
            itemID: entry.itemID,
            title: entry.title,
            serverURL: entry.serverURL,
            userID: entry.userID,
            fileName: entry.fileName,
            createdAt: entry.createdAt,
            byteCount: storedByteCount,
            checksumSha256: checksum,
            quality: quality
        )
        entries.removeAll {
            $0.itemID == itemID &&
            normalizedServer($0.serverURL) == normalizedServer(serverURL) &&
            $0.userID == userID &&
            $0.quality == quality
        }
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

    private func normalizedServer(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            .lowercased()
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
