#if canImport(SwiftUI) && canImport(AVKit)
import AVKit
import SwiftUI

/// Shared native application shell. iOS/iPadOS and tvOS apps can embed this
/// shell while supplying their own entry point, focus policy and navigation
/// chrome. Offline actions are deliberately never rendered for tvOS.
@available(iOS 16.0, tvOS 16.0, *)
@MainActor
public final class VeloraAppModel: ObservableObject {
    @Published public private(set) var isAuthenticated = false
    @Published public private(set) var isLoading = false
    @Published public private(set) var items: [JellyfinItem] = []
    @Published public private(set) var liveTvChannels: [JellyfinLiveTvChannel] = []
    @Published public private(set) var offlineDownloads: [VeloraOfflineDownload] = []
    @Published public private(set) var downloadingItemID: String?
    @Published public var errorMessage: String?
    @Published public var settings: VeloraSettings {
        didSet { settingsStore.save(settings) }
    }

    public let platform: VeloraPlatform
    private let client: JellyfinClient
    private let settingsStore: VeloraSettingsStore
    private let credentialStore: VeloraCredentialStore
    private let offlineStore: VeloraOfflineStore
    private let offlineTransfer: VeloraOfflineTransferCoordinator
    private let serverDefaults: UserDefaults
    private var session: JellyfinSession?

    public var jellyfinClient: JellyfinClient { client }

    public init(platform: VeloraPlatform, serverURL: URL, credentialStore: VeloraCredentialStore = VeloraCredentialStore(), serverDefaults: UserDefaults = .standard) throws {
        self.platform = platform
        self.settingsStore = VeloraSettingsStore()
        self.settings = settingsStore.load() ?? VeloraSettings.systemDefault()
        self.credentialStore = credentialStore
        self.offlineStore = VeloraOfflineStore()
        self.offlineTransfer = platform.supportsOfflineDownloads
            ? VeloraOfflineTransferCoordinator.shared
            : VeloraOfflineTransferCoordinator()
        self.serverDefaults = serverDefaults
        let configuredServer = serverDefaults.string(forKey: "velora.serverURL")
            .flatMap(URL.init(string:)) ?? serverURL
        let storedSession = credentialStore.load()
        // A legacy session without a server association is not restored: a
        // token must never be sent to an unknown Jellyfin server after an
        // upgrade. The user can sign in again and create an associated session.
        let restoredSession = storedSession?.serverURL == configuredServer.absoluteString
            ? storedSession
            : nil
        self.client = try JellyfinClient(serverURL: configuredServer, restoredSession: restoredSession)
        self.session = restoredSession
        self.isAuthenticated = restoredSession != nil
        self.offlineDownloads = platform.supportsOfflineDownloads
            ? offlineStore.load().filter { $0.serverURL == configuredServer.absoluteString }
            : []
        if platform.supportsOfflineDownloads {
            offlineTransfer.onFinished = { [weak self] metadata, temporaryURL, response in
                Task { @MainActor [weak self] in
                    await self?.completeBackgroundDownload(metadata: metadata, temporaryURL: temporaryURL, response: response)
                }
            }
            offlineTransfer.onFailed = { [weak self] metadata, _ in
                Task { @MainActor [weak self] in
                    self?.downloadingItemID = nil
                    self?.errorMessage = String(localized: "Unable to download", bundle: .module)
                    _ = metadata
                }
            }
        }
    }

    public func signIn(serverURL: String, username: String, password: String) async {
        do {
            guard let url = URL(string: serverURL.trimmingCharacters(in: .whitespacesAndNewlines)) else {
                throw JellyfinClient.ClientError.invalidServerURL
            }
            try await client.setServerURL(url)
            serverDefaults.set(url.absoluteString, forKey: "velora.serverURL")
            let authenticated = try await client.authenticate(username: username, password: password)
            session = authenticated
            credentialStore.save(authenticated)
            await refreshContent()
            isAuthenticated = true
            errorMessage = nil
        } catch {
            isAuthenticated = false
            errorMessage = String(localized: "Unable to sign in", bundle: .module)
        }
    }

