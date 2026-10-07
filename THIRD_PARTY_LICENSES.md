# Third-party licenses

Velora is distributed under the license in [LICENSE](LICENSE). The project
also bundles or links the following open-source components. Versions are
defined in `gradle/libs.versions.toml` and `app/build.gradle.kts`; consult the
upstream project for the complete license text and notices.

| Component | Purpose | License | Upstream |
| --- | --- | --- | --- |
| AndroidX / Jetpack Compose / Media3 | Android UI, lifecycle and playback | Apache-2.0 | https://developer.android.com/ |
| Kotlin / kotlinx.serialization | Language and serialization | Apache-2.0 | https://kotlinlang.org/ |
| Ktor | Jellyfin HTTP client | Apache-2.0 | https://ktor.io/ |
| Coil | Compose image loading | Apache-2.0 | https://github.com/coil-kt/coil |
| Glide | Image loading/compiler support | BSD-2-Clause | https://github.com/bumptech/glide |
| Lottie for Android | Animation support | Apache-2.0 | https://github.com/airbnb/lottie-android |
| OkHttp | HTTP support for playback/update paths | Apache-2.0 | https://github.com/square/okhttp |
| Jsoup | HTML parsing support | MIT | https://jsoup.org/ |
| NewPipe Extractor | Optional trailer/media extraction path | GPL-3.0-or-later | https://github.com/TeamNewPipe/NewPipeExtractor |
| Jellyfin Media3 FFmpeg decoder | Optional software audio/video decoders | GPL-3.0-or-later | https://github.com/jellyfin/AndroidXMedia |
| libgav1 decoder module | Optional AV1 software decoding | BSD-3-Clause | https://chromium.googlesource.com/codecs/libgav1/ |

## Attribution policy

Velora maintains its own visual identity and source code. General product
research does not add third-party application code, artwork or branding to the
distribution. Any future dependency must be added here with its license before
release.

This inventory is maintained alongside the build files. A release reviewer
must verify transitive dependency notices and update this file when dependency
versions or packaging change.
