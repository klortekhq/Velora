# Velora implementation status

Updated: 2026-08-29

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
- The public README now identifies the current release and uses the exact
  mobile/TV build and test tasks used by CI.
- Release metadata is synchronized at version 1.2.10 across Android, webOS,
  Samsung and VIDAA manifests.
- The ExoPlayer playback URL path now relies on MediaBrowser/X-Emby request
  headers instead of putting the Jellyfin token in the playback query string.
- Android-managed downloads and the MPV fallback now also keep the Jellyfin
  token in request headers rather than embedding it in playback/download URLs.
- Quick Connect diagnostics no longer log access-token fragments or polling
  secrets.
- Authentication diagnostics no longer print full request URLs, auth headers,
  token-bearing error bodies, or generated playback URLs; MPV's legacy launcher
  also uses its authenticated request headers without an `api_key` query.
- TMDB trailer diagnostics now redact user-provided API keys; the regression
  test prevents secret values from returning to Logcat.
- The mobile download enqueue path now checks available space and managed
  offline usage before replacing a failed entry, uses Jellyfin source size
  when available, and surfaces rejection without closing the app. The
  configurable maximum defaults to 25 GiB in `AppSettings`.
- Offline deletion now routes provider-backed `content://` URIs through
  `ContentResolver`, while retaining compatibility with `file://` and legacy
  filesystem paths.
- The Android player now reapplies the selected aspect mode after every Media3
  `VideoSize` update, preventing fullscreen or stream changes from silently
  restoring the source ratio.
- The web client now scopes Jellyfin tokens and user IDs to `sessionStorage`,
  migrates legacy localStorage values once, and removes the durable token copy;
  server URL and non-sensitive UI preferences remain persistent.
- Subtitle stream URLs and Jellyfin music stream URLs are also tokenless; the
  Media3 music service applies Jellyfin authentication through request headers.
- Android Jellyfin access tokens and passwords now use an Android Keystore-backed
  encrypted store, with a transparent migration from legacy plaintext preferences;
  API 21–22 use an RSA-wrapped AES compatibility key.
- Offline-download availability is covered by a platform-surface contract and
  unit test: mobile/tablet and iOS mobile are eligible; TV, browser, tvOS,
  Tizen, webOS and VIDAA are not.
- The shared web/Smart TV adapter now exposes the same explicit capability
  contract with `supportsOfflineDownloads: false`; its Node test covers web,
  Tizen, webOS and VIDAA user agents.
- The shared playback mapper/decision engine is now invoked from the Android
  MediaSource loading path and records the selected path for diagnostics.
- The playback mapper also falls back to Jellyfin's numeric `Channels` field
  when `ChannelLayout` is missing, with a regression test for multichannel audio.
- Android playback capabilities now probe installed MediaCodec decoders and
  display HDR types conservatively; unknown container/passthrough support is
  left unspecified.
- Aspect selection is applied to both the PlayerView and its real
  `AspectRatioFrameLayout`, including forced 4:3/16:9/cinema modes.
- The GL video surface now distinguishes stretch from fill: fill crops while
  preserving the source proportions, while stretch intentionally fills both
  axes. This keeps the selector effective when the enhanced GL path is used.
- Mobile player tap handling now ignores pointer sequences consumed by child
  controls, preventing the parent gesture layer from swallowing aspect,
  settings, audio, subtitle and playback button taps.
- Mobile detail navigation and action controls now use touch-native Material
  controls instead of TV focus controls; episode download actions are omitted
  entirely on platforms that do not support offline downloads.
- Filmography cards in the person/actor screen now keep D-pad activation on TV
  while exposing an explicit touch target on phones and tablets, so tapping an
  actor's related filmography works on mobile as well.
- The mobile player keeps fullscreen exit inside the player: Back exits
  fullscreen before leaving playback, and the visible landscape controls use
  the same orientation/fullscreen state instead of closing the activity.
- Movie and series mobile libraries now share a deterministic, tested content
  query for sorting by name/date/runtime/ratings and filtering by favorites,
  watched state and genre; the selected sort mode is persisted consistently.
- The mobile sort/filter panel is height-bounded and vertically scrollable, so
  long genre lists remain reachable on small phones and tablets.
- TV movie and series libraries now expose the same expanded sort choices
  (runtime, random, critic rating and community rating) and persist the
  selected mode through the shared settings preference.
- The home UI does not render a separate recently-added episode row; episodes
  remain reachable from series details and Continue Watching.
- Offline download metadata now uses an app-private SQLite index with a
  one-time migration from the former JSON preference store; completed media
  metadata survives process recreation without depending on SharedPreferences.
