import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { createHash } from 'node:crypto';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { mock, test } from 'node:test';
import { promisify } from 'node:util';
import { reconcileAndroidQa } from '../reconcile-android-qa.mjs';

const execFileAsync = promisify(execFile);
mock.method(globalThis, 'fetch', () => { throw new Error('Live network forbidden in QA publisher tests'); });
const repository = 'fixture/qa';
const tag = 'v1.4.0';
const names = [
  'Velora-mobile-grouped-live-tv-20260910.apk',
  'Velora-tv-grouped-live-tv-20260910.apk',
  `SHA256SUMS-android-${tag}.txt`,
  `android-qa-manifest-${tag}.json`,
];
const digest = (body) => createHash('sha256').update(body).digest('hex');
const reply = (status, data) => new Response(status === 204 ? null : JSON.stringify(data), {
  status, headers: { 'Content-Type': 'application/json' },
});
const metadata = ({ body, ...asset }) => ({ ...asset });

// No live fetch fallback. Model persistent IDs, bytes, pagination and GitHub's
// unique-name constraint, with faults before OR after a request takes effect.
function mockGitHub({ before, after, fillerCount = 0, originalCount = names.length } = {}) {
  const assets = new Map();
  let nextId = 1;
  function add(name, body) {
    const asset = { id: nextId++, name, body: Buffer.from(body), state: 'uploaded', size: body.length, digest: `sha256:${digest(body)}` };
    assets.set(asset.id, asset);
    return asset;
  }
  for (let i = 0; i < fillerCount; i++) add(`unrelated-${i}.txt`, Buffer.from(`unrelated ${i}`));
  const originals = names.slice(0, originalCount).map((name) => {
    const asset = add(name, Buffer.from(`old bytes: ${name}`));
    // Older GitHub assets may not have a server digest.
    asset.digest = null;
    return { ...asset, body: Buffer.from(asset.body) };
  });
  const unrelated = add('Velora-mobile-release.apk', Buffer.from('signed release: do not touch'));
  const calls = [];
  const release = { id: 42, tag_name: tag, prerelease: true, draft: false };
  const apiPath = `/repos/${repository}/releases`;
  const mock = { assets, originals, unrelated: { ...unrelated }, calls, release };
  mock.fetchImpl = async (input, options = {}) => {
    const url = new URL(input);
    assert.ok(['api.github.com', 'uploads.github.com'].includes(url.hostname), 'unexpected host');
    assert.equal(options.headers.Authorization, 'Bearer fixture-token');
    assert.equal(options.headers['X-GitHub-Api-Version'], '2022-11-28');
    const method = options.method || 'GET';
    const idMatch = url.pathname.match(/\/releases\/assets\/(\d+)$/);
    const id = idMatch ? Number(idMatch[1]) : undefined;
    const name = method === 'PATCH' ? JSON.parse(options.body).name : url.searchParams.get('name');
    const call = { method, id, name, url, options };
    calls.push(call);
    const early = await before?.(call, mock);
    if (early !== undefined) return early;
    let status = 200;
    let data;
    if (method === 'GET' && url.pathname === `${apiPath}/tags/${tag}`) {
      data = { ...release };
    } else if (method === 'GET' && url.pathname === `${apiPath}/42/assets`) {
      assert.equal(url.searchParams.get('per_page'), '100');
      const start = (Number(url.searchParams.get('page')) - 1) * 100;
      data = [...assets.values()].slice(start, start + 100).map(metadata);
    } else if (method === 'POST' && url.hostname === 'uploads.github.com' && url.pathname === `${apiPath}/42/assets`) {
      if ([...assets.values()].some((asset) => asset.name === name)) return reply(422, { message: 'name already exists' });
      data = metadata(add(name, Buffer.from(options.body)));
      status = 201;
    } else if (id !== undefined && url.pathname === `${apiPath}/assets/${id}`) {
      const asset = assets.get(id);
      if (!asset) return reply(404, { message: 'not found' });
      if (method === 'GET') data = metadata(asset);
      else if (method === 'PATCH') {
        if ([...assets.values()].some((other) => other.id !== id && other.name === name)) return reply(422, { message: 'name already exists' });
        asset.name = name;
        data = metadata(asset);
      } else if (method === 'DELETE') {
        assets.delete(id);
        status = 204;
      } else assert.fail(`Unexpected asset method: ${method}`);
    } else assert.fail(`Unexpected mock GitHub request: ${method} ${url}`);
    const late = await after?.(call, mock, data);
    return late === undefined ? reply(status, data) : late;
  };
  return mock;
}

