package org.schabi.newpipe.extractor.timeago;

/** Resolves the generated locale patterns with country and language fallback. */
public final class PatternsManager {
    private PatternsManager() {
    }

    public static PatternsHolder getPatterns(final String languageCode, final String countryCode) {
        PatternsHolder patterns = null;
        if (languageCode != null && countryCode != null && !countryCode.isEmpty()) {
            patterns = PatternMap.getPattern(languageCode + "_" + countryCode);
        }
        if (patterns == null && languageCode != null) {
            patterns = PatternMap.getPattern(languageCode);
        }
        if (patterns == null && "en".equals(languageCode)) {
            patterns = PatternMap.getPattern("en_GB");
        }
        if (patterns == null && "es".equals(languageCode)) {
            patterns = PatternMap.getPattern("es_US");
        }
        return patterns;
    }
}