    /// Reload server-backed content for both a fresh and a restored session.
    /// Restored credentials must never leave the library in an empty state.
    public func refreshContent() async {
        guard let session else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            async let library = client.items(userID: session.userID, includeTypes: ["Movie", "Series"])
            async let channels = client.liveTvChannels(userID: session.userID)
            items = try await library
            liveTvChannels = JellyfinLiveTvChannel.grouped((try? await channels) ?? [])
            let serverURL = (await client.serverURL()).absoluteString
            offlineDownloads = platform.supportsOfflineDownloads
                ? offlineStore.load().filter { $0.serverURL == serverURL }
                : []
            errorMessage = nil
        } catch {
            errorMessage = String(localized: "Unable to load library", bundle: .module)
        }
    }

    public func signOut() async {
        await client.setAccessToken(nil)
        credentialStore.remove()
        session = nil
        items = []
        liveTvChannels = []
        offlineDownloads = []
        isAuthenticated = false
    }

    public func play(_ item: JellyfinItem) async -> AVPlayer? {
        let configuredServer = (await client.serverURL()).absoluteString
        if let offline = offlineDownloads.first(where: { $0.itemID == item.id && $0.serverURL == configuredServer }),
           let verified = offlineStore.verifyIntegrity(offline) {
            let player = AVPlayer(url: offlineStore.mediaURL(for: verified))
            let playerItem = player.currentItem
            Task { @MainActor [weak self, weak playerItem] in
                guard let self, let playerItem else { return }
                await self.applyMediaPreferences(to: playerItem)
            }
            return player
        }
        let requestURL = await client.playbackURL(
            itemID: item.id,
            userID: session?.userID ?? ""
        ) ?? await client.videoURL(itemID: item.id)
        guard let requestURL else { return nil }
        let request = await client.authorizedRequest(for: requestURL)
        guard let url = request.url else { return nil }
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": request.allHTTPHeaderFields ?? [:]])
        let playerItem = AVPlayerItem(asset: asset)
        let player = AVPlayer(playerItem: playerItem)
        Task { @MainActor [weak self, weak playerItem] in
            guard let self, let playerItem else { return }
            await self.applyMediaPreferences(to: playerItem)
        }
        return player
    }

    /// Apply device-local audio and subtitle preferences to Apple's native
    /// media selection groups without modifying the server's source decision.
    private func applyMediaPreferences(to playerItem: AVPlayerItem) async {
        let asset = playerItem.asset
        let preferredAudio = settings.preferredAudioLanguage?.lowercased()
        let preferredSubtitle = settings.preferredSubtitleLanguage?.lowercased()

        if let group = try? await asset.loadMediaSelectionGroup(for: .audible),
           let preferredAudio,
           let option = group.options.first(where: { option in
               option.locale?.languageCode?.lowercased() == preferredAudio
           }) {
            playerItem.select(option, in: group)
        }

        guard let group = try? await asset.loadMediaSelectionGroup(for: .legible) else { return }
        switch settings.subtitlePreference {
        case .off:
            playerItem.select(nil, in: group)
        case .forced:
            let option = group.options.first(where: { $0.hasMediaCharacteristic(.containsOnlyForcedSubtitles) })
                ?? group.options.first(where: { option in
                    option.locale?.languageCode?.lowercased() == preferredSubtitle
                        && option.hasMediaCharacteristic(.containsOnlyForcedSubtitles)
                })
            playerItem.select(option, in: group)
        case .preferred:
            let option = group.options.first(where: { option in
                option.locale?.languageCode?.lowercased() == preferredSubtitle
            })
            playerItem.select(option, in: group)
        case .automatic:
            if let preferredSubtitle,
               let option = group.options.first(where: { option in
                   option.locale?.languageCode?.lowercased() == preferredSubtitle
               }) {
                playerItem.select(option, in: group)
            }
        }
    }

    public func download(_ item: JellyfinItem, quality: VeloraDownloadQuality = .original) async {
        guard platform.supportsOfflineDownloads, downloadingItemID == nil else { return }
        guard let requestURL = await client.videoURL(itemID: item.id, quality: quality) else { return }
        let serverURL = await client.serverURL()
        guard !offlineDownloads.contains(where: { $0.itemID == item.id && $0.serverURL == serverURL.absoluteString }) else { return }
        downloadingItemID = item.id
        let request = await client.authorizedRequest(for: requestURL)
        let metadata = VeloraOfflineTransferMetadata(
            itemID: item.id,
            title: item.name,
            serverURL: serverURL.absoluteString,
            quality: quality
        )
        _ = offlineTransfer.enqueue(request: request, metadata: metadata)
    }

    private func completeBackgroundDownload(
        metadata: VeloraOfflineTransferMetadata,
        temporaryURL: URL,
        response: URLResponse
    ) async {
        downloadingItemID = nil
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            errorMessage = String(localized: "Unable to download", bundle: .module)
            return
        }
        do {
            let entry = try offlineStore.add(
                mediaAt: temporaryURL,
                itemID: metadata.itemID,
                title: metadata.title,
                serverURL: metadata.serverURL
            )
            let entries = offlineStore.load().filter { $0.serverURL == metadata.serverURL }
            offlineDownloads = entries.contains(entry) ? entries : entries + [entry]
            errorMessage = nil
        } catch VeloraOfflineStoreError.insufficientStorage {
            errorMessage = String(localized: "Not enough storage", bundle: .module)
        } catch {
            errorMessage = String(localized: "Unable to download", bundle: .module)
        }
    }

    public func removeDownload(for item: JellyfinItem) async {
        let configuredServer = (await client.serverURL()).absoluteString
        guard let entry = offlineDownloads.first(where: { $0.itemID == item.id && $0.serverURL == configuredServer }) else { return }
        do {
            try offlineStore.remove(entry)
            offlineDownloads = offlineStore.load().filter { $0.serverURL == configuredServer }
        } catch {
            errorMessage = String(localized: "Unable to remove download", bundle: .module)
        }
    }

    public func playLiveTv(channel: JellyfinLiveTvChannel, sourceID: String? = nil) async -> AVPlayer? {
        guard let session else { return nil }
        let requestURL: URL?
        do {
            requestURL = try await client.liveTvPlaybackURL(userID: session.userID, channelID: channel.id, mediaSourceID: sourceID)
        } catch {
            return nil
        }
        guard let requestURL else { return nil }
        let request = await client.authorizedRequest(for: requestURL)
        guard let url = request.url else { return nil }
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": request.allHTTPHeaderFields ?? [:]])
        let playerItem = AVPlayerItem(asset: asset)
        let player = AVPlayer(playerItem: playerItem)
        Task { @MainActor [weak self, weak playerItem] in
            guard let self, let playerItem else { return }
            await self.applyMediaPreferences(to: playerItem)
        }
        return player
    }

    public func stopLiveTv(channel: JellyfinLiveTvChannel, positionSeconds: Double = 0) async {
        await client.reportPlaybackStopped(
            itemID: channel.id,
            positionTicks: Int64(max(0, positionSeconds) * 10_000_000)
        )
    }
}