async function fixture(t, mockOptions) {
  const tempParent = await fs.realpath(os.tmpdir());
  const root = await fs.mkdtemp(path.join(tempParent, 'velora-android-qa-test-'));
  t.after(async () => {
    // Only this test's newly created temporary directory can be removed.
    assert.equal(path.dirname(root), tempParent);
    assert.ok(path.basename(root).startsWith('velora-android-qa-test-'));
    await fs.rm(root, { recursive: true, force: true, maxRetries: 3 });
  });
  const gitEnv = Object.fromEntries(Object.entries(process.env).filter(([key]) => !key.startsWith('GIT_')));
  Object.assign(gitEnv, { GIT_CONFIG_NOSYSTEM: '1', GIT_CONFIG_GLOBAL: path.join(root, 'no-global-config') });
  const git = async (...args) => (await execFileAsync('git', ['-C', root, ...args], { env: gitEnv })).stdout.trim();
  // These commits belong only to disposable test fixtures, never the working repo.
  await git('init', '--quiet', '--initial-branch=main');
  await git('config', 'core.autocrlf', 'false');
  await git('config', 'core.hooksPath', path.join(root, 'no-hooks'));
  await git('config', 'user.name', 'QA fixture');
  await git('config', 'user.email', 'fixture@example.invalid');
  await git('config', 'commit.gpgsign', 'false');
  await fs.writeFile(path.join(root, '.gitignore'), '/app/build/\n');
  await fs.writeFile(path.join(root, 'source.txt'), 'fixture source\n');
  await git('add', '.gitignore', 'source.txt');
  await git('commit', '--quiet', '-m', 'fixture');
  const head = await git('rev-parse', 'HEAD');
  const apks = new Map();
  for (const [i, target] of ['mobile', 'tv'].entries()) {
    const localPath = path.join(root, 'app', 'build', 'outputs', 'apk', target, 'debug', `app-${target}-debug.apk`);
    const body = Buffer.from([0x50, 0x4b, 0, i, 0xff, 0x0a, 0x80]);
    await fs.mkdir(path.dirname(localPath), { recursive: true });
    await fs.writeFile(localPath, body);
    apks.set(target, { localPath, body });
  }
  const mock = mockGitHub(mockOptions);
  const logs = [];
  const warnings = [];
  const env = { GH_TOKEN: 'fixture-token', GITHUB_REPOSITORY: repository, VELORA_RELEASE_TAG: tag, GITHUB_RUN_ID: '7654', GITHUB_RUN_ATTEMPT: '2' };
  const run = () => reconcileAndroidQa({ root, env, fetchImpl: mock.fetchImpl, log: (s) => logs.push(s), warn: (s) => warnings.push(s) });
  return { root, git, head, apks, mock, logs, warnings, env, run };
}

function originalsPreserved(mock, { restored = true } = {}) {
  for (const original of mock.originals) {
    const actual = mock.assets.get(original.id);
    assert.ok(actual, `original ${original.id} must still exist`);
    assert.deepEqual(actual.body, original.body, 'original bytes must survive');
    if (restored) assert.equal(actual.name, original.name);
  }
  assert.deepEqual(mock.assets.get(mock.unrelated.id), mock.unrelated);
  assert.equal(mock.calls.filter((call) => call.method === 'DELETE').length, 0, 'failure must never delete assets');
}

async function rejectedRun(f, pattern) {
  await assert.rejects(f.run, pattern);
  assert.deepEqual(f.logs, [], 'failure must not report publication success');
}

