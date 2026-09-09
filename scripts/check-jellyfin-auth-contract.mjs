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

console.log('Jellyfin authentication contract consistent: Pw');
