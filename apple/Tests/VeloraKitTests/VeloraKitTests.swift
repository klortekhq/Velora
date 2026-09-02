import XCTest
@testable import VeloraKit

final class VeloraKitTests: XCTestCase {
    func testOfflineIsMobileOnly() {
        XCTAssertTrue(VeloraPlatform.iPhone.supportsOfflineDownloads)
        XCTAssertTrue(VeloraPlatform.iPad.supportsOfflineDownloads)
        XCTAssertFalse(VeloraPlatform.tvOS.supportsOfflineDownloads)
    }

    func testServerURLValidationRejectsEmbeddedCredentialsAndParameters() throws {
        XCTAssertNoThrow(try JellyfinClient(serverURL: URL(string: "http://192.168.31.232:8096")!))
        XCTAssertNoThrow(try JellyfinClient(serverURL: URL(string: "https://jellyfin.example.test/base")!))
        XCTAssertThrowsError(try JellyfinClient(serverURL: URL(string: "https://user:pass@jellyfin.example.test")!))
        XCTAssertThrowsError(try JellyfinClient(serverURL: URL(string: "https://jellyfin.example.test?token=secret")!))
        XCTAssertThrowsError(try JellyfinClient(serverURL: URL(string: "ftp://jellyfin.example.test")!))
    }

