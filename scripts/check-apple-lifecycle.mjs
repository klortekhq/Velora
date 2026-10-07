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

const offline = fs.readFileSync('apple/Sources/VeloraKit/VeloraOffline.swift', 'utf8');
const shell = fs.readFileSync('apple/Sources/VeloraKit/VeloraAppShell.swift', 'utf8');
if (!offline.includes('URLSessionConfiguration.background') ||
    !offline.includes('didWriteData bytesWritten') ||
    !offline.includes('cancel(byProducingResumeData:') ||
    !offline.includes('downloadTask(withResumeData:') ||
    !offline.includes('VeloraOfflineActiveTransfer')) {
  throw new Error('Apple offline: falta transferencia en segundo plano con progreso y pausa/reanudación');
}
if (!shell.includes('offlineTransferProgress') ||
    !shell.includes('pauseDownload()') ||
    !shell.includes('resumeDownload()') ||
    !shell.includes('activeOfflineTaskID = active.taskIdentifier')) {
  throw new Error('Apple offline: el shell no expone progreso y pausa/reanudación');
}

console.log('Apple lifecycle/offline contract passed: shell persistente y transferencias background con pausa/reanudación.');
