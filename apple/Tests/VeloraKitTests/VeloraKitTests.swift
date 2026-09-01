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