- The offline enqueue boundary now enforces the mobile/tablet capability guard
  itself, so TV builds cannot start a download even if a future caller bypasses
  the UI. The mobile episode download control also uses the mobile Material
  control rather than the TV-specific control.
- Completed DownloadManager entries now retain their complete local `content://`
  or `file://` URI, so offline playback does not depend on an invalid filesystem
  path conversion.
- Offline playback now enters the shared Media3/ExoPlayer screen directly and
  does not require a configured Jellyfin session; the explicit MPV setting and
  fallback remain available only for streamed playback.
- Offline completion semantics have regression coverage for provider-backed
  `content://` URIs, successful downloads without a filesystem path, and
  incomplete entries.
- Mobile and tablet download actions now present a quality chooser with
  Original, Alta (1080p), Media (720p) and Baja (480p); the selected profile
  is persisted in the offline index and lower profiles explicitly request a
  Jellyfin transcode rather than being mislabeled as the original file.
- A device-independent `OfflineStoragePolicy` now rejects invalid/overflowing
  sizes and enforces both a safety reserve and configurable minimum-free-space
  and managed-offline limits; it has deterministic unit coverage and is
  integrated with the download queue and mobile settings UI.
- Live TV now requests the current programme with the channel list and a
  bounded six-hour upcoming guide window, maps the next programme per channel,
  and renders the guide lazily in the existing virtualized `LazyColumn`; every
  channel row remains an actionable Jellyfin playback entry.
- Live TV channels now expose Jellyfin-backed favorites and tag-based groups;
  the Android screen provides touch/focus-safe filters and favorite toggles,
  with deterministic JVM coverage for the filtering rules.
- Login UI labels and authentication state are now resource-backed in the
  Spanish, English, French and German catalogs, including the server name
  placeholder and both mobile and TV login actions.
- Web artwork URLs no longer contain the Jellyfin token; the browser requests
  artwork with `X-Emby-Token` and assigns a short-lived object URL, covered by
  the web regression test. Browser-native video streaming still uses the
  Jellyfin-compatible query-token fallback because an HTML video element cannot
  attach custom request headers without a streaming proxy.
- Web item details now expose Jellyfin cast/guest-star buttons; selecting a
  person loads that person's movie and series filmography through the API and
  keeps the result keyboard-accessible.
- The web library now has persistent sorting and filtering for movies and
  series (name, added date, premiere, runtime, rating, favorites and playback
  state), with the required fields requested directly from Jellyfin.
- Resolved Android trailers now enter the canonical Media3/ExoPlayer player
  surface, preserving the same controls and fullscreen behavior as normal
  playback; the old direct MPV trailer handoff is removed.
- Release workflows now share a tag-scoped concurrency group, so Android and
  web assets update one release serially; web checksums use a distinct name
  and cannot overwrite the Android checksum manifest.
- Release `v1.2.4` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.4
  with mobile/TV debug and unsigned release APKs plus `SHA256SUMS.txt`; the
  four APKs were rebuilt and verified before upload.
- Release `v1.2.5` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.5
  with the same four APK variants and a verified `SHA256SUMS.txt`; it includes
  the TMDB diagnostic secret-redaction fix.
- Release `v1.2.6` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.6
  with four rebuilt APK variants and a remotely verified `SHA256SUMS.txt`; it
  includes the pre-enqueue offline storage safety gate.
- Release `v1.2.7` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.7
  with four rebuilt APK variants and a remotely verified `SHA256SUMS.txt`; it
  includes the mobile/tablet storage-limit selector and keeps that control out
  of TV settings.
- GitHub repository visibility is verified as public, with source, licensing,
  dashboard and release artifacts available at https://github.com/klortekhq/Velora.

## Explicitly incomplete or requiring verification

- Android now has a tested locale catalog, translated core language/audio/subtitle settings, and resource-backed mobile navigation/search labels in Spanish, English, French and German; full source-wide internationalization is not yet verified because legacy hardcoded strings remain.
- The mobile offline-download screen now also uses localized resources for empty, progress, availability and delete states in those four languages.
- Jellyseerr/request and Discover legacy source remains only for migration
  compatibility and is now unreachable from the product surface; it should be
  removed in a later cleanup once migration coverage exists. TMDB remains only
  behind the trailer integration.
- The playback contract is introduced; platform-specific capability population
  and runtime validation remain incomplete.
- Apple iOS/iPadOS/tvOS clients are not present/verified.
- VIDAA support is not validated on a real device or certified runtime.
- Offline queue/storage recovery, integrity verification, settings integration
  and the full end-to-end offline journey are not yet verified; the storage
  policy contract itself now has unit coverage.
  the durable metadata foundation is now in place.
