#if canImport(SwiftUI)
import SwiftUI

/// Small native SwiftUI building blocks shared by the iOS, iPadOS and tvOS apps.
/// The actual app target owns navigation and playback so each platform can keep
/// its native lifecycle, focus system and AVPlayer integration.
@available(iOS 16.0, tvOS 16.0, *)
public struct VeloraItemCard: View {
    public let item: JellyfinItem
    public let image: Image?
    public let onSelect: () -> Void

    public init(item: JellyfinItem, image: Image? = nil, onSelect: @escaping () -> Void) {
        self.item = item
        self.image = image
        self.onSelect = onSelect
    }

    public var body: some View {
        Button(action: onSelect) {
            VStack(alignment: .leading, spacing: 8) {
                Group {
                    if let image { image.resizable().scaledToFill() }
                    else { Color.secondary.opacity(0.2) }
                }
                .frame(minWidth: 120, minHeight: 170)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                Text(item.name)
                    .font(.headline)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(item.name)
    }
}

@available(iOS 16.0, tvOS 16.0, *)
public struct VeloraLibraryView: View {
    public let title: String
    public let items: [JellyfinItem]
    public let imageProvider: (JellyfinItem) -> Image?
    public let onSelect: (JellyfinItem) -> Void

    public init(title: String, items: [JellyfinItem], imageProvider: @escaping (JellyfinItem) -> Image? = { _ in nil }, onSelect: @escaping (JellyfinItem) -> Void) {
        self.title = title
        self.items = items
        self.imageProvider = imageProvider
        self.onSelect = onSelect
    }

    public var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 16)], spacing: 20) {
                ForEach(items) { item in
                    VeloraItemCard(item: item, image: imageProvider(item)) { onSelect(item) }
                }
            }
            .padding()
        }
        .navigationTitle(title)
    }
}

@available(iOS 16.0, tvOS 16.0, *)
public struct VeloraSettingsView: View {
    @Binding public var settings: VeloraSettings

    public init(settings: Binding<VeloraSettings>) {
        _settings = settings
    }

    public var body: some View {
        Form {
            Section {
                Picker(selection: $settings.languageIdentifier) {
                    Text("Automatic", bundle: .module).tag(nil as String?)
                    Text("Español").tag(VeloraLanguage.spanish.rawValue as String?)
                    Text("English").tag(VeloraLanguage.english.rawValue as String?)
                    Text("Français").tag(VeloraLanguage.french.rawValue as String?)
                    Text("Deutsch").tag(VeloraLanguage.german.rawValue as String?)
                } label: {
                    Text("App language", bundle: .module)
                }
                TextField(text: Binding(
                    get: { settings.preferredAudioLanguage ?? "" },
                    set: { settings.preferredAudioLanguage = $0.isEmpty ? nil : $0 }
                )) {
                    Text("Preferred audio", bundle: .module)
                }
                Picker(selection: $settings.subtitlePreference) {
                    Text("Automatic", bundle: .module).tag(SubtitlePreference.automatic)
                    Text("Disabled", bundle: .module).tag(SubtitlePreference.off)
                    Text("Preferred", bundle: .module).tag(SubtitlePreference.preferred)
                    Text("Forced", bundle: .module).tag(SubtitlePreference.forced)
                } label: {
                    Text("Subtitles", bundle: .module)
                }
                Picker(selection: $settings.performanceMode) {
                    Text("Automatic", bundle: .module).tag(PerformanceMode.automatic)
                    Text("Quality", bundle: .module).tag(PerformanceMode.quality)
                    Text("Balanced", bundle: .module).tag(PerformanceMode.balanced)
                    Text("Performance", bundle: .module).tag(PerformanceMode.performance)
                } label: {
                    Text("Performance", bundle: .module)
                }
            } header: {
                Text("Language and playback", bundle: .module)
            }
            Section {
                Toggle(isOn: $settings.themeMusicEnabled) {
                    Text("Enable theme music", bundle: .module)
                }
                Slider(value: $settings.themeMusicVolume, in: 0...1) {
                    Text("Volume", bundle: .module)
                }
                .disabled(!settings.themeMusicEnabled)
            } header: {
                Text("Theme music", bundle: .module)
            }
        }
        .navigationTitle(Text("Settings", bundle: .module))
    }
}
#endif
