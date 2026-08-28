# Velora implementation status

Updated: 2026-08-28

This dashboard records verified repository state only. A feature is not marked
complete merely because code or a previous build artifact exists.

## Current wave

Wave 1 — shared capability/playback contracts and Android hardening.

## Verified in source

- Android namespace and application id use `com.klortek.velora`.
- Android product flavors exist for TV and mobile/tablet installs.
- ExoPlayer/Media3 is the default Android playback path; MPV remains an
  explicit fallback/option in the current implementation.
- A pure Kotlin `PlaybackDecisionEngine` now encodes Original First ordering
  and device/preset constraints for native backends.
- `JellyfinPlaybackMapper` converts real `MediaSource`/`MediaStream` metadata
  (container, codecs, HDR, dimensions, FPS, bitrate, multichannel audio and
  subtitle type) into that common playback contract.
- `PlatformCapabilities.supportsOfflineDownloads` is false for TV builds and
  gates mobile-only offline UI.
- Jellyfin Live TV client and a conditional TV entry point exist.
- A shared web client and Samsung/LG packaging scripts exist.
- App language and preferred audio/subtitle settings have been started in
  Android and web.
- Generated local build/device artifacts are now ignored by Git.
- Release metadata is synchronized at version 1.2.2 across Android, webOS,
  Samsung and VIDAA manifests.
- The ExoPlayer playback URL path now relies on MediaBrowser/X-Emby request
  headers instead of putting the Jellyfin token in the playback query string.
- The shared playback mapper/decision engine is now invoked from the Android
  MediaSource loading path and records the selected path for diagnostics.
- Android playback capabilities now probe installed MediaCodec decoders and
  display HDR types conservatively; unknown container/passthrough support is
  left unspecified.
- Aspect selection is applied to both the PlayerView and its real
  `AspectRatioFrameLayout`, including forced 4:3/16:9/cinema modes.

## Explicitly incomplete or requiring verification

- Full source-wide internationalization is not yet verified; legacy hardcoded
  strings remain.
- Jellyseerr/TMDB legacy source is still present and must be removed or fully
  isolated from the product surface without breaking builds.
- The playback contract is introduced; platform-specific capability population
  and runtime validation remain incomplete.
- Apple iOS/iPadOS/tvOS clients are not present/verified.
- VIDAA support is not validated on a real device or certified runtime.
- Offline database/storage engine and recovery journey are not yet verified.
- Theme music, trailer resolver, persistent preview, and large-library
  virtualization need implementation and critic testing.
- No hardware test result is claimed: the connected Fire TV/phone were not
  available to this automated run.

## Platform evidence

| Platform | Source/build state | Hardware/runtime validation |
| --- | --- | --- |
| Android mobile/tablet | Debug + release APK built; unit tests pass | Hardware validation pending |
| Android TV / Fire TV | Debug + release APK built; TV download guard compiled | Hardware validation pending |
| Web | `npm run build:all` passes; webOS IPK generated | Browser/device validation pending |
| Samsung Tizen | Packaging path exists | Device/emulator validation pending |
| LG webOS | Packaging path exists | Device/emulator validation pending |
| Hisense VIDAA | Preparation only | Not verified |
| iOS/iPadOS/tvOS | Not implemented in current tree | Not applicable |

## Quality gates

- `git diff --check`: passing for the current working tree.
- Android `testMobileDebugUnitTest`, `assembleMobileDebug`,
  `assembleTvDebug`, `assembleMobileRelease`, and `assembleTvRelease`:
  passing on 2026-08-27.
- Android `testMobileDebugUnitTest` and `compileMobileDebugKotlin` passed on
  2026-08-28 after the capability probe fix; the build emitted only existing
  deprecation/KAPT warnings.
  Instrumentation and real-device playback validation were not run.
- Web `npm run build:all`: passing; Tizen CLI unavailable, webOS IPK generated,
  VIDAA hosted HTML5 bundle generated.
- Packaging and SHA-256: generated locally; see `outputs/` (ignored).
- GitHub Android workflow now builds and publishes both mobile and TV debug /
  unsigned-release variants from a version tag.
- Independent critic review: pending.

The requested historical base `c3a2e52506942597444468be78ba3281996a6576` is
not present in this clone. The reproducible local patch is therefore against
the actual branch base `418383f`.

## Latest local artifacts

| Artifact | SHA-256 |
| --- | --- |
| `Velora-Mobile-debug.apk` | `8DD83ECA430F8497C71EA550E2FD55B47131BC1469BCF7207C0C37A96F7BA398` |
| `Velora-TV-debug.apk` | `141330C77C8DFFEFCE98C92F18BEAF623AEEA3EE3EA0661C432EBEC39BF87176` |
| `Velora-Mobile-release-unsigned.apk` | `BE23B2A15B50501479901ACD0139653A96AE9B5EDD6BEA864FBFE0245787355D` |
| `Velora-TV-release-unsigned.apk` | `6A592C4FB20E6D62C21FBC9C8F67DE90809833312E291065CD9A553C59296BB8` |
