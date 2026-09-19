#!/usr/bin/env node

/**
 * Reconciles QA debug APKs only on an existing, published prerelease.
 * Deliberately requires an explicit GH_TOKEN/GITHUB_TOKEN; it never reads
 * Git credential-manager entries and never prints the token.
 * The manifest records the checkout HEAD and captured APK bytes; it does not
 * independently prove that a local APK was built from that commit.
 * Asset renames are not atomic. Rollback is best effort and retains originals
 * by ID without deleting assets on a staging or promotion failure.
 */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { pathToFileURL } from 'node:url';

const execFileAsync = promisify(execFile);

async function cleanHead(root) {
  // Includes staged, unstaged, untracked and submodule changes, but not ignored build output.
  const { stdout: status } = await execFileAsync('git', ['-C', root, 'status', '--porcelain=v1', '--untracked-files=all', '--ignore-submodules=none']);
  if (status.trim()) throw new Error('Hay cambios sin commit; no se pueden publicar APK QA.');
  const { stdout } = await execFileAsync('git', ['-C', root, 'rev-parse', '--verify', 'HEAD']);
  return stdout.trim();
}

function payload(remoteName, body, contentType) {
  return { remoteName, body, contentType, size: body.length, sha256: crypto.createHash('sha256').update(body).digest('hex') };
}

function verifyAsset(actual, expected, name, id = actual?.id) {
  if (!Number.isSafeInteger(id) || id <= 0 || actual?.id !== id || actual.name !== name || actual.state !== 'uploaded' ||
      actual.size !== expected.size || actual.digest !== `sha256:${expected.sha256}`) {
    throw new Error(`Verificación GitHub fallida (id/nombre/estado/tamaño/digest): ${name}`);
  }
}

