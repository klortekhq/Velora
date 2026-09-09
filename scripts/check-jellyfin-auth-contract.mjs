import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();
const files = {
  android: path.join(root, 'app/src/main/java/com/klortek/velora/jellyfin/JellyfinAuthService.kt'),
  apple: path.join(root, 'apple/Sources/VeloraKit/JellyfinClient.swift'),
  web: path.join(root, 'web/app.js'),
  smoke: path.join(root, 'scripts/qa/jellyfin-smoke.ps1')
};

const required = {
  android: 'val Pw: String',
  apple: '["Username": username, "Pw": password]',
  web: 'JSON.stringify({ Username: username, Pw:',
  smoke: 'Username = $Username; Pw = $Password'
};

const canonicalRoutes = {
  android: 'Users/AuthenticateByName',
  apple: 'Users/AuthenticateByName',
  web: '/Users/AuthenticateByName',
  smoke: '/Users/AuthenticateByName'
};

const forbidden = {
  apple: '["Username": username, "Password": password]',
  web: 'JSON.stringify({ Username: username, Password:'
};

const jellyfin12Files = [
  path.join(root, 'app/src/main/java'),
  path.join(root, 'apple/Sources'),
  path.join(root, 'web'),
  path.join(root, 'scripts/qa')
];

for (const [name, file] of Object.entries(files)) {
  const source = fs.readFileSync(file, 'utf8');
  if (!source.includes(required[name])) {
    throw new Error(`${name}: falta el campo Jellyfin Pw en ${path.relative(root, file)}`);
  }
  if (!source.includes(canonicalRoutes[name])) {
    throw new Error(`${name}: falta la ruta canónica Users/AuthenticateByName`);
  }
  if (forbidden[name] && source.includes(forbidden[name])) {
    throw new Error(`${name}: todavía usa Password en el payload de AuthenticateByName`);
  }
}

function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const file = path.join(dir, entry.name);
    return entry.isDirectory() ? walk(file) : [file];
  });
}

for (const directory of jellyfin12Files) {
  for (const file of walk(directory)) {
    if (!/\.(kt|swift|js|ps1)$/.test(file)) continue;
    const source = fs.readFileSync(file, 'utf8');
    if (/X-Emby-(?:Authorization|Token|Client)/.test(source)) {
      throw new Error(`Jellyfin 12: cabecera legacy encontrada en ${path.relative(root, file)}`);
    }
  }
}

console.log('Jellyfin authentication contract consistent: Pw + Authorization MediaBrowser; no legacy X-Emby headers');