test('success publishes exact APK bytes, checksum and manifest with checkout HEAD, then deletes backups', async (t) => {
  const f = await fixture(t, { fillerCount: 100 });
  const { manifest, retainedBackups } = await f.run();
  assert.equal(manifest.commit, f.head);
  assert.equal(manifest.repository, repository);
  assert.equal(manifest.releaseTag, tag);
  assert.equal(manifest.schemaVersion, 1);
  assert.equal(manifest.buildVariant, 'QA debug');
  assert.equal(manifest.workflowUrl, `https://github.com/${repository}/actions/runs/7654/attempts/2`);
  assert.deepEqual(manifest.apks, [...f.apks.entries()].map(([target, { body }], i) => ({
    name: names[i], variant: `${target}Debug`, sha256: digest(body), size: body.length,
  })));
  const published = names.map((name) => [...f.mock.assets.values()].find((asset) => asset.name === name));
  assert.ok(published.every(Boolean));
  for (const [i, { body }] of [...f.apks.values()].entries()) assert.deepEqual(published[i].body, body);
  assert.equal(published[2].body.toString(), manifest.apks.map((apk) => `${apk.sha256}  ${apk.name}\n`).join(''));
  assert.deepEqual(JSON.parse(published[3].body.toString()), manifest);
  for (const asset of published) assert.equal(asset.digest, `sha256:${digest(asset.body)}`);
  assert.deepEqual(retainedBackups, []);
  assert.deepEqual(f.warnings, []);
  assert.deepEqual(f.mock.assets.get(f.mock.unrelated.id), f.mock.unrelated);
  assert.ok(f.mock.originals.every(({ id }) => !f.mock.assets.has(id)));
  assert.equal(f.mock.assets.size, 105, 'unrelated assets are retained');
  assert.ok(f.mock.calls.some(({ url }) => url.searchParams.get('page') === '2'));
  const firstRename = f.mock.calls.findIndex(({ method }) => method === 'PATCH');
  assert.equal(f.mock.calls.slice(0, firstRename).filter(({ method }) => method === 'POST').length, 4);
  const firstDelete = f.mock.calls.findIndex(({ method }) => method === 'DELETE');
  for (const asset of published) {
    assert.ok(f.mock.calls.slice(0, firstDelete).some(({ method, id }) => method === 'GET' && id === asset.id), 'all promoted IDs read back before deletion');
  }
  assert.equal(await f.git('status', '--porcelain=v1', '--untracked-files=all'), '', 'publishing must not dirty checkout');
});

for (const originalCount of [0, 2]) {
  test(`publishes all four assets when the release has ${originalCount} existing QA assets`, async (t) => {
    const f = await fixture(t, { originalCount });
    const { manifest } = await f.run();
    assert.equal(manifest.commit, f.head);
    assert.ok(names.every((name) => [...f.mock.assets.values()].some((asset) => asset.name === name)));
    assert.equal(f.mock.calls.filter(({ method }) => method === 'DELETE').length, originalCount);
    assert.equal(f.mock.assets.size, 5);
    assert.deepEqual(f.mock.assets.get(f.mock.unrelated.id), f.mock.unrelated);
  });
}

for (const dirty of ['unstaged', 'staged', 'untracked']) {
  test(`rejects ${dirty} source changes before GitHub access`, async (t) => {
    const f = await fixture(t);
    await fs.writeFile(path.join(f.root, dirty === 'untracked' ? 'new-source.txt' : 'source.txt'), 'changed\n');
    if (dirty === 'staged') await f.git('add', 'source.txt');
    await rejectedRun(f, /cambios sin commit/);
    assert.equal(f.mock.calls.length, 0);
    originalsPreserved(f.mock);
  });
}

for (const target of ['mobile', 'tv']) {
  for (const invalid of ['missing', 'empty', 'directory']) {
    test(`rejects ${invalid} ${target} APK before GitHub access`, async (t) => {
      const f = await fixture(t);
      const { localPath } = f.apks.get(target);
      if (invalid === 'empty') await fs.writeFile(localPath, Buffer.alloc(0));
      else {
        await fs.unlink(localPath);
        if (invalid === 'directory') await fs.mkdir(localPath);
      }
      await rejectedRun(f, invalid === 'missing' ? /ENOENT/ : invalid === 'empty' ? /APK QA vacío/ : /APK QA no es un archivo/);
      assert.equal(f.mock.calls.length, 0);
      originalsPreserved(f.mock);
    });
  }
}

for (const [field, value] of Object.entries({ id: 0, name: 'wrong.apk', state: 'starter', size: 999, digest: 'sha256:wrong' })) {
  test(`upload ${field} mismatch leaves all originals untouched`, async (t) => {
    const f = await fixture(t, { after: (call, mock, data) => {
      if (call.method === 'POST') return reply(201, { ...data, [field]: value });
    } });
    await rejectedRun(f, /Verificación GitHub fallida.*Originales sin modificar/);
    originalsPreserved(f.mock);
    assert.ok(f.mock.calls.every(({ method }) => method !== 'PATCH'));
  });
}

for (const failedName of names) {
  test(`HTTP upload failure for ${failedName} preserves originals`, async (t) => {
    const f = await fixture(t, { before: (call) => {
      if (call.method === 'POST' && call.name.startsWith(`${failedName}.qa-staging-`)) return reply(502, {});
    } });
    await rejectedRun(f, /GitHub 502.*Originales sin modificar/);
    originalsPreserved(f.mock);
    assert.ok(f.mock.calls.every(({ method }) => method !== 'PATCH'));
  });
}

