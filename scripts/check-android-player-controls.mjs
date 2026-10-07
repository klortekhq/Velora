import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve('app/src/main/java/com/klortek/velora/screens/JellyfinVideoPlayerScreen.kt');
const source = fs.readFileSync(root, 'utf8');

function requireMatch(pattern, message) {
  assert.match(source, pattern, message);
}

// These are product controls, not decorative labels. Keep the contract close
// to the screen so a future player refactor cannot silently disconnect them.
requireMatch(/AspectModeButton\([\s\S]*?showAspectModeMenu\s*=\s*true/, 'El selector de aspecto debe abrir su menú real.');
requireMatch(/currentAspectModeName\s*=\s*mode\.name[\s\S]*?showAspectModeMenu\s*=\s*false/, 'El menú de aspecto debe guardar la opción elegida.');
requireMatch(/LaunchedEffect\(playerInitialized,\s*currentAspectMode\)[\s\S]*?applyAspectModeToPlayerView/, 'El modo de aspecto debe aplicarse al PlayerView real.');
requireMatch(/applyAspectModeToPlayerView\([\s\S]*?requestLayout\(\)[\s\S]*?invalidate\(\)/, 'El cambio de aspecto debe solicitar una nueva medición del vídeo.');
requireMatch(/Icons\.Filled\.Fullscreen[\s\S]*?onClick\s*=\s*\{\s*toggleMobileFullscreen\(\)\s*\}/, 'El botón móvil de pantalla completa debe cambiar la orientación/pantalla.');
requireMatch(/Icons\.Filled\.FullscreenExit[\s\S]*?onClick\s*=\s*\{\s*toggleMobileFullscreen\(\)\s*\}/, 'El botón móvil de salida de pantalla completa debe ser accionable.');
requireMatch(/icon\s*=\s*Icons\.Filled\.Settings[\s\S]*?showSettingsMenu\s*=\s*true/, 'La tuerca debe abrir el menú unificado de audio y subtítulos.');
requireMatch(/if\s*\(showSettingsMenu\)\s*\{[\s\S]*?ExoPlayerSettingsMenu\(/, 'El menú de ajustes debe renderizarse cuando se solicita.');

console.log('Android player controls contract passed: aspect, fullscreen, and unified settings are wired to real actions.');
