import XCTest
@testable import VeloraKit

final class VeloraKitTests: XCTestCase {
    func testOfflineIsMobileOnly() {
        XCTAssertTrue(VeloraPlatform.iPhone.supportsOfflineDownloads)
        XCTAssertTrue(VeloraPlatform.iPad.supportsOfflineDownloads)
        XCTAssertFalse(VeloraPlatform.tvOS.supportsOfflineDownloads)
    }

    func testPlaybackPrefersDirectPlay() {
        let caps = PlaybackCapabilities(videoCodecs: ["h264"], audioCodecs: ["aac"], containers: ["mp4"])
        let source = PlaybackSource(container: "MP4", videoCodec: "H264", audioCodec: "AAC")
        XCTAssertEqual(PlaybackDecisionEngine.decide(source: source, capabilities: caps), .directPlay)
    }
}
