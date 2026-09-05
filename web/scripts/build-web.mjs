import { cp, rm, mkdir, copyFile, writeFile } from 'node:fs/promises';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { readFile } from 'node:fs/promises';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repo = resolve(root, '..');
const out = resolve(repo, 'outputs', 'web');
const target = process.argv[2] || 'all';
const requireInstallable = process.env.VELORA_REQUIRE_INSTALLABLE_PACKAGES === '1';
const packageMetadata = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'));
const version = packageMetadata.version;

function command(name, args, cwd) {
  const result = spawnSync(name, args, {
    stdio: 'inherit',
    cwd,
    shell: process.platform === 'win32'
  });
  return result.status === 0;
}

async function copyCommon(destination) {
  await mkdir(destination, { recursive: true });
  for (const file of ['app.js', 'platform.js', 'index.html', 'manifest.webmanifest', 'styles.css', 'README.md', 'package.json', 'media-proxy-sw.js']) {
    await copyFile(join(root, file), join(destination, file));
  }
}

async function makeLegacyBrowserCompatible(destination) {
  for (const file of ['platform.js', 'app.js']) {
    const compat = `${file}.compat.js`;
    const built = command('esbuild', [file, '--target=es2017', `--outfile=${compat}`], destination);
    if (!built) {
      if (requireInstallable) throw new Error(`No se pudo transpilar ${file} para el destino ${destination}`);
      continue;
    }
    await copyFile(join(destination, compat), join(destination, file));
    await rm(join(destination, compat), { force: true });
  }
}

async function writeBuildMetadata(destination, metadata) {
  await writeFile(join(destination, 'velora-build.json'), JSON.stringify(metadata, null, 2) + '\n', 'utf8');
}

await rm(out, { recursive: true, force: true });
await mkdir(out, { recursive: true });
await copyCommon(out);
await cp(join(root, 'platforms'), join(out, 'platforms'), { recursive: true });

const results = [];

// Samsung TV is the Tizen target. Keep `samsung` as the historical output
// name and accept `tizen` as an explicit platform alias for CI/store work.
if (target === 'samsung' || target === 'tizen' || target === 'all') {
  const stage = resolve(out, 'samsung');
  await copyCommon(stage);
  await copyFile(join(root, 'platforms', 'samsung', 'config.xml'), join(stage, 'config.xml'));
  await copyFile(join(root, 'platforms', 'samsung', 'config.json'), join(stage, 'config.json'));
  await copyFile(join(root, 'platforms', 'samsung', 'icon.png'), join(stage, 'icon.png'));
  await makeLegacyBrowserCompatible(stage);
  const built = command('tizen', ['build-web', '--', '.'], stage);
  const packaged = built && command('tizen', ['package', '-t', 'wgt', '--', '.buildResult'], stage);
  if (requireInstallable && !packaged) {
    throw new Error('Tizen Studio/CLI o un perfil de firma no está disponible; no se genera un WGT publicable');
  }
  results.push(packaged
    ? 'Samsung WGT generado con Tizen CLI'
    : 'Samsung bundle preparado; falta Tizen Studio/CLI o un perfil de firma');
}

if (target === 'webos' || target === 'all') {
  const stage = resolve(out, 'webos');
  await copyCommon(stage);
  await copyFile(join(root, 'platforms', 'webos', 'appinfo.json'), join(stage, 'appinfo.json'));
  await copyFile(join(root, 'platforms', 'webos', 'icon.png'), join(stage, 'icon.png'));
  await makeLegacyBrowserCompatible(stage);
  results.push(command('ares-package', ['.'], stage)
    ? 'webOS IPK generado con ares-package'
    : 'webOS bundle preparado; falta ares-package o certificado');
}

if (target === 'vidaa' || target === 'all') {
  const stage = resolve(out, 'vidaa');
  await copyCommon(stage);
  await copyFile(join(root, 'platforms', 'vidaa', 'submission.json'), join(stage, 'submission.json'));
  await copyFile(join(root, 'platforms', 'vidaa', 'README.md'), join(stage, 'VIDAA_README.md'));
  await makeLegacyBrowserCompatible(stage);
  await writeBuildMetadata(stage, {
    product: 'Velora',
    version,
    platform: 'Hisense VIDAA',
    kind: 'hosted-html5',
    entry: 'index.html',
    generatedAt: new Date().toISOString(),
    installablePackage: false,
    note: 'La publicación oficial se tramita en el portal VIDAA y depende del modelo, región y certificado del fabricante.'
  });
  results.push('VIDAA bundle HTML5 preparado para portal/tienda; no existe un paquete genérico firmado');
}

console.log(`Velora web preparado en ${out}`);
for (const result of results) console.log(`- ${result}`);