- Theme music, a Jellyfin-first trailer resolver, persistent preview, and large-library
  virtualization need implementation and critic testing.
- Live TV favorites, groups, channel zapping/previous-channel shortcut,
  mini-player and full programme-details view still need implementation.
- No hardware test result is claimed: the connected Fire TV/phone were not
  available to this automated run.

## Platform evidence

| Platform | Source/build state | Hardware/runtime validation |
| --- | --- | --- |
| Android mobile/tablet | `assembleMobileDebug` passes with AV1 native decoder; unit tests pass | Hardware validation pending |
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
  passing on 2026-08-29. The full four-variant build completed online after
  the offline cache was found incomplete.
- After synchronizing the repository checkout, `compileMobileDebugKotlin`
  passed again on 2026-08-29; this confirms the tracked offline download
  manager source compiles, including its Android content-URI path.
- The tracked checkout also passes `testMobileDebugUnitTest` on 2026-08-29
  after that correction.
- The Media3 player was corrected so `Rellenar` uses proportional zoom/crop
  while `Estirar` remains the only deliberately distorting mode; mobile
  Kotlin compilation passed after the change.
- Android `testMobileDebugUnitTest` and `compileMobileDebugKotlin` passed on
  2026-08-29 after the security/capability changes; the build emitted only
  existing deprecation/KAPT warnings.
  Instrumentation and real-device playback validation were not run.
- Commit `8404e71` also passed `compileMobileDebugKotlin`,
  `testMobileDebugUnitTest`, and `assembleMobileDebug` with the native AV1
  decoder on 2026-08-29.
- Web `npm test` and `npm run build:all`: passing; Tizen CLI unavailable, webOS IPK generated,
  VIDAA hosted HTML5 bundle generated.
- GitHub Release `v1.2.4` assets and their published checksums were verified
  after upload.
- Android `testMobileDebugUnitTest` passed after the TMDB log-redaction change;
  the new security regression is included in the suite.
- GitHub Actions runs are currently rejected before the runner starts because
  GitHub reports failed recent account payments or an exceeded spending limit;
  this remains true after retrying the v1.2.5 tag runs even after making the
  repository public. Manual release upload remains verified until the account
  billing issue is resolved.
- Release `v1.2.3` was verified on GitHub with the four uniquely named APK
  assets and `SHA256SUMS.txt`; the release URL is
  `https://github.com/klortekhq/Velora/releases/tag/v1.2.3`.
- Web `npm test`: passing on 2026-08-29; platform capability regression test
  passes. Tizen packaging remains unvalidated without Tizen Studio/signing.
- `compileMobileDebugKotlin` and `testMobileDebugUnitTest`: passing after the
  ExoPlayer trailer routing change; only existing deprecation/KAPT warnings
  were emitted.
- A fresh local Android verification was blocked before compilation because
  this machine has no Android SDK installed; no new Android hardware result is
  claimed. The Windows non-ASCII path guard is enabled in `gradle.properties`.
- Packaging and SHA-256: generated locally; see `outputs/` (ignored).
- Latest mobile debug APK from the 1.2.8 build is listed in the release assets
  below; no physical-device validation is claimed when ADB has no device.
- GitHub Android workflow now builds and publishes both mobile and TV debug /
  unsigned-release variants from a version tag.
- GitHub web workflow validates on `main`/pull requests and packages the common
  web client plus Samsung, webOS and VIDAA targets together; a version tag
  publishes the resulting archive and checksums to the same release.
- Manual web workflow runs default safely to `all` when no target is selected.
- Independent critic review: pending.

The requested historical base `c3a2e52506942597444468be78ba3281996a6576` is
not present in this clone. The reproducible local patch is therefore against
the actual branch base `418383f`.

## Latest local artifacts

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `A96E048B566BBF913C1B5E7300871D97D1E33A1094435F37CC877A3385E5A827` |
| `Velora-tv-debug.apk` | `981D6A439856B72212AE38CC7AE5387AC42D433401BB1108B9AD0E0BE882CEBB` |
| `Velora-mobile-release-unsigned.apk` | `87C403E7D380C5A6321CCF77136C0E342AD93B782153D7CB666BB32BDFCEDC7B` |
| `Velora-tv-release-unsigned.apk` | `93A45509A89D7A6AF28675497E021E9F4349421501AA6D621AEB605840F71DB9` |

Release `v1.2.10`: https://github.com/klortekhq/Velora/releases/tag/v1.2.10