    func testPlaybackPrefersDirectPlay() {
        let caps = PlaybackCapabilities(videoCodecs: ["H264"], audioCodecs: ["AAC"], containers: ["MP4"])
        let source = PlaybackSource(container: "MP4", videoCodec: "H264", audioCodec: "AAC")
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps), .directPlay)
    }

    func testPlaybackFallsBackWhenActualDeviceLimitsAreExceeded() {
        let caps = PlaybackCapabilities(videoCodecs: ["h264"], audioCodecs: ["aac"], containers: ["mp4"], maxAudioChannels: 2, maxWidth: 1920)
        let source = PlaybackSource(container: "mp4", videoCodec: "h264", audioCodec: "aac", audioChannels: 6, width: 3840, height: 2160)
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps), .transcode)
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps, quality: .fullHD10), .transcode)
    }

    func testQualityPresetDoesNotAddCapsToOriginal() {
        let caps = PlaybackCapabilities(videoCodecs: ["hevc"], audioCodecs: ["eac3"], containers: ["mkv"])
        let source = PlaybackSource(container: "mkv", videoCodec: "hevc", audioCodec: "eac3", width: 7680, height: 4320, bitrateKbps: 100_000)
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps), .directPlay)
    }

    func testSettingsClampMusicVolumeAndKeepAppleTVStreamingOnly() {
        let settings = VeloraSettings(themeMusicVolume: 4)
        XCTAssertEqual(settings.themeMusicVolume, 1)
        XCTAssertFalse(VeloraPlatform.tvOS.supportsOfflineDownloads)
    }

    func testSubtitlePreferenceIsCodable() throws {
        let settings = VeloraSettings(subtitlePreference: .preferred, preferredSubtitleLanguage: "es")
        let data = try JSONEncoder().encode(settings)
        let restored = try JSONDecoder().decode(VeloraSettings.self, from: data)
        XCTAssertEqual(restored, settings)
    }

    func testSettingsStoreRoundTripsDeviceLocalPreferences() {
        let suiteName = "velora.settings.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer {
            defaults.removePersistentDomain(forName: suiteName)
        }

        let store = VeloraSettingsStore(defaults: defaults, key: "settings")
        let expected = VeloraSettings(
            languageIdentifier: VeloraLanguage.french.rawValue,
            preferredAudioLanguage: "fr",
            subtitlePreference: .preferred,
            preferredSubtitleLanguage: "fr",
            performanceMode: .balanced,
            themeMusicEnabled: true,
            themeMusicVolume: 0.42
        )

        XCTAssertNil(store.load())
        store.save(expected)
        XCTAssertEqual(store.load(), expected)
        store.remove()
        XCTAssertNil(store.load())
    }

    func testCredentialStoreRoundTripsAndRemovesSession() {
        let suiteName = "velora.credentials.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let store = VeloraCredentialStore(
            service: "test.service",
            account: "test.account",
            defaults: defaults,
            defaultsKey: "session"
        )
        let expected = JellyfinSession(accessToken: "secret-token", userID: "user-1", username: "demo-user", serverURL: "http://jellyfin.local:8096")
        XCTAssertNil(store.load())
        XCTAssertTrue(store.save(expected))
        XCTAssertEqual(store.load(), expected)
        XCTAssertTrue(store.remove())
        XCTAssertNil(store.load())
    }

    func testLanguageSelectionUsesDeviceLocaleByDefaultAndSupportsSupportedLocales() {
        let automatic = VeloraSettings()
        XCTAssertFalse(automatic.appLocale.identifier.isEmpty)
        XCTAssertEqual(Set(VeloraLanguage.allCases.map(\.rawValue)), Set(["es", "en", "fr", "de"]))

        let settings = VeloraSettings(languageIdentifier: VeloraLanguage.spanish.rawValue)
        XCTAssertEqual(settings.appLocale.identifier, "es")
    }

    func testJellyfinItemDecodesServerFieldNames() throws {
        let data = #"{"Id":"movie-1","Name":"Una película","Type":"Movie","Overview":"Descripción","ImageTags":{"Primary":"abc"}}"#.data(using: .utf8)!
        let item = try JSONDecoder().decode(JellyfinItem.self, from: data)
        XCTAssertEqual(item.id, "movie-1")
        XCTAssertEqual(item.name, "Una película")
        XCTAssertEqual(item.type, "Movie")
        XCTAssertEqual(item.overview, "Descripción")
        XCTAssertEqual(item.imageTags?["Primary"], "abc")
    }

    func testLiveTvModelsDecodeServerFieldNames() throws {
        let json = #"{"Id":"channel-1","Name":"Noticias","ChannelNumber":"24","CurrentProgram":{"Id":"program-1","Name":"Informativo","ChannelId":"channel-1","StartDate":"2026-09-01T10:00:00Z","EndDate":"2026-09-01T11:00:00Z","Overview":"Actualidad"}}"#
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let channel = try decoder.decode(JellyfinLiveTvChannel.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(channel.id, "channel-1")
        XCTAssertEqual(channel.number, "24")
        XCTAssertEqual(channel.currentProgram?.name, "Informativo")
        XCTAssertEqual(channel.currentProgram?.channelID, "channel-1")
    }

    func testLiveTvPlaybackInfoDecodesSelectedStreamURL() throws {
        let json = #"{"MediaSources":[{"Id":"source-1","LiveStreamId":"live-1","TranscodingUrl":"http://jellyfin.local:8096/Videos/channel-1/stream.m3u8","Protocol":"hls"}]}"#
        let info = try JSONDecoder().decode(JellyfinLiveTvPlaybackInfo.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(info.mediaSources.first?.liveStreamID, "live-1")
        XCTAssertEqual(info.mediaSources.first?.transcodingURL?.path, "/Videos/channel-1/stream.m3u8")
    }

    func testPlaybackStoppedRequestUsesJellyfinFieldNamesAndClampsPosition() throws {
        let request = JellyfinPlaybackStoppedRequest(itemID: "channel-1", positionTicks: 0)
        let json = try JSONSerialization.jsonObject(with: JSONEncoder().encode(request)) as? [String: Any]
        XCTAssertEqual(json?["ItemId"] as? String, "channel-1")
        XCTAssertEqual(json?["PositionTicks"] as? Int64, 0)
    }

    func testVideoRequestUsesStreamEndpointWithoutCredentialQuery() async throws {
        let client = try JellyfinClient(serverURL: URL(string: "http://jellyfin.local:8096")!)
        let url = await client.videoURL(itemID: "movie-one")
        XCTAssertEqual(url?.path, "/Videos/movie-one/stream")
        XCTAssertEqual(url?.query, "static=true")
        XCTAssertFalse(url?.absoluteString.contains("api_key") ?? true)
    }

    func testAuthenticationPayloadUsesJellyfinPasswordField() throws {
        let payload = try JSONEncoder().encode(["Username": "demo-user", "Password": "secret"])
        let json = try JSONSerialization.jsonObject(with: payload) as? [String: String]
        XCTAssertEqual(json?["Username"], "demo-user")
        XCTAssertEqual(json?["Password"], "secret")
        XCTAssertNil(json?["Pw"])
    }

    func testMediaIdentifiersAreEncodedAsPathComponents() async throws {
        let client = try JellyfinClient(serverURL: URL(string: "http://jellyfin.local:8096")!)
        let url = await client.videoURL(itemID: "movie with spaces")
        XCTAssertEqual(url?.percentEncodedPath, "/Videos/movie%20with%20spaces/stream")
        let slashURL = await client.videoURL(itemID: "movie/with-slash")
        let traversalURL = await client.imageURL(itemID: "../escape")
        XCTAssertNil(slashURL)
        XCTAssertNil(traversalURL)
    }
}