export async function reconcileAndroidQa({ root = process.cwd(), env = process.env, fetchImpl = globalThis.fetch, log = console.log, warn = console.warn } = {}) {
  const repository = env.GITHUB_REPOSITORY || 'klortekhq/Velora';
  const tag = env.VELORA_RELEASE_TAG || 'v1.4.0';
  const token = env.GH_TOKEN || env.GITHUB_TOKEN;
  if (!token) throw new Error('Define GH_TOKEN o GITHUB_TOKEN explícitamente antes de ejecutar este script.');
  if (!/^v\d+\.\d+\.0$/.test(tag)) throw new Error(`Solo se permiten etiquetas vX.Y.0: ${tag}`);
  if (!/^[\w.-]+\/[\w.-]+$/.test(repository)) throw new Error('GITHUB_REPOSITORY no válido.');
  if (!/^[1-9]\d*$/.test(env.GITHUB_RUN_ID || '') || !/^[1-9]\d*$/.test(env.GITHUB_RUN_ATTEMPT || '1')) {
    throw new Error('Se requiere GITHUB_RUN_ID (y GITHUB_RUN_ATTEMPT válido) para trazar el workflow QA.');
  }

  const commit = await cleanHead(root);
  const apks = [];
  for (const target of ['mobile', 'tv']) {
    const localPath = path.join(root, 'app', 'build', 'outputs', 'apk', target, 'debug', `app-${target}-debug.apk`);
    if (!(await fs.stat(localPath)).isFile()) throw new Error(`APK QA no es un archivo: ${localPath}`);
    // Hash and upload the same bytes, even if another build replaces the local file later.
    const body = await fs.readFile(localPath);
    if (!body.length) throw new Error(`APK QA vacío: ${localPath}`);
    apks.push({ ...payload(`Velora-${target}-grouped-live-tv-20260910.apk`, body, 'application/vnd.android.package-archive'), variant: `${target}Debug` });
  }

  const manifest = {
    schemaVersion: 1,
    repository,
    releaseTag: tag,
    commit,
    buildVariant: 'QA debug',
    workflowUrl: `https://github.com/${repository}/actions/runs/${env.GITHUB_RUN_ID}/attempts/${env.GITHUB_RUN_ATTEMPT || '1'}`,
    apks: apks.map(({ remoteName, variant, sha256, size }) => ({ name: remoteName, variant, sha256, size })),
  };
  const hashes = apks.map(({ sha256, remoteName }) => `${sha256}  ${remoteName}`);
  const uploadPlan = [
    ...apks,
    payload(`SHA256SUMS-android-${tag}.txt`, Buffer.from(`${hashes.join('\n')}\n`), 'text/plain; charset=utf-8'),
    payload(`android-qa-manifest-${tag}.json`, Buffer.from(`${JSON.stringify(manifest, null, 2)}\n`), 'application/json'),
  ];
  const apiRoot = `https://api.github.com/repos/${repository}`;
  const headers = { Accept: 'application/vnd.github+json', Authorization: `Bearer ${token}`, 'X-GitHub-Api-Version': '2022-11-28' };
  async function github(url, options = {}) {
    const response = await fetchImpl(url, { ...options, headers: { ...headers, ...(options.headers || {}) } });
    if (!response.ok) throw new Error(`GitHub ${response.status} en ${url}`);
    return response.status === 204 ? null : response.json();
  }
  async function publishedPrerelease() {
    const release = await github(`${apiRoot}/releases/tags/${encodeURIComponent(tag)}`);
    if (release.prerelease !== true || release.draft !== false || release.tag_name !== tag) {
      throw new Error(`${tag} debe ser una pre-release publicada.`);
    }
    return release;
  }
  async function listAssets(releaseId) {
    const assets = [];
    for (let page = 1; ; page++) {
      const batch = await github(`${apiRoot}/releases/${releaseId}/assets?per_page=100&page=${page}`);
      assets.push(...batch);
      if (batch.length < 100) return assets;
    }
  }
  async function rename(id, name) {
    const result = await github(`${apiRoot}/releases/assets/${id}`, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name }),
    });
    if (result?.id !== id || result.name !== name) throw new Error(`Renombrado GitHub no confirmado: ${id} -> ${name}`);
    return result;
  }
  async function restoreName(id, name) {
    let renameError;
    try { await rename(id, name); }
    catch (error) { renameError = error; }
    // A failed response may hide a successful rename (or an already-correct
    // name). Conversely, a PATCH response alone does not verify restoration.
    const actual = await github(`${apiRoot}/releases/assets/${id}`);
    if (actual?.id !== id || actual.name !== name) {
      throw new Error(`Restauración GitHub no confirmada: ${id} -> ${name}`, { cause: renameError });
    }
    return actual;
  }
  async function unchangedHead() {
    if (await cleanHead(root) !== commit) throw new Error('HEAD cambió durante la preparación de los APK QA.');
  }

  await unchangedHead();
  const release = await publishedPrerelease();
  const existingAssets = await listAssets(release.id);
  const transaction = crypto.randomUUID();
  const staged = [];
  // Never touch existing names until every upload (including metadata) is verified.
  try {
    for (const asset of uploadPlan) {
      const stagingName = `${asset.remoteName}.qa-staging-${transaction}`;
      const uploaded = await github(`https://uploads.github.com/repos/${repository}/releases/${release.id}/assets?name=${encodeURIComponent(stagingName)}`, {
        method: 'POST', headers: { 'Content-Type': asset.contentType }, body: asset.body,
      });
      verifyAsset(uploaded, asset, stagingName);
      staged.push({ ...asset, id: uploaded.id, stagingName });
    }
    await unchangedHead();
    if ((await publishedPrerelease()).id !== release.id) throw new Error('La pre-release QA cambió durante la subida.');
  } catch (error) {
    throw new Error(`${error.message} Originales sin modificar; pueden quedar assets .qa-staging-${transaction}.`, { cause: error });
  }

  const backups = [];
  try {
    for (const asset of staged) {
      const existing = existingAssets.find((candidate) => candidate.name === asset.remoteName);
      if (existing) {
        const backup = { id: existing.id, remoteName: asset.remoteName, backupName: `${asset.remoteName}.qa-backup-${transaction}`, original: existing };
        // Record BEFORE requesting the rename: GitHub may apply it and lose the response.
        backups.push(backup);
        await rename(backup.id, backup.backupName);
      }
    }
    for (const asset of staged) {
      verifyAsset(await rename(asset.id, asset.remoteName), asset, asset.remoteName, asset.id);
    }
    // Read back the complete promoted set before deleting any previous asset.
    for (const asset of staged) {
      verifyAsset(await github(`${apiRoot}/releases/assets/${asset.id}`), asset, asset.remoteName, asset.id);
    }
  } catch (error) {
    const rollbackFailures = [];
    // Re-stage all new IDs, including a rename whose response may have been lost.
    for (const asset of staged) {
      try { await restoreName(asset.id, asset.stagingName); }
      catch (rollbackError) { rollbackFailures.push(`${asset.id} (${asset.stagingName}): ${rollbackError.message}`); }
    }
    for (const backup of [...backups].reverse()) {
      try {
        const actual = await restoreName(backup.id, backup.remoteName);
        // Old releases may lack digest metadata; compare it when available.
        if (actual.size !== backup.original.size || actual.state !== backup.original.state ||
            (backup.original.digest != null && actual.digest !== backup.original.digest)) {
          throw new Error(`Metadatos del original no confirmados: ${backup.id}`);
        }
      }
      catch (rollbackError) { rollbackFailures.push(`${backup.id} (${backup.backupName}): ${rollbackError.message}`); }
    }
    // Never delete on failure, even if GitHub cannot confirm restoration.
    const rollback = rollbackFailures.length
      ? `Rollback incompleto; no se han eliminado originales; comprobar IDs (pueden seguir como .qa-backup-${transaction}): ${rollbackFailures.join('; ')}`
      : 'Nombres originales restaurados; uploads temporales conservados.';
    throw new Error(`${error.message} ${rollback}`, { cause: error });
  }

  const retainedBackups = [];
  for (const backup of backups) {
    try { await github(`${apiRoot}/releases/assets/${backup.id}`, { method: 'DELETE' }); }
    catch (error) {
      retainedBackups.push(backup);
      warn(`Publicación QA verificada; no se confirmó la eliminación del backup ${backup.id} (${backup.backupName}): ${error.message}`);
    }
  }
  for (const asset of staged) log(`Publicado y verificado: ${asset.remoteName}`);
  log(`Release QA reconciliada: ${tag}; commit ${commit}`);
  for (const hash of hashes) log(hash);
  return { manifest, retainedBackups };
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  await reconcileAndroidQa();
}
