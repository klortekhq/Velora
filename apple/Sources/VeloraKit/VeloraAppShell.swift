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
    @Published public var errorMessage: String?
    @Published public var settings: VeloraSettings {
        didSet { settingsStore.save(settings) }
    }

    public let platform: VeloraPlatform
    private let client: JellyfinClient
    private let settingsStore: VeloraSettingsStore
    private var session: JellyfinSession?

    public init(platform: VeloraPlatform, serverURL: URL) throws {
        self.platform = platform
        self.client = try JellyfinClient(serverURL: serverURL)
        self.settingsStore = VeloraSettingsStore()
        self.settings = settingsStore.load() ?? VeloraSettings.systemDefault()
    }

    public func signIn(username: String, password: String) async {
        do {
            let authenticated = try await client.authenticate(username: username, password: password)
            session = authenticated
            items = try await client.items(userID: authenticated.userID, includeTypes: ["Movie", "Series"])
            isAuthenticated = true
            errorMessage = nil
        } catch {
            isAuthenticated = false
            errorMessage = "Unable to sign in"
        }
    }

    public func play(_ item: JellyfinItem) async -> AVPlayer? {
        guard let requestURL = await client.videoURL(itemID: item.id) else { return nil }
        let request = await client.authorizedRequest(for: requestURL)
        guard let url = request.url else { return nil }
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": request.allHTTPHeaderFields ?? [:]])
        return AVPlayer(playerItem: AVPlayerItem(asset: asset))
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
                    VeloraLibraryView(title: "Library", items: model.items) { item in
                        selectedItem = item
                    }
                    .toolbar {
                        ToolbarItem(placement: .automatic) {
                            NavigationLink("Settings") {
                                VeloraSettingsView(settings: $model.settings)
                            }
                        }
                    }
                }
                .sheet(item: $selectedItem) { item in
                    NavigationStack { VeloraItemDetailView(item: item, model: model) }
                }
            } else {
                VeloraLoginView(
                    server: serverText,
                    username: $username,
                    password: $password,
                    errorMessage: model.errorMessage,
                    onSignIn: { await model.signIn(username: username, password: password) }
                )
            }
        }
        .environment(\.locale, model.settings.appLocale)
    }
}

@available(iOS 16.0, tvOS 16.0, *)
private struct VeloraLoginView: View {
    let server: String
    @Binding var username: String
    @Binding var password: String
    let errorMessage: String?
    let onSignIn: () async -> Void

    var body: some View {
        Form {
            Section("Connect to Jellyfin") {
                LabeledContent("Server address", value: server)
                TextField("Username", text: $username)
                SecureField("Password", text: $password)
                Button("Sign in") { Task { await onSignIn() } }
                    .disabled(server.isEmpty || username.isEmpty || password.isEmpty)
                if let errorMessage { Text(errorMessage).foregroundStyle(.red) }
            }
        }
        .navigationTitle("Sign in")
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
                Button("Play") {
                    Task { player = await model.play(item); player?.play() }
                }
                if let player { VideoPlayer(player: player).aspectRatio(16 / 9, contentMode: .fit) }
                if model.platform.supportsOfflineDownloads {
                    Text("Offline downloads are available on this device.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .padding()
        }
        .navigationTitle(item.name)
    }
}
#endif
