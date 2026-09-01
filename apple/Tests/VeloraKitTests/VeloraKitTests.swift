import XCTest
@testable import VeloraKit

final class VeloraKitTests: XCTestCase {
    func testOfflineIsMobileOnly() {
        XCTAssertTrue(VeloraPlatform.iPhone.supportsOfflineDownloads)
        XCTAssertTrue(VeloraPlatform.iPad.supportsOfflineDownloads)
        XCTAssertFalse(VeloraPlatform.tvOS.supportsOfflineDownloads)
    }

    func testPlaybackPrefersDirectPlay() {
        let caps = PlaybackCapabilities(videoCodecs: ["H264"], audioCodecs: ["AAC"], containers: ["MP4"])
        let source = PlaybackSource(container: "MP4", videoCodec: "H264", audioCodec: "AAC")
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps), .directPlay)
    }

    func testPlaybackFallsBackWhenActualDeviceLimitsAreExceeded() {
        let caps = PlaybackCapabilities(videoCodecs: ["h264"], audioCodecs: ["aac"], containers: ["mp4"], maxAudioChannels: 2, maxWidth: 1920)
        let source = PlaybackSource(container: "mp4", videoCodec: "h264", audioCodec: "aac", audioChannels: 6, width: 3840, height: 2160)
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps), .directStream)
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps, quality: .fullHD10), .directStream)
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
}
