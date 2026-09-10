#if canImport(SwiftUI) && canImport(AVKit)
import AVKit
import CoreMedia
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
    @Published public private(set) var totalItemCount: Int?
    @Published public private(set) var isLoadingMoreItems = false
    @Published public private(set) var hasMoreItems = true
    @Published public private(set) var liveTvChannels: [JellyfinLiveTvChannel] = []
    @Published public private(set) var liveTvPrograms: [JellyfinLiveTvProgram] = []
    @Published public private(set) var offlineDownloads: [VeloraOfflineDownload] = []
    @Published public private(set) var downloadingItemID: String?
    @Published public private(set) var offlineTransferProgress: VeloraOfflineTransferProgress?
    @Published public private(set) var offlineTransferPaused = false
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
    private var liveTvProgramTask: Task<Void, Never>?
    private var activeOfflineTaskID: Int?
    private var activeOfflineMetadata: VeloraOfflineTransferMetadata?

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
            ? offlineStore.load().filter { $0.serverURL == configuredServer.absoluteString && $0.userID == restoredSession?.userID }
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
                    self?.activeOfflineTaskID = nil
                    self?.activeOfflineMetadata = nil
                    self?.offlineTransferProgress = nil
                    self?.offlineTransferPaused = false
                    self?.errorMessage = String(localized: "Unable to download", bundle: .module)
                    _ = metadata
                }
            }
            offlineTransfer.onProgress = { [weak self] progress in
                Task { @MainActor [weak self] in
                    self?.offlineTransferProgress = progress
                }
            }
            Task { @MainActor [weak self] in
                guard let self,
                      let active = await self.offlineTransfer.activeTransfers().first else { return }
                self.downloadingItemID = active.metadata.itemID
                self.activeOfflineTaskID = active.taskIdentifier
                self.activeOfflineMetadata = active.metadata
                self.offlineTransferPaused = false
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
            let authenticated = try await client.authenticate(
                username: username,
                password: password,
                languageIdentifier: settings.languageIdentifier
            )
            session = authenticated
            credentialStore.save(authenticated)
            await refreshContent()
            isAuthenticated = true
            errorMessage = nil
        } catch let error as JellyfinClient.ClientError {
            isAuthenticated = false
            switch error {
            case .unauthorized:
                errorMessage = String(localized: "Invalid credentials", bundle: .module)
            case .invalidServerURL:
                errorMessage = String(localized: "Invalid server address", bundle: .module)
            case .invalidResponse:
                errorMessage = String(localized: "Unable to sign in", bundle: .module)
            }
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
            async let library = client.itemsPage(userID: session.userID, includeTypes: ["Movie", "Series"])
            async let channels = client.liveTvChannels(userID: session.userID)
            let libraryPage = try await library
            let loadedChannels = JellyfinLiveTvChannel.grouped((try? await channels) ?? [])
            items = libraryPage.items
            totalItemCount = libraryPage.totalRecordCount
            if let total = libraryPage.totalRecordCount {
                hasMoreItems = libraryPage.items.count < total
            } else {
                hasMoreItems = libraryPage.items.count >= 100
            }
            liveTvChannels = loadedChannels
            liveTvProgramTask?.cancel()
            liveTvProgramTask = nil
            liveTvPrograms = []
            if !loadedChannels.isEmpty {
                let channelIDs = loadedChannels.map(\.id)
                let userID = session.userID
                let now = Date()
                let jellyfinClient = client
                liveTvProgramTask = Task { [weak self] in
                    let programs = (try? await jellyfinClient.liveTvPrograms(
                        userID: userID,
                        channelIDs: channelIDs,
                        from: now,
                        until: now.addingTimeInterval(6 * 60 * 60)
                    )) ?? []
                    guard !Task.isCancelled else { return }
                    self?.liveTvPrograms = programs
                }
            }
            let serverURL = (await client.serverURL()).absoluteString
            offlineDownloads = platform.supportsOfflineDownloads
                ? offlineStore.load().filter { $0.serverURL == serverURL && $0.userID == session.userID }
                : []
            errorMessage = nil
        } catch {
            errorMessage = String(localized: "Unable to load library", bundle: .module)
        }
    }

    /// Fetches the next library page only when the user reaches the end of the
    /// grid. This keeps initial rendering responsive without hiding catalog
    /// items behind an arbitrary cap.
    public func loadMoreItems() async {
        guard let session,
              !isLoadingMoreItems,
              hasMoreItems else { return }
        isLoadingMoreItems = true
        defer { isLoadingMoreItems = false }
        do {
            let page = try await client.itemsPage(
                userID: session.userID,
                includeTypes: ["Movie", "Series"],
                startIndex: items.count
            )
            let existingIDs = Set(items.map(\.id))
            let newItems = page.items.filter { !existingIDs.contains($0.id) }
            items.append(contentsOf: newItems)
            if let total = page.totalRecordCount { self.totalItemCount = total }
            if let total = page.totalRecordCount {
                hasMoreItems = !page.items.isEmpty && !newItems.isEmpty && items.count < total
            } else {
                hasMoreItems = !page.items.isEmpty && !newItems.isEmpty && page.items.count >= 100
            }
        } catch {
            errorMessage = String(localized: "Unable to load library", bundle: .module)
        }
    }

    public func signOut() async {
        await client.setAccessToken(nil)
        credentialStore.remove()
        session = nil
        items = []
        totalItemCount = nil
        isLoadingMoreItems = false
        hasMoreItems = false
        liveTvChannels = []
        liveTvProgramTask?.cancel()
        liveTvProgramTask = nil
        liveTvPrograms = []
        offlineDownloads = []
        isAuthenticated = false
    }

    public func filmography(for person: JellyfinPerson) async throws -> [JellyfinItem] {
        guard let personID = person.id else { return [] }
        guard let session else { return [] }
        return try await client.items(userID: session.userID, forPerson: personID)
    }

    public func trailers(for item: JellyfinItem) async -> [JellyfinItem] {
        guard let session else { return [] }
        return (try? await client.trailers(for: item.id, userID: session.userID)) ?? []
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
        let requestURL: URL?
        if let playbackURL = await client.playbackURL(
            itemID: item.id,
            userID: session?.userID ?? ""
        ) {
            requestURL = playbackURL
        } else {
            requestURL = await client.videoURL(itemID: item.id)
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

    public func themeMusicPlayer(for item: JellyfinItem) async -> AVPlayer? {
        guard settings.themeMusicEnabled, let session,
              let requestURL = await client.themeSongURL(itemID: item.id, userID: session.userID) else { return nil }
        let request = await client.authorizedRequest(for: requestURL)
        guard let url = request.url else { return nil }
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": request.allHTTPHeaderFields ?? [:]])
        let player = AVPlayer(playerItem: AVPlayerItem(asset: asset))
        player.volume = Float(settings.themeMusicVolume)
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
        guard platform.supportsOfflineDownloads, downloadingItemID == nil, let session else { return }
        guard let requestURL = await client.videoURL(itemID: item.id, quality: quality) else { return }
        let serverURL = await client.serverURL()
        guard !offlineDownloads.contains(where: { $0.itemID == item.id && $0.serverURL == serverURL.absoluteString && $0.userID == session.userID && $0.quality == quality }) else { return }
        downloadingItemID = item.id
        let request = await client.authorizedRequest(for: requestURL)
        let metadata = VeloraOfflineTransferMetadata(
            itemID: item.id,
            title: item.name,
            serverURL: serverURL.absoluteString,
            userID: session.userID,
            quality: quality
        )
        activeOfflineMetadata = metadata
        offlineTransferPaused = false
        activeOfflineTaskID = offlineTransfer.enqueue(request: request, metadata: metadata)
    }

    /// Pause and resume are available only for the mobile Apple targets that
    /// expose managed offline downloads. tvOS never reaches these methods.
    public func pauseDownload() {
        guard platform.supportsOfflineDownloads, let taskID = activeOfflineTaskID else { return }
        offlineTransfer.pause(taskIdentifier: taskID) { [weak self] didPause in
            guard didPause else { return }
            Task { @MainActor [weak self] in
                self?.offlineTransferProgress = nil
                self?.offlineTransferPaused = true
            }
        }
    }

    public func resumeDownload() async {
        guard platform.supportsOfflineDownloads,
              let taskID = activeOfflineTaskID,
              let metadata = activeOfflineMetadata,
              let requestURL = await client.videoURL(itemID: metadata.itemID, quality: metadata.quality) else { return }
        let request = await client.authorizedRequest(for: requestURL)
        activeOfflineTaskID = offlineTransfer.resume(taskIdentifier: taskID, request: request, metadata: metadata)
        offlineTransferPaused = false
    }

    private func completeBackgroundDownload(
        metadata: VeloraOfflineTransferMetadata,
        temporaryURL: URL,
        response: URLResponse
    ) async {
        downloadingItemID = nil
        activeOfflineTaskID = nil
        activeOfflineMetadata = nil
        offlineTransferProgress = nil
        offlineTransferPaused = false
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            errorMessage = String(localized: "Unable to download", bundle: .module)
            return
        }
        do {
            let entry = try offlineStore.add(
                mediaAt: temporaryURL,
                itemID: metadata.itemID,
                title: metadata.title,
                serverURL: metadata.serverURL,
                userID: metadata.userID,
                quality: metadata.quality
            )
            let entries = offlineStore.load().filter { $0.serverURL == metadata.serverURL && $0.userID == metadata.userID }
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
        guard let entry = offlineDownloads.first(where: { $0.itemID == item.id && $0.serverURL == configuredServer && $0.userID == session?.userID }) else { return }
        do {
            try offlineStore.remove(entry)
            offlineDownloads = offlineStore.load().filter { $0.serverURL == configuredServer && $0.userID == session?.userID }
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
                VeloraAuthenticatedContent(model: model, selectedItem: $selectedItem)
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

/// Platform-adaptive top-level navigation. iPhone/iPad get the same clearly
/// reachable library sections as the Android client, while tvOS uses the same
/// focusable tabs without exposing mobile-only offline actions.
@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraAuthenticatedContent: View {
    @ObservedObject var model: VeloraAppModel
    @Binding var selectedItem: JellyfinItem?
    @State private var selectedSection: Section = .home

    private enum Section: Hashable {
        case home, movies, series, liveTV
    }

    private var movies: [JellyfinItem] {
        model.items.filter { $0.type?.caseInsensitiveCompare("Movie") == .orderedSame }
    }

    private var series: [JellyfinItem] {
        model.items.filter { $0.type?.caseInsensitiveCompare("Series") == .orderedSame }
    }

    var body: some View {
        TabView(selection: $selectedSection) {
            library(
                title: String(localized: "Home", bundle: .module),
                items: model.items
            )
            .tabItem {
                Label("Home", systemImage: "house")
            }
            .tag(Section.home)

            library(
                title: String(localized: "Movies", bundle: .module),
                items: movies
            )
            .tabItem {
                Label("Movies", systemImage: "film")
            }
            .tag(Section.movies)

            library(
                title: String(localized: "Series", bundle: .module),
                items: series
            )
            .tabItem {
                Label("Series", systemImage: "rectangle.stack")
            }
            .tag(Section.series)

            if !model.liveTvChannels.isEmpty {
                NavigationStack {
                    VeloraLiveTvView(model: model)
                        .toolbar { settingsToolbar }
                }
                .tabItem {
                    Label("Live TV", systemImage: "tv")
                }
                .tag(Section.liveTV)
            }
        }
        .sheet(item: $selectedItem) { item in
            NavigationStack { VeloraItemDetailView(item: item, model: model) }
        }
    }

    @ViewBuilder
    private func library(title: String, items: [JellyfinItem]) -> some View {
        NavigationStack {
            VeloraLibraryView(
                title: title,
                items: items,
                artworkClient: model.jellyfinClient,
                onReachEnd: {
                    if selectedSection == .home { Task { await model.loadMoreItems() } }
                },
                onSelect: { selectedItem = $0 }
            )
            .toolbar { settingsToolbar }
        }
    }

    @ToolbarContentBuilder
    private var settingsToolbar: some ToolbarContent {
        ToolbarItem(placement: .automatic) {
            NavigationLink {
                VeloraSettingsView(settings: $model.settings)
            } label: {
                Label("Settings", systemImage: "gearshape")
            }
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
    @State private var isPlayerFullscreen = false
    @State private var downloadQuality: VeloraDownloadQuality = .original
    @State private var aspectMode: VeloraAspectMode = .fit
    @State private var themePlayer: AVPlayer?
    @State private var resumePositionSeconds: Double?
    @State private var showResumePrompt = false
    @State private var trailers: [JellyfinItem] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(item.name).font(.largeTitle.bold())
                if let year = item.productionYear { Text(String(year)).foregroundStyle(.secondary) }
                if let overview = item.overview, !overview.isEmpty { Text(overview) }
                Button {
                    Task {
                        let nextPlayer = await model.play(item)
                        player = nextPlayer
                        guard let nextPlayer else { return }
                        let ticks = item.userData?.playbackPositionTicks ?? 0
                        let seconds = Double(ticks) / 10_000_000
                        if item.userData?.played != true, seconds >= 30 {
                            resumePositionSeconds = seconds
                            showResumePrompt = true
                        } else {
                            nextPlayer.play()
                        }
                    }
                } label: {
                    Text("Play", bundle: .module)
                }
                if let trailer = trailers.first {
                    Button {
                        Task {
                            player = await model.play(trailer)
                            player?.play()
                        }
                    } label: {
                        Label {
                            Text("Trailer", bundle: .module)
                        } icon: {
                            Image(systemName: "play.rectangle")
                        }
                    }
                }
                if let player {
                    ZStack(alignment: .topTrailing) {
                        VideoPlayer(player: player)
                            .veloraVideoAspect(aspectMode)
                        Button {
                            isPlayerFullscreen = true
                        } label: {
                            Label {
                                Text("Fullscreen", bundle: .module)
                            } icon: {
                                Image(systemName: "arrow.up.left.and.arrow.down.right")
                            }
                            .labelStyle(.iconOnly)
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityLabel(Text("Fullscreen", bundle: .module))
                        .padding(10)
                    }
                    .fullScreenCover(isPresented: $isPlayerFullscreen) {
                        VeloraFullscreenPlayer(
                            player: player,
                            isPresented: $isPlayerFullscreen,
                            aspectMode: $aspectMode
                        )
                    }
                    HStack {
                        VeloraAspectMenu(selection: $aspectMode)
                        VeloraMediaSelectionMenu(player: player)
                    }
                }
                if let people = item.people, !people.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Cast", bundle: .module).font(.title2.bold())
                        ForEach(Array(people.prefix(12).enumerated()), id: \.offset) { _, person in
                            if person.id != nil {
                                NavigationLink {
                                    VeloraFilmographyView(person: person, model: model)
                                } label: {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(person.name)
                                        if let role = person.role, !role.isEmpty {
                                            Text(role).font(.caption).foregroundStyle(.secondary)
                                        }
                                    }
                                }
                            } else {
                                Text(person.name)
                            }
                        }
                    }
                }
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
                        if model.downloadingItemID == item.id {
                            if let progress = model.offlineTransferProgress {
                                ProgressView(value: progress.fractionCompleted)
                                if let expected = progress.totalBytesExpected {
                                    Text("\(progress.bytesWritten) / \(expected) bytes")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            Button {
                                if model.offlineTransferPaused {
                                    Task { await model.resumeDownload() }
                                } else {
                                    model.pauseDownload()
                                }
                            } label: {
                                Label {
                                    Text(model.offlineTransferPaused ? "Resume download" : "Pause download", bundle: .module)
                                } icon: {
                                    Image(systemName: model.offlineTransferPaused ? "play.fill" : "pause.fill")
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
            }
            .padding()
        }
        .navigationTitle(item.name)
        .alert(Text("Resume playback?", bundle: .module), isPresented: $showResumePrompt) {
            Button(action: {
                seekAndPlay(to: resumePositionSeconds ?? 0)
            }) {
                Text("Resume", bundle: .module)
            }
            Button(action: {
                seekAndPlay(to: 0)
            }) {
                Text("Start over", bundle: .module)
            }
            Button(role: .cancel, action: {
                player?.pause()
                player = nil
                resumePositionSeconds = nil
            }) {
                Text("Cancel", bundle: .module)
            }
        } message: {
            Text("Continue where you left off?", bundle: .module)
        }
        .task(id: item.id) {
            guard model.settings.themeMusicEnabled else { return }
            do {
                try await Task.sleep(nanoseconds: 700_000_000)
            } catch {
                return
            }
            guard !Task.isCancelled else { return }
            guard let nextPlayer = await model.themeMusicPlayer(for: item) else { return }
            guard !Task.isCancelled else {
                nextPlayer.pause()
                return
            }
            nextPlayer.volume = 0
            themePlayer = nextPlayer
            nextPlayer.play()
            for step in 1...8 {
                do {
                    try await Task.sleep(nanoseconds: 50_000_000)
                } catch {
                    return
                }
                guard !Task.isCancelled else {
                    nextPlayer.pause()
                    return
                }
                nextPlayer.volume = Float(step) / 8
            }
        }
        .task {
            trailers = await model.trailers(for: item)
        }
        .onDisappear {
            if let player {
                let position = player.currentTime().seconds
                if position.isFinite, position >= 0 {
                    Task {
                        await model.jellyfinClient.reportPlaybackStopped(
                            itemID: item.id,
                            positionTicks: Int64(position * 10_000_000)
                        )
                    }
                }
            }
            if let outgoingThemePlayer = themePlayer {
                Task { await fadeOutThemeMusic(outgoingThemePlayer) }
            }
            themePlayer = nil
        }
    }

    private func fadeOutThemeMusic(_ player: AVPlayer) async {
        for step in stride(from: 8, through: 0, by: -1) {
            player.volume = Float(step) / 8
            if step > 0 {
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
        player.pause()
    }

    private func seekAndPlay(to seconds: Double) {
        guard let player else { return }
        player.seek(to: CMTime(seconds: max(0, seconds), preferredTimescale: 600))
        player.play()
        resumePositionSeconds = nil
    }
}

private enum VeloraAspectMode: String, CaseIterable, Identifiable {
    case fit
    case fill
    case original

    var id: String { rawValue }

}

private extension View {
    @ViewBuilder
    func veloraVideoAspect(_ mode: VeloraAspectMode) -> some View {
        switch mode {
        case .fit:
            self.aspectRatio(16 / 9, contentMode: .fit)
        case .fill:
            self.aspectRatio(16 / 9, contentMode: .fill).clipped()
        case .original:
            self.aspectRatio(contentMode: .fit)
        }
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraAspectMenu: View {
    @Binding var selection: VeloraAspectMode

    var body: some View {
        Menu {
            Picker(String(localized: "Aspect ratio", bundle: .module), selection: $selection) {
                Text("Fit", bundle: .module).tag(VeloraAspectMode.fit)
                Text("Fill", bundle: .module).tag(VeloraAspectMode.fill)
                Text("Original", bundle: .module).tag(VeloraAspectMode.original)
            }
        } label: {
            Label {
                Text("Aspect ratio", bundle: .module)
            } icon: {
                Image(systemName: "rectangle.arrowtriangle.2.outward")
            }
        }
        .accessibilityLabel(Text("Aspect ratio", bundle: .module))
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraMediaSelectionMenu: View {
    let player: AVPlayer
    @State private var audioGroup: AVMediaSelectionGroup?
    @State private var subtitleGroup: AVMediaSelectionGroup?

    var body: some View {
        Menu {
            if let audioGroup, !audioGroup.options.isEmpty {
                Section {
                    ForEach(Array(audioGroup.options.enumerated()), id: \.offset) { _, option in
                        Button(option.displayName) {
                            player.currentItem?.select(option, in: audioGroup)
                        }
                    }
                } header: {
                    Text("Audio track", bundle: .module)
                }
            }
            if let subtitleGroup {
                Section {
                    Button {
                        player.currentItem?.select(nil, in: subtitleGroup)
                    } label: {
                        Text("Subtitles off", bundle: .module)
                    }
                    ForEach(Array(subtitleGroup.options.enumerated()), id: \.offset) { _, option in
                        Button(option.displayName) {
                            player.currentItem?.select(option, in: subtitleGroup)
                        }
                    }
                } header: {
                    Text("Subtitles", bundle: .module)
                }
            }
        } label: {
            Label {
                Text("Player settings", bundle: .module)
            } icon: {
                Image(systemName: "gearshape")
            }
        }
        .accessibilityLabel(Text("Player settings", bundle: .module))
        .task {
            guard let item = player.currentItem else { return }
            audioGroup = try? await item.asset.loadMediaSelectionGroup(for: .audible)
            subtitleGroup = try? await item.asset.loadMediaSelectionGroup(for: .legible)
        }
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraFullscreenPlayer: View {
    let player: AVPlayer
    @Binding var isPresented: Bool
    @Binding var aspectMode: VeloraAspectMode

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()
            VideoPlayer(player: player)
                .veloraVideoAspect(aspectMode)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea()
            Button {
                isPresented = false
            } label: {
                Label {
                    Text("Exit fullscreen", bundle: .module)
                } icon: {
                    Image(systemName: "arrow.down.right.and.arrow.up.left")
                }
                .labelStyle(.iconOnly)
            }
            .buttonStyle(.borderedProminent)
            .accessibilityLabel(Text("Exit fullscreen", bundle: .module))
            .padding()
            VeloraAspectMenu(selection: $aspectMode)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(.top, 56)
                .padding(.trailing)
            VeloraMediaSelectionMenu(player: player)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(.top, 104)
                .padding(.trailing)
        }
        .onAppear { player.play() }
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraFilmographyView: View {
    let person: JellyfinPerson
    @ObservedObject var model: VeloraAppModel
    @State private var works: [JellyfinItem] = []
    @State private var isLoading = true
    @State private var failed = false
    @State private var selectedItem: JellyfinItem?

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
            } else if failed {
                Text("Unable to load filmography", bundle: .module)
                    .foregroundStyle(.secondary)
            } else if works.isEmpty {
                Text("No other titles available", bundle: .module)
                    .foregroundStyle(.secondary)
            } else {
                VeloraLibraryView(title: person.name, items: works, artworkClient: model.jellyfinClient) { item in
                    selectedItem = item
                }
            }
        }
        .navigationTitle(person.name)
        .sheet(item: $selectedItem) { item in
            NavigationStack { VeloraItemDetailView(item: item, model: model) }
        }
        .task {
            do {
                works = try await model.filmography(for: person)
            } catch {
                failed = true
            }
            isLoading = false
        }
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraLiveTvView: View {
    @ObservedObject var model: VeloraAppModel
    @State private var player: AVPlayer?
    @State private var selectedChannel: JellyfinLiveTvChannel?
    @State private var sourceSelection: JellyfinLiveTvChannel?
    @State private var playbackTask: Task<Void, Never>?
    @State private var isLiveFullscreen = false
    @State private var liveAspectMode: VeloraAspectMode = .fit

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

    private func sourceLabel(_ source: JellyfinLiveTvMediaSource, index: Int) -> String {
        if let name = source.name?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            return name
        }
        let descriptor = [source.protocolName]
            .compactMap { $0?.lowercased() }
            .joined(separator: " ")
        if descriptor.contains("iptv") || descriptor.contains("m3u") {
            return String(localized: "IPTV source", bundle: .module)
        }
        if index == 0 {
            return String(localized: "Primary source", bundle: .module)
        }
        return String(format: String(localized: "Source option %d", bundle: .module), index + 1)
    }

    private func upcomingPrograms(for channel: JellyfinLiveTvChannel) -> [JellyfinLiveTvProgram] {
        let now = Date()
        return model.liveTvPrograms
            .filter { $0.channelID == channel.id && ($0.endDate ?? .distantPast) > now }
            .sorted { ($0.startDate ?? .distantPast) < ($1.startDate ?? .distantPast) }
            .prefix(3)
            .map { $0 }
    }

    var body: some View {
        List(model.liveTvChannels) { channel in
            Button {
                if channel.mediaSources.count > 1 {
                    sourceSelection = channel
                } else {
                    startPlayback(channel: channel)
                }
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text((channel.number.map { "\($0) · " } ?? "") + channel.name)
                        .font(.headline)
                    if let program = channel.currentProgram {
                        Text(program.name)
                            .foregroundStyle(.secondary)
                    }
                    ForEach(upcomingPrograms(for: channel)) { program in
                        HStack(spacing: 6) {
                            if let startDate = program.startDate {
                                Text(startDate, style: .time)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Text(program.name)
                                .font(.caption)
                                .lineLimit(1)
                        }
                    }
                }
            }
            .buttonStyle(.plain)
            .accessibilityHint(String(
                localized: channel.mediaSources.count > 1 ? "Choose channel source" : "Play live channel",
                bundle: .module
            ))
        }
        .navigationTitle(Text("Live TV", bundle: .module))
        .confirmationDialog(
            String(localized: "Choose channel source", bundle: .module),
            isPresented: Binding(
                get: { sourceSelection != nil },
                set: { if !$0 { sourceSelection = nil } }
            ),
            titleVisibility: .visible
        ) {
            if let channel = sourceSelection {
                ForEach(Array(channel.mediaSources.enumerated()), id: \.offset) { index, source in
                    Button(sourceLabel(source, index: index)) {
                        sourceSelection = nil
                        startPlayback(channel: channel, sourceID: source.id ?? source.liveStreamID)
                    }
                }
            }
            Button(String(localized: "Cancel", bundle: .module), role: .cancel) {
                sourceSelection = nil
            }
        } message: {
            if let channel = sourceSelection {
                Text(channel.name)
            }
        }
        .onDisappear {
            playbackTask?.cancel()
            let activeChannel = selectedChannel
            let position = player?.currentTime().seconds ?? 0
            player?.pause()
            player = nil
            selectedChannel = nil
            sourceSelection = nil
            if let activeChannel {
                Task { await model.stopLiveTv(channel: activeChannel, positionSeconds: position) }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let player, let selectedChannel {
                VStack(alignment: .leading, spacing: 8) {
                    Text(selectedChannel.name).font(.headline)
                    ZStack(alignment: .topTrailing) {
                        VideoPlayer(player: player)
                            .veloraVideoAspect(liveAspectMode)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        Button {
                            isLiveFullscreen = true
                        } label: {
                            Image(systemName: "arrow.up.left.and.arrow.down.right")
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityLabel(Text("Fullscreen", bundle: .module))
                        .padding(10)
                    }
                    HStack {
                        VeloraAspectMenu(selection: $liveAspectMode)
                        VeloraMediaSelectionMenu(player: player)
                    }
                }
                .padding()
                .background(.regularMaterial)
                #if os(macOS)
                .sheet(isPresented: $isLiveFullscreen) {
                    VeloraFullscreenPlayer(
                        player: player,
                        isPresented: $isLiveFullscreen,
                        aspectMode: $liveAspectMode
                    )
                }
                #else
                .fullScreenCover(isPresented: $isLiveFullscreen) {
                    VeloraFullscreenPlayer(
                        player: player,
                        isPresented: $isLiveFullscreen,
                        aspectMode: $liveAspectMode
                    )
                }
                #endif
            }
        }
    }
}
#endif
