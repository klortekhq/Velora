#!/usr/bin/env node

/**
 * Replaces the two Android QA assets for an existing Velora prerelease.
 * Deliberately requires an explicit GH_TOKEN/GITHUB_TOKEN; it never reads
 * Git credential-manager entries and never prints the token.
 */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';

const repository = process.env.GITHUB_REPOSITORY || 'klortekhq/Velora';
const tag = process.env.VELORA_RELEASE_TAG || 'v1.4.0';
const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN;
if (!token) throw new Error('Define GH_TOKEN o GITHUB_TOKEN explícitamente antes de ejecutar este script.');
if (!/^v\d+\.\d+\.0$/.test(tag)) throw new Error(`Solo se permiten etiquetas vX.Y.0: ${tag}`);

const root = process.cwd();
const assets = [
  { localPath: path.join(root, 'app', 'build', 'outputs', 'apk', 'mobile', 'debug', 'app-mobile-debug.apk'), remoteName: 'Velora-mobile-grouped-live-tv-20260910.apk', contentType: 'application/vnd.android.package-archive' },
  { localPath: path.join(root, 'app', 'build', 'outputs', 'apk', 'tv', 'debug', 'app-tv-debug.apk'), remoteName: 'Velora-tv-grouped-live-tv-20260910.apk', contentType: 'application/vnd.android.package-archive' },
];
const apiRoot = `https://api.github.com/repos/${repository}`;
const headers = { Accept: 'application/vnd.github+json', Authorization: `Bearer ${token}`, 'X-GitHub-Api-Version': '2022-11-28' };

async function github(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { ...headers, ...(options.headers || {}) } });
  if (!response.ok) throw new Error(`GitHub ${response.status} en ${url}: ${(await response.text()).slice(0, 240)}`);
  return response.status === 204 ? null : response.json();
}
async function sha256(filePath) {
  const digest = crypto.createHash('sha256');
  digest.update(await fs.readFile(filePath));
  return digest.digest('hex');
}

const release = await github(`${apiRoot}/releases/tags/${encodeURIComponent(tag)}`);
if (!release.prerelease || release.draft) throw new Error(`${tag} debe ser una pre-release publicada.`);
const hashes = [];
for (const asset of assets) {
  await fs.access(asset.localPath);
  hashes.push(`${await sha256(asset.localPath)}  ${asset.remoteName}`);
}

const checksumName = `SHA256SUMS-android-${tag}.txt`;
const checksumPath = path.join(root, '.velora-sha256sums-android.txt');
await fs.writeFile(checksumPath, `${hashes.join('\n')}\n`, 'utf8');
const uploadPlan = [...assets, { localPath: checksumPath, remoteName: checksumName, contentType: 'text/plain; charset=utf-8' }];
try {
  for (const asset of uploadPlan) {
    const existing = release.assets.find((candidate) => candidate.name === asset.remoteName);
    if (existing) await github(`${apiRoot}/releases/assets/${existing.id}`, { method: 'DELETE' });
    const uploadUrl = `https://uploads.github.com/repos/${repository}/releases/${release.id}/assets?name=${encodeURIComponent(asset.remoteName)}`;
    await github(uploadUrl, { method: 'POST', headers: { 'Content-Type': asset.contentType }, body: await fs.readFile(asset.localPath) });
    console.log(`Publicado: ${asset.remoteName}`);
  }
  console.log(`Release reconciliada: ${tag}`);
  for (const hash of hashes) console.log(hash);
} finally {
  await fs.rm(checksumPath, { force: true });
}