test('upload applied with a lost response leaves originals untouched and temporary bytes intact', async (t) => {
  const f = await fixture(t, { after: (call) => {
    if (call.method === 'POST') throw new Error('upload response lost');
  } });
  await rejectedRun(f, /upload response lost.*Originales sin modificar/);
  originalsPreserved(f.mock);
  assert.equal([...f.mock.assets.values()].filter(({ name }) => name.includes('.qa-staging-')).length, 1);
});

for (const change of ['dirty tree', 'HEAD']) {
  test(`${change} changing during upload prevents original renames`, async (t) => {
    let changed = false;
    const f = await fixture(t, { after: async (call) => {
      if (call.method !== 'POST' || changed) return;
      changed = true;
      if (change === 'HEAD') await f.git('commit', '--allow-empty', '--quiet', '-m', 'fixture HEAD change');
      else await fs.writeFile(path.join(f.root, 'source.txt'), 'changed during upload\n');
    } });
    await rejectedRun(f, change === 'HEAD' ? /HEAD cambió.*Originales sin modificar/ : /cambios sin commit.*Originales sin modificar/);
    originalsPreserved(f.mock);
    assert.ok(f.mock.calls.every(({ method }) => method !== 'PATCH'));
  });
}

test('hash and upload use the same captured bytes even if ignored APK output changes', async (t) => {
  let changed = false;
  const f = await fixture(t, { before: async () => {
    if (changed) return;
    changed = true;
    for (const { localPath } of f.apks.values()) await fs.writeFile(localPath, 'a later build');
  } });
  const { manifest } = await f.run();
  for (const [i, { body }] of [...f.apks.values()].entries()) {
    assert.equal(manifest.apks[i].sha256, digest(body));
    assert.deepEqual([...f.mock.assets.values()].find(({ name }) => name === names[i]).body, body);
  }
});

for (const releaseChange of [{ prerelease: false }, { draft: true }, { tag_name: 'v9.9.0' }]) {
  test(`rejects unsuitable release ${JSON.stringify(releaseChange)}`, async (t) => {
    const f = await fixture(t);
    Object.assign(f.mock.release, releaseChange);
    await rejectedRun(f, /debe ser una pre-release publicada/);
    originalsPreserved(f.mock);
    assert.ok(f.mock.calls.every(({ method }) => method === 'GET'));
  });
}

test('release replaced during staging prevents promotion', async (t) => {
  const f = await fixture(t, { after: (call, mock) => {
    if (call.method === 'POST') mock.release.id = 99;
  } });
  await rejectedRun(f, /pre-release QA cambió.*Originales sin modificar/);
  originalsPreserved(f.mock);
});

for (const phase of ['backup', 'promotion']) {
  for (const lostResponse of [false, true]) {
    test(`${phase} failure ${lostResponse ? 'after' : 'before'} rename restores original IDs, names and bytes`, async (t) => {
      let failed = false;
      const fault = (call) => {
        const target = phase === 'backup' ? call.name?.startsWith(`${names[1]}.qa-backup-`) : call.name === names[1];
        if (!failed && call.method === 'PATCH' && target) {
          failed = true;
          if (lostResponse) throw new Error('rename response lost');
          return reply(503, {});
        }
      };
      const f = await fixture(t, lostResponse ? { after: fault } : { before: fault });
      await rejectedRun(f, /Nombres originales restaurados/);
      assert.ok(failed);
      originalsPreserved(f.mock);
      assert.equal([...f.mock.assets.values()].filter(({ name }) => name.includes('.qa-staging-')).length, 4);
    });
  }
}

for (const phase of ['promotion', 'readback']) {
  test(`${phase} digest mismatch rolls back every original without deleting any`, async (t) => {
    let failed = false;
    const f = await fixture(t, { after: (call, mock, data) => {
      const target = phase === 'promotion' ? call.method === 'PATCH' && call.name === names[3] : call.method === 'GET' && data?.name === names[3];
      if (!failed && target) {
        failed = true;
        return reply(200, { ...data, digest: 'sha256:wrong' });
      }
    } });
    await rejectedRun(f, /Verificación GitHub fallida.*Nombres originales restaurados/);
    assert.ok(failed);
    originalsPreserved(f.mock);
  });
}

test('lost rollback responses are confirmed by readback, including old assets without digests', async (t) => {
  let rollback = false;
  const f = await fixture(t, {
    before: (call) => {
      if (!rollback && call.method === 'PATCH' && call.name === names[1]) {
        rollback = true;
        return reply(503, {});
      }
    },
    after: (call) => {
      if (rollback && call.method === 'PATCH') throw new Error('rollback response lost');
    },
  });
  await rejectedRun(f, /Nombres originales restaurados/);
  originalsPreserved(f.mock);
  for (const original of f.mock.originals) assert.ok(f.mock.calls.some(({ method, id }) => method === 'GET' && id === original.id));
});

