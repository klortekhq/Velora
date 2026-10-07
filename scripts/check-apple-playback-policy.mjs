import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const models = fs.readFileSync(
  path.join(root, 'apple/Sources/VeloraKit/VeloraModels.swift'),
  'utf8'
);
const tests = fs.readFileSync(
  path.join(root, 'apple/Tests/VeloraKitTests/VeloraKitTests.swift'),
  'utf8'
);

const requiredCoverage = ['x265', 'mpeg transport stream', 'H265', 'EC-3', 'matroska', 'Dolby Vision'];
const failures = [];

if (!models.includes('public static func canonicalCapability')) {
  failures.push('Apple playback must expose one canonical capability normalizer.');
}
if (!models.includes('PlaybackDecisionEngine.canonicalCapability')) {
  failures.push('Apple capability sets must be normalized at construction time.');
}
if (!models.includes('values.contains(canonicalCapability(value))')) {
  failures.push('Apple source capabilities must be normalized before comparison.');
}
for (const input of requiredCoverage) {
  if (!tests.toLowerCase().includes(input.toLowerCase())) {
    failures.push(`Apple playback regression is missing coverage for ${input}.`);
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log('Apple playback capability contract passed: aliases are canonicalized and regression-covered.');
}
