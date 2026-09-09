import assert from 'node:assert/strict';
import fs from 'node:fs';

const database = fs.readFileSync('app/src/main/java/com/klortek/velora/offline/OfflineDatabase.kt', 'utf8');
const manager = fs.readFileSync('app/src/main/java/com/klortek/velora/offline/OfflineDownloadManager.kt', 'utf8');
const identity = fs.readFileSync('app/src/main/java/com/klortek/velora/offline/OfflineIdentity.kt', 'utf8');

assert.match(database, /\n    11\n\)\s*\{/,
  'offline database must have a migration version for account-scoped identity');
assert.match(database, /entry_key TEXT NOT NULL PRIMARY KEY/,
  'offline database must key rows by a durable identity, not item ID alone');
assert.match(database, /downloads_v11/,
  'offline database must rebuild legacy rows when introducing the identity key');
assert.match(database, /offlineEntryKey\(this@values\)/,
  'offline writes must persist the account-scoped identity key');
assert.match(identity, /serverUrl.*userId.*itemId.*quality/s,
  'offline identity must include server, user, item and quality');
assert.match(manager, /workNameFor\(itemId, quality, serverUrl, userId\)/,
  'WorkManager names must be scoped to the account as well');
assert.match(manager, /offlineAccountMatches\(first, second\)/,
  'in-memory identity comparisons must include account ownership');

console.log('Offline identity contract passed: server/user scoped SQLite and WorkManager keys are protected.');