test('rollback verifies restored names instead of trusting a successful PATCH response', async (t) => {
  let rollback = false;
  const f = await fixture(t, { before: (call, mock) => {
    if (!rollback && call.method === 'PATCH' && call.name === names[1]) {
      rollback = true;
      return reply(503, {});
    }
    if (rollback && call.method === 'PATCH' && call.id === mock.originals[0].id) {
      // Acknowledged but not applied: the subsequent GET must catch this.
      return reply(200, { ...metadata(mock.assets.get(call.id)), name: call.name });
    }
  } });
  await rejectedRun(f, /Rollback incompleto/);
  originalsPreserved(f.mock, { restored: false });
  assert.match(f.mock.assets.get(f.mock.originals[0].id).name, /\.qa-backup-/);
});

for (const [field, value] of Object.entries({ size: 999, state: 'starter', digest: 'sha256:wrong' })) {
  test(`rollback reports unconfirmed original ${field} after restoring its name`, async (t) => {
    let rollback = false;
    const f = await fixture(t, {
      before: (call) => {
        if (!rollback && call.method === 'PATCH' && call.name === names[1]) {
          rollback = true;
          return reply(503, {});
        }
      },
      after: (call, mock, data) => {
        if (rollback && call.method === 'GET' && call.id === mock.originals[0].id) return reply(200, { ...data, [field]: value });
      },
    });
    const original = f.mock.originals[0];
    original.digest = `sha256:${digest(original.body)}`;
    f.mock.assets.get(original.id).digest = original.digest;
    await rejectedRun(f, /Rollback incompleto.*Metadatos del original no confirmados/);
    originalsPreserved(f.mock);
  });
}

test('failed restaging leaves conflicting original as a recoverable backup and restores others', async (t) => {
  let rollback = false;
  const f = await fixture(t, { before: (call, mock) => {
    if (!rollback && call.method === 'PATCH' && call.name === names[1]) {
      rollback = true;
      return reply(503, {});
    }
    if (rollback && call.method === 'PATCH' && call.name?.includes('.qa-staging-') && mock.assets.get(call.id)?.name === names[0]) return reply(503, {});
  } });
  await rejectedRun(f, /Rollback incompleto.*qa-backup-/);
  originalsPreserved(f.mock, { restored: false });
  assert.match(f.mock.assets.get(f.mock.originals[0].id).name, /\.qa-backup-/);
  for (const original of f.mock.originals.slice(1)) assert.equal(f.mock.assets.get(original.id).name, original.name);
});

test('failed original restore retains its ID and bytes and continues restoring other originals', async (t) => {
  let rollback = false;
  const f = await fixture(t, { before: (call, mock) => {
    if (!rollback && call.method === 'PATCH' && call.name === names[1]) {
      rollback = true;
      return reply(503, {});
    }
    if (rollback && call.method === 'PATCH' && call.id === mock.originals[2].id) return reply(503, {});
  } });
  await rejectedRun(f, /Rollback incompleto/);
  originalsPreserved(f.mock, { restored: false });
  assert.match(f.mock.assets.get(f.mock.originals[2].id).name, /\.qa-backup-/);
  for (const original of f.mock.originals.filter((_, i) => i !== 2)) assert.equal(f.mock.assets.get(original.id).name, original.name);
});

test('backup cleanup failure warns and retains the backup after verified publication', async (t) => {
  const f = await fixture(t, { before: (call, mock) => {
    if (call.method === 'DELETE' && call.id === mock.originals[0].id) return reply(503, {});
  } });
  const { retainedBackups } = await f.run();
  assert.equal(retainedBackups.length, 1);
  assert.equal(retainedBackups[0].id, f.mock.originals[0].id);
  assert.deepEqual(f.mock.assets.get(retainedBackups[0].id).body, f.mock.originals[0].body);
  assert.match(f.mock.assets.get(retainedBackups[0].id).name, /\.qa-backup-/);
  assert.equal(f.warnings.length, 1);
  assert.match(f.warnings[0], /no se confirmó la eliminación del backup/);
  assert.ok(names.every((name) => [...f.mock.assets.values()].some((asset) => asset.name === name)));
  assert.equal(f.mock.calls.filter(({ method }) => method === 'DELETE').length, 4, 'cleanup continues after one failure');
});