@available(iOS 16.0, tvOS 16.0, *)
public struct VeloraAppShell: View {
    @StateObject private var model: VeloraAppModel
    @State private var serverText: String
    @State private var username = ""
    @State private var password = ""
    @State private var selectedItem: JellyfinItem?

    public init(platform: VeloraPlatform, serverURL: URL = URL(string: "http://jellyfin.local:8096")!) throws {
        let savedServer = UserDefaults.standard.string(forKey: "velora.serverURL")
            .flatMap(URL.init(string:)) ?? serverURL
        let model = try VeloraAppModel(platform: platform, serverURL: savedServer)
        _model = StateObject(wrappedValue: model)
        _serverText = State(initialValue: savedServer.absoluteString)
    }

    public var body: some View {
        Group {
            if model.isAuthenticated {
                NavigationStack {
                    VeloraLibraryView(title: String(localized: "Library", bundle: .module), items: model.items, artworkClient: model.jellyfinClient) { item in
                        selectedItem = item
                    }
                    .toolbar {
                        ToolbarItem(placement: .automatic) {
                            NavigationLink {
                                VeloraSettingsView(settings: $model.settings)
                            } label: {
                                Text("Settings", bundle: .module)
                            }
                        }
                        if !model.liveTvChannels.isEmpty {
                            ToolbarItem(placement: .automatic) {
                                NavigationLink {
                                    VeloraLiveTvView(model: model)
                                } label: {
                                    Text("Live TV", bundle: .module)
                                }
                            }
                        }
                    }
                }
                .sheet(item: $selectedItem) { item in
                    NavigationStack { VeloraItemDetailView(item: item, model: model) }
                }
            } else {
                VeloraLoginView(
                    server: $serverText,
                    username: $username,
                    password: $password,
                    errorMessage: model.errorMessage,
                    onSignIn: { await model.signIn(serverURL: serverText, username: username, password: password) }
                )
            }
        }
        .environment(\.locale, model.settings.appLocale)
        .task(id: model.isAuthenticated) {
            if model.isAuthenticated { await model.refreshContent() }
        }
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraLoginView: View {
    @Binding var server: String
    @Binding var username: String
    @Binding var password: String
    let errorMessage: String?
    let onSignIn: () async -> Void

    var body: some View {
        Form {
            Section {
                TextField(text: $server) {
                    Text("Server address", bundle: .module)
                }
                TextField(text: $username) {
                    Text("Username", bundle: .module)
                }
                SecureField(text: $password) {
                    Text("Password", bundle: .module)
                }
                Button { Task { await onSignIn() } } label: {
                    Text("Sign in", bundle: .module)
                }
                    .disabled(server.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || username.isEmpty || password.isEmpty)
                if let errorMessage { Text(errorMessage).foregroundStyle(.red) }
            } header: {
                Text("Connect to Jellyfin", bundle: .module)
            }
        }
        .navigationTitle(Text("Sign in", bundle: .module))
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraItemDetailView: View {
    let item: JellyfinItem
    @ObservedObject var model: VeloraAppModel
    @State private var player: AVPlayer?
    @State private var downloadQuality: VeloraDownloadQuality = .original

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(item.name).font(.largeTitle.bold())
                if let overview = item.overview, !overview.isEmpty { Text(overview) }
                Button {
                    Task { player = await model.play(item); player?.play() }
                } label: {
                    Text("Play", bundle: .module)
                }
                if let player { VideoPlayer(player: player).aspectRatio(16 / 9, contentMode: .fit) }
                if model.platform.supportsOfflineDownloads {
                    if model.offlineDownloads.contains(where: { $0.itemID == item.id }) {
                        Button {
                            Task { await model.removeDownload(for: item) }
                        } label: {
                            Label {
                                Text("Remove download", bundle: .module)
                            } icon: {
                                Image(systemName: "trash")
                            }
                        }
                    } else {
                        Picker(String(localized: "Download quality", bundle: .module), selection: $downloadQuality) {
                            Text("Original", bundle: .module).tag(VeloraDownloadQuality.original)
                            Text("High", bundle: .module).tag(VeloraDownloadQuality.high)
                            Text("Medium", bundle: .module).tag(VeloraDownloadQuality.medium)
                            Text("Low", bundle: .module).tag(VeloraDownloadQuality.low)
                        }
                        .pickerStyle(.menu)
                        Button {
                            Task { await model.download(item, quality: downloadQuality) }
                        } label: {
                            Label {
                                Text("Download", bundle: .module)
                            } icon: {
                                Image(systemName: "arrow.down.circle")
                            }
                        }
                        .disabled(model.downloadingItemID != nil)
                    }
                }
            }
            .padding()
        }
        .navigationTitle(item.name)
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraLiveTvView: View {
    @ObservedObject var model: VeloraAppModel
    @State private var player: AVPlayer?
    @State private var selectedChannel: JellyfinLiveTvChannel?
    @State private var playbackTask: Task<Void, Never>?

    private func startPlayback(channel: JellyfinLiveTvChannel, sourceID: String? = nil) {
        let previousChannel = selectedChannel
        playbackTask?.cancel()
        playbackTask = Task { @MainActor in
            if let previousChannel, previousChannel.id != channel.id {
                await model.stopLiveTv(
                    channel: previousChannel,
                    positionSeconds: player?.currentTime().seconds ?? 0
                )
            }
            guard !Task.isCancelled else { return }
            let nextPlayer = await model.playLiveTv(channel: channel, sourceID: sourceID)
            guard !Task.isCancelled else {
                nextPlayer?.pause()
                return
            }
            player?.pause()
            player = nextPlayer
            selectedChannel = channel
            player?.play()
        }
    }

    var body: some View {
        List(model.liveTvChannels) { channel in
            Button {
                startPlayback(channel: channel)
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text((channel.number.map { "\($0) · " } ?? "") + channel.name)
                        .font(.headline)
                    if let program = channel.currentProgram {
                        Text(program.name)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .buttonStyle(.plain)
                    .accessibilityHint(String(localized: "Play live channel", bundle: .module))
            if channel.mediaSources.count > 1 {
                Menu {
                    ForEach(Array(channel.mediaSources.enumerated()), id: \.offset) { index, source in
                        Button {
                            startPlayback(channel: channel, sourceID: source.id ?? source.liveStreamID)
                        } label: {
                            Text(source.id ?? source.liveStreamID ?? "Source \(index + 1)")
                        }
                    }
                } label: {
                    Image(systemName: "rectangle.stack")
                }
                .accessibilityLabel("Choose channel source")
            }
        }
        .navigationTitle(Text("Live TV", bundle: .module))
        .onDisappear {
            playbackTask?.cancel()
            let activeChannel = selectedChannel
            let position = player?.currentTime().seconds ?? 0
            player?.pause()
            player = nil
            selectedChannel = nil
            if let activeChannel {
                Task { await model.stopLiveTv(channel: activeChannel, positionSeconds: position) }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let player, let selectedChannel {
                VStack(alignment: .leading, spacing: 8) {
                    Text(selectedChannel.name).font(.headline)
                    VideoPlayer(player: player)
                        .aspectRatio(16 / 9, contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding()
                .background(.regularMaterial)
            }
        }
    }
}
#endif
