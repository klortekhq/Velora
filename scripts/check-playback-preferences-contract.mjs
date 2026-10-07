import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const androidSettings = read('app/src/main/java/com/klortek/velora/jellyfin/AppSettings.kt');
const androidPlayer = read('app/src/main/java/com/klortek/velora/screens/JellyfinVideoPlayerScreen.kt');
const appleShell = read('apple/Sources/VeloraKit/VeloraAppShell.swift');
const web = read('web/app.js');

const failures = [];
if (!androidSettings.includes('var preferredAudioLanguage') || !androidSettings.includes('var subtitleMode') || !androidSettings.includes('var preferredSubtitleLanguage')) {
  failures.push('Android must persist audio and subtitle preferences.');
}
if (!androidPlayer.includes('.setPreferredAudioLanguage(preferredAudioLanguage)') ||
    !androidPlayer.includes('.setPreferredTextLanguage') ||
    !androidPlayer.includes('setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitleMode == "off")')) {
  failures.push('Android Media3 must apply the saved audio/subtitle preferences.');
}
if (!appleShell.includes('applyMediaPreferences(to: playerItem)') ||
    !appleShell.includes('playerItem.select(option, in: group)') ||
    !appleShell.includes('playerItem.select(nil, in: group)')) {
  failures.push('Apple AVPlayer must apply the saved audio/subtitle preferences.');
}
if (!web.includes("preference('veloraAudioLanguage', 'auto')") ||
    !web.includes('AudioStreamIndex=') ||
    !web.includes("preference('veloraSubtitleMode', 'off')") ||
    !web.includes('SubtitleStreamIndex=')) {
  failures.push('Web playback must apply the saved audio/subtitle preferences.');
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log('Playback preference contract passed: Android Media3, Apple AVPlayer and web apply audio/subtitle settings.');
}
