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
    @Published public private(set) var items: [JellyfinItem] = []
    @Published public private(set) var liveTvChannels: [JellyfinLiveTvChannel] = []
    @Published public var errorMessage: String?
    @Published public var settings: VeloraSettings {
        didSet { settingsStore.save(settings) }
    }

    public let platform: VeloraPlatform
    private let client: JellyfinClient
    private let settingsStore: VeloraSettingsStore
    private let credentialStore: VeloraCredentialStore
    private let serverDefaults: UserDefaults
    private var session: JellyfinSession?

    public var jellyfinClient: JellyfinClient { client }

    public init(platform: VeloraPlatform, serverURL: URL, credentialStore: VeloraCredentialStore = VeloraCredentialStore(), serverDefaults: UserDefaults = .standard) throws {
        self.platform = platform
        self.settingsStore = VeloraSettingsStore()
        self.settings = settingsStore.load() ?? VeloraSettings.systemDefault()
        self.credentialStore = credentialStore
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
            async let library = client.items(userID: authenticated.userID, includeTypes: ["Movie", "Series"])
            async let channels = client.liveTvChannels(userID: authenticated.userID)
            items = try await library
            liveTvChannels = (try? await channels) ?? []
            isAuthenticated = true
            errorMessage = nil
        } catch {
            isAuthenticated = false
            errorMessage = String(localized: "Unable to sign in", bundle: .module)
        }
    }

    public func signOut() async {
        await client.setAccessToken(nil)
        credentialStore.remove()
        session = nil
        items = []
        liveTvChannels = []
        isAuthenticated = false
    }

    public func play(_ item: JellyfinItem) async -> AVPlayer? {
        guard let requestURL = await client.videoURL(itemID: item.id) else { return nil }
        let request = await client.authorizedRequest(for: requestURL)
        guard let url = request.url else { return nil }
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": request.allHTTPHeaderFields ?? [:]])
        return AVPlayer(playerItem: AVPlayerItem(asset: asset))
    }

    public func playLiveTv(channel: JellyfinLiveTvChannel) async -> AVPlayer? {
        guard let session else { return nil }
        let requestURL: URL?
        do {
            requestURL = try await client.liveTvPlaybackURL(userID: session.userID, channelID: channel.id)
        } catch {
            return nil
        }
        guard let requestURL else { return nil }
        let request = await client.authorizedRequest(for: requestURL)
        guard let url = request.url else { return nil }
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": request.allHTTPHeaderFields ?? [:]])
        return AVPlayer(playerItem: AVPlayerItem(asset: asset))
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

    public init(platform: VeloraPlatform, serverURL: URL) throws {
        let model = try VeloraAppModel(platform: platform, serverURL: serverURL)
        _model = StateObject(wrappedValue: model)
        _serverText = State(initialValue: serverURL.absoluteString)
    }

    public var body: some View {
        Group {
            if model.isAuthenticated {
                NavigationStack {
                    VeloraLibraryView(title: "Library", items: model.items, artworkClient: model.jellyfinClient) { item in
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
                    Text("Offline downloads are available on this device.", bundle: .module)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
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

    var body: some View {
        List(model.liveTvChannels) { channel in
            Button {
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
                    let nextPlayer = await model.playLiveTv(channel: channel)
                    guard !Task.isCancelled else {
                        nextPlayer?.pause()
                        return
                    }
                    player?.pause()
                    player = nextPlayer
                    selectedChannel = channel
                    player?.play()
                }
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
            .accessibilityHint("Play live channel")
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
