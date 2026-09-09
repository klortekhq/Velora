import fs from 'node:fs';
const roots = [
  'apple/Sources/VeloraMobile/main.swift',
  'apple/Sources/VeloraTV/main.swift'
];

for (const file of roots) {
  const source = fs.readFileSync(file, 'utf8');
  if (!source.includes('@State private var shell: VeloraAppShell?')) {
    throw new Error(`${file}: el shell Apple debe conservarse en @State`);
  }
  if (/body\s*:\s*some View[\s\S]*try\?\s+VeloraAppShell/.test(source)) {
    throw new Error(`${file}: no se debe crear VeloraAppShell durante cada body`);
  }
}

console.log('Apple lifecycle contract passed: VeloraAppShell is created once and retained by SwiftUI state.');
