# Velora implementation status

Updated: 2026-08-27

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
- Android unit tests: 6 playback decision/metadata tests passed in the prior
  run; the follow-up run was blocked by a Gradle cache `AccessDeniedException`
  while closing Media3 jars, not by a test assertion. Instrumentation not run.
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
| `Velora-Mobile-debug.apk` | `658AE0D44F521392A081F5C0B0B86EAB327D57E6C243B148BCE27BE8F95F7411` |
| `Velora-TV-debug.apk` | `74BDD788D7D52764639D222A8DB27F155D873A9449D0995317BE78F44B0E3A7A` |
| `Velora-Mobile-release-unsigned.apk` | `0C89B6C9642A98FFAE511440BF2CB91EAF5042D8DEC27B2741907C22DADF9FD7` |
| `Velora-TV-release-unsigned.apk` | `C5955EC50ED99D2F92C409783E5975ABE06CBA8E6D697FFC7ECE0618338DBB78` |
