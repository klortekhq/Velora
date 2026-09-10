import { cp, rm, mkdir, copyFile, writeFile } from 'node:fs/promises';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repo = resolve(root, '..');
// Allow CI and local verification to use an isolated output directory. This
// avoids stale/locked generated files affecting a later build while keeping
// the normal repository output unchanged.
const out = resolve(process.env.VELORA_WEB_OUTPUT_DIR || join(repo, 'outputs', 'web'));
const target = process.argv[2] || 'all';
const requireInstallable = process.env.VELORA_REQUIRE_INSTALLABLE_PACKAGES === '1';
const packageMetadata = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'));
const version = packageMetadata.version;
const sourceDateEpoch = Number.parseInt(process.env.SOURCE_DATE_EPOCH || '', 10);
const generatedAt = Number.isFinite(sourceDateEpoch) && sourceDateEpoch >= 0
  ? new Date(sourceDateEpoch * 1000).toISOString()
  : undefined;

function resolveCommand(name) {
  if (process.platform !== 'win32') {
    const lookup = spawnSync('which', [name], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
    if (lookup.status === 0) {
      const resolved = lookup.stdout.trim().split(/\r?\n/)[0];
      if (resolved) return resolved;
    }
    return null;
  }

  // PowerShell/npm installations can expose only a .cmd or .ps1 shim, while
  // `where.exe` may ignore it depending on PATHEXT. Resolve those shims
  // explicitly so the secure shell-free runner does not lose available CLIs.
  // Prefer real executables and npm's .cmd shims. PowerShell may put a .ps1
  // shim first in PATH; it is not a native executable for spawnSync.
  const preferredExtensions = ['.COM', '.EXE', '.BAT', '.CMD'];
  const pathext = (process.env.PATHEXT || '').split(';')
    .map((extension) => extension.toUpperCase())
    .filter((extension) => extension && !preferredExtensions.includes(extension));
  const extensions = [...preferredExtensions, ...pathext, ''];
  const searchDirectories = [
    ...(process.env.PATH || '').split(';'),
    process.env.APPDATA ? join(process.env.APPDATA, 'npm') : '',
    process.env.LOCALAPPDATA ? join(process.env.LOCALAPPDATA, 'npm') : ''
  ].filter(Boolean);
  for (const directory of searchDirectories) {
    for (const extension of extensions) {
      const candidate = join(directory, `${name}${extension.toLowerCase()}`);
      if (existsSync(candidate)) return candidate;
    }
  }
  return null;
}

function quoteWindowsArg(value) {
  const text = String(value);
  if (!/[\s"]/.test(text)) return text;
  return `"${text.replace(/(\\*)"/g, '$1$1\\"').replace(/(\\*)$/g, '$1$1')}"`;
}

function runCommand(executable, args, cwd) {
  if (process.platform === 'win32' && /\.ps1$/i.test(executable)) {
    const powershell = process.env.SystemRoot
      ? join(process.env.SystemRoot, 'System32', 'WindowsPowerShell', 'v1.0', 'powershell.exe')
      : 'powershell.exe';
    return spawnSync(powershell, [
      '-NoLogo', '-NoProfile', '-NonInteractive',
      '-ExecutionPolicy', 'Bypass', '-File', executable, ...args
    ], {
      stdio: 'inherit',
      cwd,
      shell: false
    });
  }
  if (process.platform === 'win32' && /\.(?:cmd|bat)$/i.test(executable)) {
    const commandLine = [executable, ...args].map(quoteWindowsArg).join(' ');
    return spawnSync(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', commandLine], {
      stdio: 'inherit',
      cwd,
      shell: false
    });
  }
  return spawnSync(executable, args, {
    stdio: 'inherit',
    cwd,
    shell: false
  });
}

function command(name, args, cwd) {
  // Missing optional vendor CLIs are a supported QA state: the build should
  // leave a truthful bundle and metadata instead of printing a shell error.
  const executable = resolveCommand(name);
  if (!executable) return false;
  const result = runCommand(executable, args, cwd);
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
  const reproducibleMetadata = generatedAt ? { ...metadata, generatedAt } : metadata;
  await writeFile(join(destination, 'velora-build.json'), JSON.stringify(reproducibleMetadata, null, 2) + '\n', 'utf8');
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
  await writeBuildMetadata(stage, {
    product: 'Velora',
    version,
    platform: 'Samsung Tizen',
    kind: 'web',
    entry: 'index.html',
    installablePackage: packaged,
    packaging: packaged ? 'wgt' : 'bundle',
    requiresTizenStudio: true,
    note: packaged
      ? 'Paquete WGT generado por Tizen CLI; la firma y validación en dispositivo siguen siendo requisitos de tienda.'
      : 'Bundle preparado; falta Tizen Studio/CLI o un perfil de firma.'
  });
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
  const packaged = command('ares-package', ['.'], stage);
  await writeBuildMetadata(stage, {
    product: 'Velora',
    version,
    platform: 'LG webOS',
    kind: 'web',
    entry: 'index.html',
    installablePackage: packaged,
    packaging: packaged ? 'ipk' : 'bundle',
    requiresAres: true,
    note: packaged
      ? 'Paquete IPK generado por ares-package; la firma y validación en dispositivo siguen siendo requisitos de tienda.'
      : 'Bundle preparado; falta ares-package o certificado.'
  });
  results.push(packaged
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
    installablePackage: false,
    packaging: 'hosted-html5',
    note: 'La publicación oficial se tramita en el portal VIDAA y depende del modelo, región y certificado del fabricante.'
  });
  results.push('VIDAA bundle HTML5 preparado para portal/tienda; no existe un paquete genérico firmado');
}

console.log(`Velora web preparado en ${out}`);
for (const result of results) console.log(`- ${result}`);
